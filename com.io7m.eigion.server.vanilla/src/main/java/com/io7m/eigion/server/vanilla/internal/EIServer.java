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


package com.io7m.eigion.server.vanilla.internal;

import com.io7m.eigion.domaincheck.api.EIDomainCheckerConfiguration;
import com.io7m.eigion.domaincheck.api.EIDomainCheckerFactoryType;
import com.io7m.eigion.error_codes.EIException;
import com.io7m.eigion.protocol.amberjack.cb.EIAJCB1Messages;
import com.io7m.eigion.protocol.pike.cb.EIPCB1Messages;
import com.io7m.eigion.server.amberjack_v1.EISAJ1Sends;
import com.io7m.eigion.server.amberjack_v1.EISAJ1Server;
import com.io7m.eigion.server.api.EIServerConfiguration;
import com.io7m.eigion.server.api.EIServerException;
import com.io7m.eigion.server.api.EIServerType;
import com.io7m.eigion.server.controller.EISStrings;
import com.io7m.eigion.server.controller.login.EILoginService;
import com.io7m.eigion.server.controller.login.EILoginServiceType;
import com.io7m.eigion.server.database.api.EISDatabaseType;
import com.io7m.eigion.server.pike_v1.EISP1Sends;
import com.io7m.eigion.server.pike_v1.EISP1Server;
import com.io7m.eigion.server.service.clock.EISClock;
import com.io7m.eigion.server.service.configuration.EISConfigurationService;
import com.io7m.eigion.server.service.configuration.EISConfigurationServiceType;
import com.io7m.eigion.server.service.domaincheck.EISDomainChecking;
import com.io7m.eigion.server.service.domaincheck.EISDomainCheckingType;
import com.io7m.eigion.server.service.idstore.EISIdstoreClients;
import com.io7m.eigion.server.service.idstore.EISIdstoreClientsType;
import com.io7m.eigion.server.service.limits.EIRequestLimits;
import com.io7m.eigion.server.service.limits.EIRequestLimitsType;
import com.io7m.eigion.server.service.sessions.EISessionService;
import com.io7m.eigion.server.service.sessions.EISessionServiceType;
import com.io7m.eigion.server.service.telemetry.api.EISTelemetryNoOp;
import com.io7m.eigion.server.service.telemetry.api.EISTelemetryServiceFactoryType;
import com.io7m.eigion.server.service.telemetry.api.EISTelemetryServiceType;
import com.io7m.eigion.server.service.verdant.EISVerdantMessages;
import com.io7m.eigion.server.service.verdant.EISVerdantMessagesType;
import com.io7m.eigion.services.api.EIServiceDirectory;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import io.opentelemetry.api.trace.SpanKind;
import org.eclipse.jetty.server.Server;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.SERVER_STARTUP_ERROR;

/**
 * The default server implementation.
 */

public final class EIServer implements EIServerType
{
  private final EIServerConfiguration configuration;
  private final AtomicBoolean closed;
  private CloseableCollectionType<EIServerException> resources;
  private EISTelemetryServiceType telemetry;
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
    this.closed =
      new AtomicBoolean(true);
  }

  private EISTelemetryServiceType createTelemetry()
  {
    return this.configuration.openTelemetry()
      .flatMap(config -> {
        final var loader =
          ServiceLoader.load(EISTelemetryServiceFactoryType.class);
        return loader.findFirst().map(f -> f.create(config));
      }).orElseGet(EISTelemetryNoOp::noop);
  }

  @Override
  public void start()
    throws EIServerException
  {
    if (this.closed.compareAndSet(true, false)) {
      this.telemetry =
        this.createTelemetry();

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

        final var amberjackServer =
          this.createAmberjackServer();
        this.resources.add(amberjackServer::stop);

        final var pikeServer =
          this.createPikeServer();
        this.resources.add(pikeServer::stop);

      } catch (final EIException e) {
        this.close();
        startupSpan.recordException(e);
        throw new EIServerException(e.errorCode(), e.getMessage(), e);
      } catch (final Exception e) {
        this.close();
        startupSpan.recordException(e);
        throw new EIServerException(SERVER_STARTUP_ERROR, e.getMessage(), e);
      } finally {
        startupSpan.end();
      }
    }
  }

  private Server createPikeServer()
    throws Exception
  {
    return EISP1Server.createPikeServer(this.services);
  }

  private Server createAmberjackServer()
    throws Exception
  {
    return EISAJ1Server.createAmberjackServer(this.services);
  }

  private EIServiceDirectory createServiceDirectory(
    final EISDatabaseType inDatabase)
    throws IOException
  {
    final var newServices = new EIServiceDirectory();

    final var strings = new EISStrings(this.configuration.locale());
    newServices.register(EISStrings.class, strings);

    final var idstore =
      EISIdstoreClients.create(
        this.configuration.locale(),
        this.configuration.idstoreConfiguration()
      );

    newServices.register(EISIdstoreClientsType.class, idstore);

    final var sessions =
      new EISessionService(
        idstore,
        this.telemetry.openTelemetry(),
        Duration.ofMinutes(30L)
      );

    newServices.register(EISessionServiceType.class, sessions);
    newServices.register(EISDatabaseType.class, inDatabase);

    newServices.register(
      EISConfigurationServiceType.class,
      new EISConfigurationService(this.configuration)
    );

    final var clock = new EISClock(this.configuration.clock());
    newServices.register(EISClock.class, clock);

    newServices.register(
      EISVerdantMessagesType.class, new EISVerdantMessages());

    newServices.register(
      EISTelemetryServiceType.class, this.telemetry);

    final var isaj1Messages = new EIAJCB1Messages();
    newServices.register(EIAJCB1Messages.class, isaj1Messages);

    final var isaj1Sends = new EISAJ1Sends(isaj1Messages);
    newServices.register(EISAJ1Sends.class, isaj1Sends);

    final var ipcb1Messages = new EIPCB1Messages();
    newServices.register(EIPCB1Messages.class, ipcb1Messages);

    final var isp1Sends = new EISP1Sends(ipcb1Messages);
    newServices.register(EISP1Sends.class, isp1Sends);

    final var checkers =
      loadDomainCheckers();

    final var checker =
      checkers.createChecker(new EIDomainCheckerConfiguration(
        this.telemetry.openTelemetry(),
        this.configuration.clock(),
        this.configuration.httpClients().get()
      ));

    final var domainCheck =
      new EISDomainChecking(this.database, checker);

    newServices.register(EISDomainCheckingType.class, domainCheck);

    final var limits = new EIRequestLimits(size -> {
      return strings.format("requestTooLarge", size);
    });
    newServices.register(EIRequestLimitsType.class, limits);

    final var loginService =
      new EILoginService(clock, strings, sessions, idstore);

    newServices.register(EILoginServiceType.class, loginService);
    return newServices;
  }

  private static EIDomainCheckerFactoryType loadDomainCheckers()
  {
    return ServiceLoader.load(EIDomainCheckerFactoryType.class)
      .findFirst()
      .orElseThrow(() -> {
        return new ServiceConfigurationError(
          "No services available of type %s"
            .formatted(EIDomainCheckerFactoryType.class)
        );
      });
  }

  @Override
  public EISDatabaseType database()
  {
    if (this.closed.get()) {
      throw new IllegalStateException("Server is closed.");
    }
    return this.database;
  }

  @Override
  public void close()
    throws EIServerException
  {
    if (this.closed.compareAndSet(false, true)) {
      this.resources.close();
    }
  }
}
