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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

public final class EITestDatabase implements AutoCloseable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EITestDatabase.class);

  private final PostgreSQLContainer<?> container;
  private final EISDatabaseType database;
  private final CloseableCollectionType<ClosingResourceFailedException> resources;

  private EITestDatabase(
    final PostgreSQLContainer<?> inContainer,
    final EISDatabaseType inDatabase,
    final CloseableCollectionType<ClosingResourceFailedException> inResources)
  {
    this.container =
      Objects.requireNonNull(inContainer, "container");
    this.database =
      Objects.requireNonNull(inDatabase, "database");
    this.resources =
      Objects.requireNonNull(inResources, "resources");
  }

  public static EITestDatabase create(
    final PostgreSQLContainer<?> container,
    final EIFakeClock clock)
    throws Exception
  {
    final var resources =
      CloseableCollection.create();

    waitForDatabaseToStart(container);

    container.addEnv("PGPASSWORD", "12345678");
    container.execInContainer("createdb", "-U", "postgres", "-w", "idstore");

    final var databaseConfiguration =
      new EISDatabaseConfiguration(
        "postgres",
        "12345678",
        container.getHost(),
        container.getFirstMappedPort().intValue(),
        "idstore",
        EISDatabaseCreate.CREATE_DATABASE,
        EISDatabaseUpgrade.UPGRADE_DATABASE,
        clock
      );

    final var databases = new EISDatabases();
    final var database =
      resources.add(
        databases.open(databaseConfiguration, OpenTelemetry.noop(), s -> {

        }));

    return new EITestDatabase(container, database, resources);
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

  public <T, E extends Exception> T withTransaction(
    final WithTransactionType<T, E> f)
    throws EISDatabaseException, E
  {
    try (var c = this.database.openConnection(EISDatabaseRole.EIGION)) {
      try (var t = c.openTransaction()) {
        return f.execute(t);
      }
    }
  }

  @Override
  public void close()
    throws Exception
  {
    this.resources.close();
  }

  public interface WithTransactionType<T, E extends Exception>
  {
    T execute(EISDatabaseTransactionType transaction)
      throws E, EISDatabaseException;
  }
}
