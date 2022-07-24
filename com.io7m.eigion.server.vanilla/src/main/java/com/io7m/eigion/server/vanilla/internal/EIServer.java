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

import com.io7m.eigion.server.api.EIServerClosed;
import com.io7m.eigion.server.api.EIServerConfiguration;
import com.io7m.eigion.server.api.EIServerEventType;
import com.io7m.eigion.server.api.EIServerException;
import com.io7m.eigion.server.api.EIServerStarted;
import com.io7m.eigion.server.api.EIServerStarting;
import com.io7m.eigion.server.api.EIServerType;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseType;
import com.io7m.eigion.protocol.admin_api.v1.EISA1Messages;
import com.io7m.eigion.protocol.public_api.v1.EISP1Messages;
import com.io7m.eigion.protocol.versions.EISVMessages;
import com.io7m.eigion.server.vanilla.internal.admin_api.EIACommandServlet;
import com.io7m.eigion.server.vanilla.internal.admin_api.EIALogin;
import com.io7m.eigion.server.vanilla.internal.admin_api.EIASends;
import com.io7m.eigion.server.vanilla.internal.admin_api.EIATransactionServlet;
import com.io7m.eigion.server.vanilla.internal.admin_api.EIAVersions;
import com.io7m.eigion.server.vanilla.internal.public_api.EIPImageCreate;
import com.io7m.eigion.server.vanilla.internal.public_api.EIPImageGet;
import com.io7m.eigion.server.vanilla.internal.public_api.EIPLogin;
import com.io7m.eigion.server.vanilla.internal.public_api.EIPProducts;
import com.io7m.eigion.server.vanilla.internal.public_api.EIPSends;
import com.io7m.eigion.server.vanilla.internal.public_api.EIPVersions;
import com.io7m.eigion.server.vanilla.logging.EIServerRequestLog;
import com.io7m.eigion.services.api.EIServiceDirectory;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import com.io7m.eigion.storage.api.EIStorageConfigurationException;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.FileSessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The main server implementation.
 */

