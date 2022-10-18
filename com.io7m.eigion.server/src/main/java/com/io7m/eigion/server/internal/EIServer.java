/*
 * Copyright © 2022 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */


package com.io7m.eigion.server.internal;

import com.io7m.eigion.protocol.amberjack.cb.EIAJCB1Messages;
import com.io7m.eigion.server.api.EIServerConfiguration;
import com.io7m.eigion.server.api.EIServerException;
import com.io7m.eigion.server.api.EIServerType;
import com.io7m.eigion.server.database.api.EISDatabaseException;
import com.io7m.eigion.server.database.api.EISDatabaseType;
import com.io7m.eigion.server.internal.amberjack_v1.EISAJ1CommandServlet;
import com.io7m.eigion.server.internal.amberjack_v1.EISAJ1Login;
import com.io7m.eigion.server.internal.amberjack_v1.EISAJ1Sends;
import com.io7m.eigion.server.internal.amberjack_v1.EISAJ1Versions;
import com.io7m.eigion.server.internal.sessions.EISUserSessionService;
import com.io7m.eigion.services.api.EIServiceDirectory;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import io.opentelemetry.api.trace.SpanKind;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.NullSessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Objects;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.SERVER_STARTUP_ERROR;

/**
 * The default server.
 */

public final class EIServer implements EIServerType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIServer.class);

  private final EIServerConfiguration configuration;
  private CloseableCollectionType<EIServerException> resources;
  private EISTelemetryService telemetry;
  private EISDatabaseType database;
  private EIServiceDirectory services;

  /**
   * Construct a new server.
   *
   * @param inConfiguration The configuration
   */

  public EIServer(
    final EIServerConfiguration inConfiguration)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.resources =
      CloseableCollection.create(() -> {
        return new EIServerException(
          SERVER_STARTUP_ERROR,
          "Server failed to start."
        );
      });
  }

  @Override
  public void start()
    throws EIServerException
  {
    this.telemetry =
      EISTelemetryService.create(this.configuration);

    final var startupSpan =
      this.telemetry.tracer()
        .spanBuilder("EIServer.start")
        .setSpanKind(SpanKind.INTERNAL)
        .startSpan();

    try {
      this.resources =
        CloseableCollection.create(() -> {
          return new EIServerException(
            SERVER_STARTUP_ERROR,
            "Server failed to start."
          );
        });

      this.database =
        this.resources.add(
          this.configuration.databases()
            .open(
              this.configuration.databaseConfiguration(),
              this.telemetry.openTelemetry(),
              statement -> {

              })
        );

      this.services =
        this.resources.add(this.createServiceDirectory(this.database));

      final var amberjackServer = this.createAmberjackServer();
      this.resources.add(amberjackServer::stop);

    } catch (final EISDatabaseException e) {
      startupSpan.recordException(e);
      throw new EIServerException(e.errorCode(), e.getMessage(), e);
    } catch (final Exception e) {
      startupSpan.recordException(e);
      throw new EIServerException(SERVER_STARTUP_ERROR, e.getMessage(), e);
    } finally {
      startupSpan.end();
    }
  }

  private Server createAmberjackServer()
    throws Exception
  {
    final var httpConfig =
      this.configuration.amberjackApiAddress();
    final var address =
      InetSocketAddress.createUnresolved(
        httpConfig.listenAddress(),
        httpConfig.listenPort()
      );

    final var server =
      new Server(address);

    /*
     * Configure all the servlets.
     */

    final var servletHolders =
      new EISServletHolders(this.services);
    final var servlets =
      new ServletContextHandler();

    servlets.addServlet(
      servletHolders.create(EISAJ1Versions.class, EISAJ1Versions::new),
      "/"
    );
    servlets.addServlet(
      servletHolders.create(EISAJ1Login.class, EISAJ1Login::new),
      "/amberjack/1/0/login"
    );
    servlets.addServlet(
      servletHolders.create(
        EISAJ1CommandServlet.class,
        EISAJ1CommandServlet::new),
      "/amberjack/1/0/command"
    );

    servlets.addEventListener(
      this.services.requireService(EISUserSessionService.class)
    );

    /*
     * Set up a session handler.
     */

    final var sessionIds = new DefaultSessionIdManager(server);
    server.setSessionIdManager(sessionIds);

    final var sessionHandler = new SessionHandler();
    sessionHandler.setSessionCookie("EIGION_AMBERJACK_SESSION");

    final var sessionStore = new NullSessionDataStore();
    final var sessionCache = new DefaultSessionCache(sessionHandler);
    sessionCache.setSessionDataStore(sessionStore);

    sessionHandler.setSessionCache(sessionCache);
    sessionHandler.setSessionIdManager(sessionIds);
    sessionHandler.setHandler(servlets);

    /*
     * Enable gzip.
     */

    final var gzip = new GzipHandler();
    gzip.setHandler(sessionHandler);

    /*
     * Add a connector listener that adds unique identifiers to all requests.
     */

    Arrays.stream(server.getConnectors()).forEach(
      connector -> connector.addBean(new EISRequestDecoration(this.services))
    );

    server.setErrorHandler(new EISErrorHandler());
    server.setHandler(gzip);
    server.start();
    LOG.info("[{}] amberjack server started", address);
    return server;
  }

  private EIServiceDirectory createServiceDirectory(
    final EISDatabaseType inDatabase)
    throws IOException
  {
    final var newServices = new EIServiceDirectory();

    newServices.register(EISTelemetryService.class, this.telemetry);
    newServices.register(EISDatabaseType.class, inDatabase);

    final var strings = new EISStrings(this.configuration.locale());
    newServices.register(EISStrings.class, strings);

    final var clock = new EISClock(this.configuration.clock());
    newServices.register(EISClock.class, clock);
    newServices.register(EISVerdantMessages.class, new EISVerdantMessages());

    final var maintenance =
      EISMaintenanceService.create(clock, this.telemetry, this.database);
    newServices.register(EISMaintenanceService.class, maintenance);

    final var userSessions = new EISUserSessionService(this.telemetry);
    newServices.register(EISUserSessionService.class, userSessions);

    final var messages = new EIAJCB1Messages();
    newServices.register(EIAJCB1Messages.class, messages);
    newServices.register(EISAJ1Sends.class, new EISAJ1Sends(messages));
    newServices.register(EISRequestLimits.class, new EISRequestLimits(strings));

    final var idstoreClients =
      EISIdstoreClients.create(
        this.configuration.locale(),
        this.configuration.idstoreConfiguration()
      );
    newServices.register(EISIdstoreClients.class, idstoreClients);
    return newServices;
  }

  @Override
  public EISDatabaseType database()
  {
    return this.database;
  }

  @Override
  public void close()
    throws EIServerException
  {
    this.resources.close();
  }
}
