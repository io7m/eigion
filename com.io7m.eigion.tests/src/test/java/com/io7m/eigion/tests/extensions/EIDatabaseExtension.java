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

import com.io7m.eigion.server.database.api.EISDatabaseConfiguration;
import com.io7m.eigion.server.database.api.EISDatabaseCreate;
import com.io7m.eigion.server.database.api.EISDatabaseException;
import com.io7m.eigion.server.database.api.EISDatabaseRole;
import com.io7m.eigion.server.database.api.EISDatabaseTransactionType;
import com.io7m.eigion.server.database.api.EISDatabaseType;
import com.io7m.eigion.server.database.api.EISDatabaseUpgrade;
import com.io7m.eigion.server.database.postgres.EISDatabases;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jmulticlose.core.ClosingResourceFailedException;
import io.opentelemetry.api.OpenTelemetry;
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

import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

public final class EIDatabaseExtension
  implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback,
  ExtensionContext.Store.CloseableResource,
  ParameterResolver
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIDatabaseExtension.class);

  private static final EISDatabases DATABASES =
    new EISDatabases();

  private static final PostgreSQLContainer<?> CONTAINER =
    new PostgreSQLContainer<>(
      DockerImageName.parse("postgres")
        .withTag("14.4"))
      .withDatabaseName("eigion")
      .withUsername("postgres")
      .withPassword("12345678");

  private CloseableCollectionType<ClosingResourceFailedException> globalResources;
  private CloseableCollectionType<ClosingResourceFailedException> perTestResources;
  private AtomicBoolean started;
  private EISDatabaseType database;

  public EIDatabaseExtension()
  {
    this.globalResources =
      CloseableCollection.create();
    this.perTestResources =
      CloseableCollection.create();
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
        .put("com.io7m.eigion.tests.extensions.EIDatabaseExtension", this);

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
    LOG.debug("tearing down database container");
    this.globalResources.close();
  }

  @Override
  public void beforeEach(
    final ExtensionContext context)
    throws Exception
  {
    LOG.debug("setting up database");

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

    final var configuration =
      new EISDatabaseConfiguration(
        "postgres",
        "12345678",
        CONTAINER.getContainerIpAddress(),
        CONTAINER.getFirstMappedPort().intValue(),
        "eigion",
        EISDatabaseCreate.CREATE_DATABASE,
        EISDatabaseUpgrade.UPGRADE_DATABASE,
        Clock.systemUTC()
      );

    this.perTestResources = CloseableCollection.create();
    this.database = this.perTestResources.add(DATABASES.open(
      configuration,
      OpenTelemetry.noop(),
      message -> {

      }
    ));
  }

  @Override
  public void afterEach(
    final ExtensionContext context)
    throws Exception
  {
    LOG.debug("tearing down database");
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

    return Objects.equals(requiredType, EISDatabaseType.class)
           || Objects.equals(requiredType, EISDatabaseTransactionType.class);
  }

  @Override
  public Object resolveParameter(
    final ParameterContext parameterContext,
    final ExtensionContext extensionContext)
    throws ParameterResolutionException
  {
    try {
      final Class<?> requiredType =
        parameterContext.getParameter().getType();

      if (Objects.equals(requiredType, EISDatabaseType.class)) {
        return this.database;
      }

      if (Objects.equals(requiredType, EISDatabaseTransactionType.class)) {
        final var connection =
          this.perTestResources.add(
            this.database.openConnection(EISDatabaseRole.EIGION));
        final EISDatabaseTransactionType transaction =
          this.perTestResources.add(connection.openTransaction());
        return transaction;
      }

      throw new IllegalStateException(
        "Unrecognized requested parameter type: %s".formatted(requiredType)
      );
    } catch (final EISDatabaseException e) {
      throw new ParameterResolutionException(e.getMessage(), e);
    }
  }
}
