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

package com.io7m.eigion.tests.extensions;

import com.io7m.eigion.server.api.EIServerConfiguration;
import com.io7m.eigion.server.api.EIServerConfiguratorType;
import com.io7m.eigion.server.api.EIServerException;
import com.io7m.eigion.server.api.EIServerHTTPServiceConfiguration;
import com.io7m.eigion.server.api.EIServerIdstoreConfiguration;
import com.io7m.eigion.server.api.EIServerType;
import com.io7m.eigion.server.database.api.EISDatabaseConfiguration;
import com.io7m.eigion.server.database.api.EISDatabaseCreate;
import com.io7m.eigion.server.database.api.EISDatabaseUpgrade;
import com.io7m.eigion.server.database.postgres.EISDatabases;
import com.io7m.eigion.server.vanilla.EIServerFactory;
import com.io7m.eigion.tests.EIFakeClock;
import com.io7m.eigion.tests.domaincheck.EIInterceptHttpClient;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jmulticlose.core.ClosingResourceFailedException;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

public final class EIServerExtension
  implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback,
  ExtensionContext.Store.CloseableResource,
  ParameterResolver
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIServerExtension.class);

  private static final PostgreSQLContainer<?> CONTAINER =
    new PostgreSQLContainer<>(
      DockerImageName.parse("postgres")
        .withTag("14.4"))
      .withDatabaseName("eigion")
      .withUsername("postgres")
      .withPassword("12345678");

  public static final URI PIKE_URI =
    URI.create("http://localhost:60000/");

  public static final URI AMBERJACK_URI =
    URI.create("http://localhost:61000/");

  private final EIInterceptHttpClient httpClient;
  private EIFakeClock serverClock;
  private CloseableCollectionType<ClosingResourceFailedException> globalResources;
  private CloseableCollectionType<ClosingResourceFailedException> perTestResources;
  private AtomicBoolean started;
  private EIServerType server;
  private EIServerConfiguration configuration;
  private Supplier<HttpClient> httpClients;
  private EIServerConfiguratorType serverConfigurator;

  public EIServerExtension()
  {
    this.globalResources =
      CloseableCollection.create();
    this.perTestResources =
      CloseableCollection.create();
    this.serverClock =
      new EIFakeClock();
    this.httpClient =
      new EIInterceptHttpClient(uri -> uri, HttpClient.newHttpClient());
    this.httpClients =
      () -> this.httpClient;
    this.started =
      new AtomicBoolean(false);
  }

  @Override
  public void beforeAll(
    final ExtensionContext context)
    throws Exception
  {
    if (this.started.compareAndSet(false, true)) {
      context.getRoot()
        .getStore(GLOBAL)
        .put("com.io7m.eigion.tests.extensions.EIServerExtension", this);

      this.globalResources = CloseableCollection.create();
      this.globalResources.add(() -> {
        CONTAINER.execInContainer(
          "dropdb",
          "-w",
          "-U",
          "postgres",
          "eigion"
        );
        CONTAINER.stop();
      });

      CONTAINER.start();
      CONTAINER.addEnv("PGPASSWORD", "12345678");
    }
  }

  @Override
  public void close()
    throws Throwable
  {
    LOG.debug("tearing down eigion server container");
    this.globalResources.close();
  }

  @Override
  public void beforeEach(
    final ExtensionContext context)
    throws Exception
  {
    LOG.debug("setting up eigion server");

    final var r0 =
      CONTAINER.execInContainer(
        "dropdb",
        "-w",
        "-U",
        "postgres",
        "eigion"
      );
    LOG.debug("stderr: {}", r0.getStderr());

    final var r1 =
      CONTAINER.execInContainer(
        "createdb",
        "-w",
        "-U",
        "postgres",
        "eigion"
      );

    LOG.debug("stderr: {}", r0.getStderr());
    assertEquals(0, r1.getExitCode());

    this.perTestResources =
      CloseableCollection.create();
    this.serverClock =
      new EIFakeClock();

    this.createServer();
    this.perTestResources.add(this.server);
    this.perTestResources.add(this.serverConfigurator);
    this.server.start();
  }

  private void createServer()
    throws EIServerException
  {
    final var databaseConfiguration =
      new EISDatabaseConfiguration(
        "postgres",
        "12345678",
        CONTAINER.getHost(),
        CONTAINER.getFirstMappedPort().intValue(),
        "eigion",
        EISDatabaseCreate.CREATE_DATABASE,
        EISDatabaseUpgrade.UPGRADE_DATABASE,
        this.serverClock
      );

    final var pikeService =
      new EIServerHTTPServiceConfiguration(
        "localhost",
        60000,
        PIKE_URI,
        Optional.empty()
      );
    final var amberjackService =
      new EIServerHTTPServiceConfiguration(
        "localhost",
        61000,
        AMBERJACK_URI,
        Optional.empty()
      );

    this.configuration = new EIServerConfiguration(
      Locale.getDefault(),
      this.serverClock,
      this.httpClients,
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

    final var servers = new EIServerFactory();
    this.server =
      servers.createServer(this.configuration);
    this.serverConfigurator =
      servers.createServerConfigurator(this.configuration);
  }

  @Override
  public void afterEach(
    final ExtensionContext context)
    throws Exception
  {
    LOG.debug("tearing down eigion server");
    this.perTestResources.close();
  }

  @Override
  public boolean supportsParameter(
    final ParameterContext parameterContext,
    final ExtensionContext extensionContext)
    throws ParameterResolutionException
  {
    final Class<?> requiredType =
      parameterContext.getParameter().getType();

    return Objects.equals(requiredType, EIServerType.class)
           || Objects.equals(requiredType, EIServerConfiguratorType.class)
           || Objects.equals(requiredType, EIInterceptHttpClient.class);
  }

  @Override
  public Object resolveParameter(
    final ParameterContext parameterContext,
    final ExtensionContext extensionContext)
    throws ParameterResolutionException
  {
    final Class<?> requiredType =
      parameterContext.getParameter().getType();

    if (Objects.equals(requiredType, EIServerType.class)) {
      return this.server;
    }

    if (Objects.equals(requiredType, EIServerConfiguratorType.class)) {
      return this.serverConfigurator;
    }

    if (Objects.equals(requiredType, EIInterceptHttpClient.class)) {
      return this.httpClient;
    }

    throw new IllegalStateException(
      "Unrecognized requested parameter type: %s".formatted(requiredType)
    );
  }
}
