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

import com.io7m.idstore.database.api.IdDatabaseConfiguration;
import com.io7m.idstore.database.api.IdDatabaseCreate;
import com.io7m.idstore.database.api.IdDatabaseUpgrade;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.database.postgres.IdDatabases;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.server.IdServers;
import com.io7m.idstore.server.api.IdServerBrandingConfiguration;
import com.io7m.idstore.server.api.IdServerConfiguration;
import com.io7m.idstore.server.api.IdServerHTTPServiceConfiguration;
import com.io7m.idstore.server.api.IdServerHistoryConfiguration;
import com.io7m.idstore.server.api.IdServerMailConfiguration;
import com.io7m.idstore.server.api.IdServerMailTransportSMTP;
import com.io7m.idstore.server.api.IdServerRateLimitConfiguration;
import com.io7m.idstore.server.api.IdServerType;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jmulticlose.core.ClosingResourceFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class EITestIdstore implements AutoCloseable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EITestIdstore.class);

  private final PostgreSQLContainer<?> container;
  private final IdServerType server;
  private final CloseableCollectionType<ClosingResourceFailedException> resources;
  private final UUID adminId;

  private EITestIdstore(
    final PostgreSQLContainer<?> inContainer,
    final IdServerType inServer,
    final CloseableCollectionType<ClosingResourceFailedException> inResources,
    final UUID inAdminId)
  {
    this.container =
      Objects.requireNonNull(inContainer, "container");
    this.server =
      Objects.requireNonNull(inServer, "server");
    this.resources =
      Objects.requireNonNull(inResources, "resources");
    this.adminId =
      Objects.requireNonNull(inAdminId, "adminId");
  }

  private static IdServerType createIdstoreServer(
    final EIFakeClock clock,
    final PostgreSQLContainer<?> inContainer)
  {
    LOG.debug("creating idstore server");

    final var databaseConfiguration =
      new IdDatabaseConfiguration(
        "postgres",
        "12345678",
        inContainer.getHost(),
        inContainer.getFirstMappedPort().intValue(),
        "idstore",
        IdDatabaseCreate.CREATE_DATABASE,
        IdDatabaseUpgrade.UPGRADE_DATABASE,
        clock
      );

    final var mailService =
      new IdServerMailConfiguration(
        new IdServerMailTransportSMTP("localhost", 25000),
        Optional.empty(),
        "no-reply@example.com",
        Duration.ofDays(1L)
      );

    final var userApiService =
      new IdServerHTTPServiceConfiguration(
        "localhost",
        50000,
        URI.create("http://localhost:50000/"),
        Optional.empty()
      );
    final var userViewService =
      new IdServerHTTPServiceConfiguration(
        "localhost",
        50001,
        URI.create("http://localhost:50001/"),
        Optional.empty()
      );
    final var adminApiService =
      new IdServerHTTPServiceConfiguration(
        "localhost",
        51000,
        URI.create("http://localhost:51000/"),
        Optional.empty()
      );

    final var branding =
      new IdServerBrandingConfiguration(
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
      );

    final var history =
      new IdServerHistoryConfiguration(100, 100);

    final var rateLimit =
      new IdServerRateLimitConfiguration(
        Duration.of(10L, ChronoUnit.MINUTES),
        Duration.of(10L, ChronoUnit.MINUTES)
      );

    return new IdServers().createServer(
      new IdServerConfiguration(
        Locale.getDefault(),
        clock,
        new IdDatabases(),
        databaseConfiguration,
        mailService,
        userApiService,
        userViewService,
        adminApiService,
        branding,
        history,
        rateLimit,
        Optional.empty()
      )
    );
  }

  public static EITestIdstore create(
    final PostgreSQLContainer<?> container,
    final EIFakeClock clock)
    throws Exception
  {
    final var resources =
      CloseableCollection.create();

    waitForDatabaseToStart(container);

    container.addEnv("PGPASSWORD", "12345678");

    final var r =
      container.execInContainer("createdb", "-U", "postgres", "idstore");

    assertEquals(0, r.getExitCode());

    final var server =
      resources.add(createIdstoreServer(clock, container));

    final var adminId = UUID.randomUUID();
    server.setup(
      Optional.of(adminId),
      new IdName("someone"),
      new IdEmail("someone@example.org"),
      new IdRealName("Someone"),
      "12345678"
    );

    return new EITestIdstore(container, server, resources, adminId);
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

  public IdServerType idstore()
  {
    return this.server;
  }

  public URI baseURI()
  {
    return URI.create("http://localhost:50000/");
  }

  /**
   * @return The initial admin ID
   */

  public UUID adminId()
  {
    return this.adminId;
  }

  @Override
  public void close()
    throws Exception
  {
    this.resources.close();
  }

  public UUID createUser(
    final String name,
    final String password)
    throws Exception
  {
    final var db = this.server.database();
    try (var c = db.openConnection(IDSTORE)) {
      try (var t = c.openTransaction()) {
        t.adminIdSet(this.adminId);
        final var u =
          t.queries(IdDatabaseUsersQueriesType.class);

        final var user =
          u.userCreate(
            new IdName(name),
            new IdRealName(name),
            new IdEmail("%s@example.org".formatted(name)),
            IdPasswordAlgorithmPBKDF2HmacSHA256.create().createHashed(password)
          );
        t.commit();
        return user.id();
      }
    }
  }
}
