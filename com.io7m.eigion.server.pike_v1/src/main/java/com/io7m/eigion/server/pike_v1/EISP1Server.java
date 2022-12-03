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


package com.io7m.eigion.server.pike_v1;

import com.io7m.eigion.server.http.EISPlainErrorHandler;
import com.io7m.eigion.server.http.EISRequestUniqueIDs;
import com.io7m.eigion.server.http.EISServletHolders;
import com.io7m.eigion.server.service.configuration.EISConfigurationServiceType;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.NullSessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Objects;

/**
 * Pike servers.
 */

public final class EISP1Server
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EISP1Server.class);

  private EISP1Server()
  {

  }

  /**
   * Create an Pike server.
   *
   * @param services The service directory
   *
   * @return A server
   *
   * @throws Exception On errors
   */

  public static Server createPikeServer(
    final EIServiceDirectoryType services)
    throws Exception
  {
    Objects.requireNonNull(services, "services");

    final var configurations =
      services.requireService(EISConfigurationServiceType.class);
    final var configuration =
      configurations.configuration();
    final var httpConfig =
      configuration.pikeApiAddress();
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
      new EISServletHolders(services);
    final var servlets =
      new ServletContextHandler();

    servlets.addServlet(
      servletHolders.create(EISP1Versions.class, EISP1Versions::new),
      "/"
    );
    servlets.addServlet(
      servletHolders.create(EISP1Login.class, EISP1Login::new),
      "/pike/1/0/login"
    );
    servlets.addServlet(
      servletHolders.create(
        EISP1CommandServlet.class,
        EISP1CommandServlet::new),
      "/pike/1/0/command"
    );

    /*
     * Set up a session handler.
     */

    final var sessionIds = new DefaultSessionIdManager(server);
    server.setSessionIdManager(sessionIds);

    final var sessionHandler = new SessionHandler();
    sessionHandler.setSessionCookie("EIGION_PIKE_SESSION");

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
      connector -> connector.addBean(new EISRequestUniqueIDs(services))
    );

    server.setErrorHandler(new EISPlainErrorHandler());
    server.setHandler(gzip);
    server.start();
    LOG.info("[{}] Pike server started", address);
    return server;
  }
}