public final class EIServer implements EIServerType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIServer.class);

  private final EIServerConfiguration configuration;
  private final CloseableCollectionType<EIServerException> resources;
  private final AtomicBoolean closed;
  private final EIServerEventBus events;
  private EIServerDatabaseType database;

  /**
   * The main server implementation.
   *
   * @param inConfiguration The server configuration
   */

  public EIServer(
    final EIServerConfiguration inConfiguration)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.resources =
      CloseableCollection.create(
        () -> {
          return new EIServerException(
            "Server creation failed.",
            "server-creation"
          );
        }
      );

    this.events =
      new EIServerEventBus();
    this.closed =
      new AtomicBoolean(false);
  }

  @Override
  public void start()
    throws EIServerException
  {
    if (this.closed.get()) {
      throw new IllegalStateException("Server is closed!");
    }

    try {
      this.database =
        this.resources.add(this.createDatabase());

      final var services =
        this.createServiceDirectory(this.database);

      final var adminServer = this.createAdminServer(services);
      this.resources.add(adminServer::stop);
      final var publicServer = this.createPublicServer(services);
      this.resources.add(publicServer::stop);

      this.events.publish(new EIServerStarted(this.configuration.now()));
    } catch (final EIServerDatabaseException e) {
      try {
        this.close();
      } catch (final EIServerException ex) {
        e.addSuppressed(ex);
      }
      throw new EIServerException(e.getMessage(), e, "database");
    } catch (final Exception e) {
      try {
        this.close();
      } catch (final EIServerException ex) {
        e.addSuppressed(ex);
      }
      throw new EIServerException(e.getMessage(), e, "startup");
    }
  }

  @Override
  public EIServerDatabaseType database()
  {
    return Optional.ofNullable(this.database)
      .orElseThrow(() -> {
        return new IllegalStateException("Server is not started.");
      });
  }

  private EIServiceDirectory createServiceDirectory(
    final EIServerDatabaseType inDatabase)
    throws IOException, EIStorageConfigurationException
  {
    final var services = new EIServiceDirectory();

    services.register(
      EIServerEventBus.class,
      this.events
    );

    services.register(
      EIServerDatabaseType.class,
      inDatabase
    );

    final var clock = new EIServerClock(this.configuration.clock());
    services.register(EIServerClock.class, clock);

    final var config = new EIServerConfigurations(this.configuration);
    services.register(EIServerConfigurations.class, config);

    final var messages = new EISVMessages();
    services.register(EISVMessages.class, messages);

    final var pv1messages = new EISP1Messages();
    services.register(EISP1Messages.class, pv1messages);

    final var av1messages = new EISA1Messages();
    services.register(EISA1Messages.class, av1messages);

    final var strings = new EIServerStrings(this.configuration.locale());
    services.register(EIServerStrings.class, strings);

    services.register(
      EIPSends.class,
      new EIPSends(pv1messages)
    );

    services.register(
      EIASends.class,
      new EIASends(av1messages)
    );

    services.register(
      EIRequestLimits.class,
      new EIRequestLimits(strings)
    );

    services.register(
      EIServerImageStorage.class,
      EIServerImageStorage.create(
        this.configuration.imageStorageFactory(),
        this.configuration.imageStorageParameters())
    );
    return services;
  }

  @Override
  public Flow.Publisher<EIServerEventType> events()
  {
    return this.events.publisher();
  }

  private Server createPublicServer(
    final EIServiceDirectoryType services)
    throws Exception
  {
    final var address =
      this.configuration.publicAddress();
    final var server =
      new Server(address);

    /*
     * Configure all the servlets.
     */

    final var servletHolders =
      new EIServletHolders(services);
    final var servlets =
      new ServletContextHandler();

    servlets.addServlet(
      servletHolders.create(EIPVersions.class, EIPVersions::new),
      "/"
    );
    servlets.addServlet(
      servletHolders.create(EIPLogin.class, EIPLogin::new),
      "/public/1/0/login"
    );
    servlets.addServlet(
      servletHolders.create(EIPImageCreate.class, EIPImageCreate::new),
      "/public/1/0/image/create/*"
    );
    servlets.addServlet(
      servletHolders.create(EIPImageGet.class, EIPImageGet::new),
      "/public/1/0/image/get/*"
    );
    servlets.addServlet(
      servletHolders.create(EIPProducts.class, EIPProducts::new),
      "/public/1/0/products/get/*"
    );

    /*
     * Set up a session handler that allows for Servlets to have sessions
     * that can survive server restarts.
     */

    final var sessionIds = new DefaultSessionIdManager(server);
    final var sessionHandler = new SessionHandler();

    final var sessionStore = new FileSessionDataStore();
    sessionStore.setStoreDir(this.configuration.sessionDirectory().toFile());

    final var sessionCache = new DefaultSessionCache(sessionHandler);
    sessionCache.setSessionDataStore(sessionStore);

    sessionHandler.setSessionCache(sessionCache);
    sessionHandler.setSessionIdManager(sessionIds);
    sessionHandler.setHandler(servlets);

    /*
     * Set up an MBean container so that the statistics handler can export
     * statistics to JMX.
     */

    final var mbeanContainer =
      new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
    server.addBean(mbeanContainer);

    /*
     * Set up a statistics handler that wraps everything.
     */

    final var statsHandler = new StatisticsHandler();
    statsHandler.setHandler(sessionHandler);

    /*
     * Add a connector listener that adds unique identifiers to all requests.
     */

    Arrays.stream(server.getConnectors()).forEach(
      connector -> connector.addBean(new EIServerRequestDecoration(services))
    );

    server.setErrorHandler(new EIErrorHandler());
    server.setRequestLog(new EIServerRequestLog(services, "public"));
    server.setHandler(statsHandler);
    server.start();
    LOG.info("[{}] public server started", address);
    return server;
  }

  private Server createAdminServer(
    final EIServiceDirectoryType services)
    throws Exception
  {
    final var address =
      this.configuration.adminAddress();
    final var server =
      new Server(address);

    final var servletHolders =
      new EIServletHolders(services);
    final var servlets =
      new ServletContextHandler();

    servlets.addServlet(
      servletHolders.create(EIAVersions.class, EIAVersions::new),
      "/"
    );
    servlets.addServlet(
      servletHolders.create(EIALogin.class, EIALogin::new),
      "/admin/1/0/login"
    );
    servlets.addServlet(
      servletHolders.create(EIACommandServlet.class, EIACommandServlet::new),
      "/admin/1/0/command/*"
    );
    servlets.addServlet(
      servletHolders.create(
        EIATransactionServlet.class,
        EIATransactionServlet::new),
      "/admin/1/0/transaction/*"
    );

    /*
     * Set up a session handler that allows for Servlets to have sessions
     * that can survive server restarts.
     */

    final var sessionIds = new DefaultSessionIdManager(server);
    final var sessionHandler = new SessionHandler();

    final var sessionStore = new FileSessionDataStore();
    sessionStore.setStoreDir(this.configuration.sessionDirectory().toFile());

    final var sessionCache = new DefaultSessionCache(sessionHandler);
    sessionCache.setSessionDataStore(sessionStore);

    sessionHandler.setSessionCache(sessionCache);
    sessionHandler.setSessionIdManager(sessionIds);
    sessionHandler.setHandler(servlets);

    /*
     * Set up an MBean container so that the statistics handler can export
     * statistics to JMX.
     */

    final var mbeanContainer =
      new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
    server.addBean(mbeanContainer);

    /*
     * Set up a statistics handler that wraps everything.
     */

    final var statsHandler = new StatisticsHandler();
    statsHandler.setHandler(sessionHandler);

    /*
     * Add a connector listener that adds unique identifiers to all requests.
     */

    Arrays.stream(server.getConnectors()).forEach(
      connector -> connector.addBean(new EIServerRequestDecoration(services))
    );

    server.setErrorHandler(new EIErrorHandler());
    server.setRequestLog(new EIServerRequestLog(services, "admin"));
    server.setHandler(statsHandler);
    server.start();
    LOG.info("[{}] admin server started", address);
    return server;
  }

  private EIServerDatabaseType createDatabase()
    throws EIServerDatabaseException
  {
    return this.configuration.databases()
      .open(
        this.configuration.databaseConfiguration(),
        this::publishStartupEvent);
  }

  private void publishStartupEvent(
    final String message)
  {
    this.events.publish(
      new EIServerStarting(
        this.configuration.now(), "Database: " + message)
    );
  }

  @Override
  public void close()
    throws EIServerException
  {
    if (this.closed.compareAndSet(false, true)) {
      this.resources.close();
      this.events.publish(new EIServerClosed(this.configuration.now()));
      this.events.close();
    }
  }
}
