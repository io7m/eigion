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

import com.io7m.eigion.model.EIGroupPrefix;
import com.io7m.eigion.model.EIPassword;
import com.io7m.eigion.model.EIPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.eigion.model.EIPasswordException;
import com.io7m.eigion.server.api.EIServerConfiguration;
import com.io7m.eigion.server.api.EIServerException;
import com.io7m.eigion.server.api.EIServerType;
import com.io7m.eigion.server.database.api.EIServerDatabaseAdminsQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseConfiguration;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseUsersQueriesType;
import com.io7m.eigion.server.database.postgres.EIServerDatabases;
import com.io7m.eigion.server.vanilla.EIServers;
import com.io7m.eigion.storage.api.EIStorageParameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.io7m.eigion.server.database.api.EIServerDatabaseCreate.CREATE_DATABASE;
import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.EIGION;
import static com.io7m.eigion.server.database.api.EIServerDatabaseUpgrade.UPGRADE_DATABASE;

@Testcontainers(disabledWithoutDocker = true)
public abstract class EIWithServerContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIWithServerContract.class);

  @Container
  private final PostgreSQLContainer<?> container =
    new PostgreSQLContainer<>("postgres")
      .withDatabaseName("postgres")
      .withUsername("postgres")
      .withPassword("12345678");

  private EICapturingDatabases databases;
  private EIFakeStorageFactory storage;
  private EIServerType server;
  private EIServers servers;
  private Path directory;
  private EIFakeDomainCheckers domainCheckers;
  private AtomicBoolean started;

  private static EIPassword createBadPassword()
    throws EIPasswordException
  {
    return EIPasswordAlgorithmPBKDF2HmacSHA256.create()
      .createHashed("12345678");
  }

  protected static OffsetDateTime timeNow()
  {
    return OffsetDateTime.now(Clock.systemUTC()).withNano(0);
  }

  protected final UUID serverCreateUser(
    final UUID admin,
    final String name)
    throws EIServerDatabaseException, EIPasswordException
  {
    this.serverStartIfNecessary();

    final var database = this.databases.mostRecent();
    try (var connection = database.openConnection(EIGION)) {
      try (var transaction = connection.openTransaction()) {
        final var users =
          transaction.queries(EIServerDatabaseUsersQueriesType.class);
        transaction.adminIdSet(admin);

        final var userId = UUID.randomUUID();
        users.userCreate(
          userId,
          name,
          "%s@example.com".formatted(name),
          timeNow(),
          createBadPassword()
        );
        transaction.commit();
        return userId;
      }
    }
  }

  public final PostgreSQLContainer<?> container()
  {
    return this.container;
  }

  public final EIServerType server()
  {
    return this.server;
  }

  public final EIFakeDomainCheckers domainCheckers()
  {
    return this.domainCheckers;
  }

  @BeforeEach
  public final void serverSetup()
    throws Exception
  {
    LOG.debug("serverSetup");

    this.waitForDatabaseToStart();

    this.started =
      new AtomicBoolean(false);
    this.directory =
      EITestDirectories.createTempDirectory();
    this.servers =
      new EIServers();
    this.databases =
      new EICapturingDatabases(new EIServerDatabases());
    this.storage =
      new EIFakeStorageFactory();
    this.domainCheckers =
      new EIFakeDomainCheckers();
    this.server =
      this.createServer();
  }

  private void waitForDatabaseToStart()
    throws InterruptedException, TimeoutException
  {
    LOG.debug("waiting for database to start");
    final var timeWait = Duration.ofSeconds(60L);
    final var timeThen = Instant.now();
    while (!this.container.isRunning()) {
      Thread.sleep(1L);
      final var timeNow = Instant.now();
      if (Duration.between(timeThen, timeNow).compareTo(timeWait) > 0) {
        LOG.error("timed out waiting for database to start");
        throw new TimeoutException("Timed out waiting for database to start");
      }
    }
    LOG.debug("database started");
  }

  @AfterEach
  public final void serverTearDown()
    throws Exception
  {
    LOG.debug("serverTearDown");

    this.server.close();
  }

  private EIServerType createServer()
  {
    LOG.debug("creating server");

    final var databaseConfiguration =
      new EIServerDatabaseConfiguration(
        "postgres",
        "12345678",
        this.container.getContainerIpAddress(),
        this.container.getFirstMappedPort().intValue(),
        "postgres",
        CREATE_DATABASE,
        UPGRADE_DATABASE,
        Clock.systemUTC()
      );

    final var adminAddress =
      new InetSocketAddress("localhost", 40000);
    final var publicAddress =
      new InetSocketAddress("localhost", 40001);

    return this.servers.createServer(
      new EIServerConfiguration(
        Locale.getDefault(),
        Clock.systemUTC(),
        this.domainCheckers,
        this.databases,
        databaseConfiguration,
        this.storage,
        new EIStorageParameters(Map.of()),
        adminAddress,
        publicAddress,
        this.directory,
        new EIGroupPrefix("com.eigion.users.")
      )
    );
  }

  protected final URI serverPublicURI()
  {
    return URI.create("http://localhost:40001/");
  }

  protected final URI serverAdminURI()
  {
    return URI.create("http://localhost:40000/");
  }

  protected final UUID serverCreateAdminInitial(
    final String user,
    final String pass)
    throws Exception
  {
    this.serverStartIfNecessary();

    final var database = this.databases.mostRecent();
    try (var c = database.openConnection(EIGION)) {
      try (var t = c.openTransaction()) {
        final var q =
          t.queries(EIServerDatabaseAdminsQueriesType.class);

        final var password =
          EIPasswordAlgorithmPBKDF2HmacSHA256.create()
            .createHashed(pass);

        final var id = UUID.randomUUID();
        q.adminCreateInitial(
          id,
          user,
          id + "@example.com",
          OffsetDateTime.now(),
          password
        );
        t.commit();
        return id;
      }
    }
  }

  protected final void serverStartIfNecessary()
  {
    if (this.started.compareAndSet(false, true)) {
      try {
        this.server.start();
      } catch (final EIServerException e) {
        this.started.set(false);
        throw new IllegalStateException(e);
      }
    }
  }
}
