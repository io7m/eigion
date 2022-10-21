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

package com.io7m.eigion.tests;

import com.io7m.eigion.server.EIServerFactory;
import com.io7m.eigion.server.api.EIServerConfiguration;
import com.io7m.eigion.server.api.EIServerConfiguratorType;
import com.io7m.eigion.server.api.EIServerHTTPServiceConfiguration;
import com.io7m.eigion.server.api.EIServerIdstoreConfiguration;
import com.io7m.eigion.server.api.EIServerType;
import com.io7m.eigion.server.database.api.EISDatabaseConfiguration;
import com.io7m.eigion.server.database.api.EISDatabaseCreate;
import com.io7m.eigion.server.database.api.EISDatabaseType;
import com.io7m.eigion.server.database.api.EISDatabaseUpgrade;
import com.io7m.eigion.server.database.postgres.EISDatabases;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jmulticlose.core.ClosingResourceFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class EITestServer implements AutoCloseable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EITestServer.class);

  private final PostgreSQLContainer<?> container;
  private final EIServerType server;
  private final EIServerConfiguratorType configurator;
  private final CloseableCollectionType<ClosingResourceFailedException> resources;

  private EITestServer(
    final PostgreSQLContainer<?> inContainer,
    final EIServerType inServer,
    final EIServerConfiguratorType inConfigurator,
    final CloseableCollectionType<ClosingResourceFailedException> inResources)
  {
    this.container =
      Objects.requireNonNull(inContainer, "container");
    this.server =
      Objects.requireNonNull(inServer, "server");
    this.configurator =
      Objects.requireNonNull(inConfigurator, "configurator");
    this.resources =
      Objects.requireNonNull(inResources, "resources");
  }

  private static EIServerConfiguration createConfiguration(
    final EIFakeClock clock,
    final Supplier<HttpClient> httpClients,
    final PostgreSQLContainer<?> inContainer)
  {
    final var databaseConfiguration =
      new EISDatabaseConfiguration(
        "postgres",
        "12345678",
        inContainer.getHost(),
        inContainer.getFirstMappedPort().intValue(),
        "eigion",
        EISDatabaseCreate.CREATE_DATABASE,
        EISDatabaseUpgrade.UPGRADE_DATABASE,
        clock
      );

    final var pikeService =
      new EIServerHTTPServiceConfiguration(
        "localhost",
        60000,
        URI.create("http://localhost:60000/"),
        Optional.empty()
      );
    final var amberjackService =
      new EIServerHTTPServiceConfiguration(
        "localhost",
        61000,
        URI.create("http://localhost:61000/"),
        Optional.empty()
      );

    return new EIServerConfiguration(
      Locale.getDefault(),
      clock,
      httpClients,
      new EISDatabases(),
      databaseConfiguration,
      pikeService,
      amberjackService,
      new EIServerIdstoreConfiguration(
        URI.create("http://localhost:50000/"),
        URI.create("http://localhost:51000/password-reset")
      ),
      Optional.empty()
    );
  }

  public static EITestServer create(
    final PostgreSQLContainer<?> container,
    final Supplier<HttpClient> httpClients,
    final EIFakeClock clock)
    throws Exception
  {
    final var resources =
      CloseableCollection.create();

    waitForDatabaseToStart(container);

    container.addEnv("PGPASSWORD", "12345678");

    final var r =
      container.execInContainer("createdb", "-U", "postgres", "eigion");

    assertEquals(0, r.getExitCode());

    final var configuration =
      createConfiguration(clock, httpClients, container);
    final var servers =
      new EIServerFactory();
    final var server =
      resources.add(servers.createServer(configuration));
    final var configurator =
      resources.add(servers.createServerConfigurator(configuration));

    return new EITestServer(container, server, configurator, resources);
  }

  private static void waitForDatabaseToStart(
    final PostgreSQLContainer<?> container)
    throws InterruptedException, TimeoutException
  {
    LOG.debug("waiting for database to start");
    final var timeWait = Duration.ofSeconds(60L);
    final var timeThen = Instant.now();
    while (!container.isRunning()) {
      Thread.sleep(1L);
      final var timeNow = Instant.now();
      if (Duration.between(timeThen, timeNow).compareTo(timeWait) > 0) {
        LOG.error("timed out waiting for database to start");
        throw new TimeoutException("Timed out waiting for database to start");
      }
    }
    LOG.debug("database started");
  }

  @Override
  public void close()
    throws Exception
  {
    this.resources.close();
  }

  public URI baseAmberjackURI()
  {
    return URI.create("http://localhost:61000/");
  }

  public URI basePikeURI()
  {
    return URI.create("http://localhost:60000/");
  }

  public EIServerType server()
  {
    return this.server;
  }

  public EIServerConfiguratorType configurator()
  {
    return this.configurator;
  }

  public EISDatabaseType database()
  {
    return this.server.database();
  }
}
