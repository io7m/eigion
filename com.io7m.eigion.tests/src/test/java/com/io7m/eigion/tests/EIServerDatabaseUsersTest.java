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

import com.io7m.eigion.model.EIPassword;
import com.io7m.eigion.model.EIPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.eigion.model.EIPasswordException;
import com.io7m.eigion.model.EISubsetMatch;
import com.io7m.eigion.server.database.api.EIServerDatabaseAuditQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseConfiguration;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseRole;
import com.io7m.eigion.server.database.api.EIServerDatabaseTransactionType;
import com.io7m.eigion.server.database.api.EIServerDatabaseType;
import com.io7m.eigion.server.database.api.EIServerDatabaseUsersQueriesType;
import com.io7m.eigion.server.database.postgres.EIServerDatabases;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jmulticlose.core.ClosingResourceFailedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

import static com.io7m.eigion.server.database.api.EIServerDatabaseCreate.CREATE_DATABASE;
import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.EIGION;
import static com.io7m.eigion.server.database.api.EIServerDatabaseUpgrade.UPGRADE_DATABASE;
import static java.time.OffsetDateTime.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class EIServerDatabaseUsersTest
{
  private static final EIServerDatabases DATABASES =
    new EIServerDatabases();

  @Container
  private final PostgreSQLContainer<?> container =
    new PostgreSQLContainer<>(DockerImageName.parse("postgres").withTag("14.4"))
      .withDatabaseName("postgres")
      .withUsername("postgres")
      .withPassword("12345678");

  private CloseableCollectionType<ClosingResourceFailedException> resources;

  private static void checkAuditLog(
    final EIServerDatabaseTransactionType transaction,
    final ExpectedEvent... expectedEvents)
    throws EIServerDatabaseException
  {
    final var audit =
      transaction.queries(EIServerDatabaseAuditQueriesType.class);
    final var events =
      audit.auditEvents(
        timeNow().minusYears(1L),
        timeNow().plusYears(1L),
        new EISubsetMatch<>("", ""),
        new EISubsetMatch<>("", ""),
        new EISubsetMatch<>("", "")
      );

    for (var index = 0; index < expectedEvents.length; ++index) {
      final var event =
        events.get(index);
      final var expect =
        expectedEvents[index];

      assertEquals(
        expect.type,
        event.type(),
        String.format(
          "Event [%d] %s type must be %s",
          Integer.valueOf(index),
          event,
          expect.type)
      );

      if (expect.message != null) {
        assertEquals(
          expect.message,
          event.message(),
          String.format(
            "Event [%d] %s message must be %s",
            Integer.valueOf(index),
            event,
            expect.message)
        );
      }
    }
  }

  private static OffsetDateTime timeNow()
  {
    /*
     * Postgres doesn't store times at as high a resolution as the JVM,
     * so trim the nanoseconds off in order to ensure we can correctly
     * compare results returned from the database.
     */

    return now().withNano(0);
  }

  private EIServerDatabaseType databaseOf(
    final PostgreSQLContainer<?> container)
    throws EIServerDatabaseException
  {
    return this.resources.add(
      DATABASES.open(
        new EIServerDatabaseConfiguration(
          "postgres",
          "12345678",
          container.getContainerIpAddress(),
          container.getFirstMappedPort().intValue(),
          "postgres",
          CREATE_DATABASE,
          UPGRADE_DATABASE,
          Clock.systemUTC()
        ),
        message -> {

        }
      ));
  }

  private EIServerDatabaseTransactionType transactionOf(
    final EIServerDatabaseRole role)
    throws EIServerDatabaseException
  {
    final var database =
      this.databaseOf(this.container);
    final var connection =
      this.resources.add(database.openConnection(role));
    return this.resources.add(connection.openTransaction());
  }

  @BeforeEach
  public void setup()
  {
    this.resources = CloseableCollection.create();
  }

  @AfterEach
  public void tearDown()
    throws ClosingResourceFailedException
  {
    this.resources.close();
  }

  /**
   * Setting the transaction user to a nonexistent user fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserSetNonexistent()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var transaction =
      this.transactionOf(EIGION);

    final var ex =
      assertThrows(EIServerDatabaseException.class, () -> {
        transaction.userIdSet(randomUUID());
      });
    assertEquals("user-nonexistent", ex.errorCode());
  }

  /**
   * Creating a user works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUser()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var transaction =
      this.transactionOf(EIGION);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      createBadPassword();

    var user =
      users.userCreate(
        reqId,
        "someone",
        "someone@example.com",
        now,
        password
      );

    assertEquals("someone", user.name());
    assertEquals(reqId, user.id());
    assertEquals("someone@example.com", user.email());
    assertEquals(now.toEpochSecond(), user.created().toEpochSecond());
    assertEquals(now.toEpochSecond(), user.lastLoginTime().toEpochSecond());
    assertFalse(user.ban().isPresent());

    user = users.userGet(reqId).orElseThrow();
    assertEquals("someone", user.name());
    assertEquals(reqId, user.id());
    assertEquals("someone@example.com", user.email());
    assertEquals(now.toEpochSecond(), user.created().toEpochSecond());
    assertEquals(now.toEpochSecond(), user.lastLoginTime().toEpochSecond());
    assertFalse(user.ban().isPresent());

    user = users.userGetForEmail("someone@example.com").orElseThrow();
    assertEquals("someone", user.name());
    assertEquals(reqId, user.id());
    assertEquals("someone@example.com", user.email());
    assertEquals(now.toEpochSecond(), user.created().toEpochSecond());
    assertEquals(now.toEpochSecond(), user.lastLoginTime().toEpochSecond());
    assertFalse(user.ban().isPresent());

    user = users.userGetForName("someone").orElseThrow();
    assertEquals("someone", user.name());
    assertEquals(reqId, user.id());
    assertEquals("someone@example.com", user.email());
    assertEquals(now.toEpochSecond(), user.created().toEpochSecond());
    assertEquals(now.toEpochSecond(), user.lastLoginTime().toEpochSecond());
    assertFalse(user.ban().isPresent());

    checkAuditLog(
      transaction,
      new ExpectedEvent("USER_CREATED", reqId.toString())
    );
  }

  /**
   * Creating a user with a duplicate ID fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserDuplicateId()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var transaction =
      this.transactionOf(EIGION);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      createBadPassword();

    final var user =
      users.userCreate(
        reqId,
        "someone",
        "someone@example.com",
        now,
        password
      );

    final var ex =
      assertThrows(EIServerDatabaseException.class, () -> {
        users.userCreate(
          reqId,
          "someoneElse",
          "someone2@example.com",
          now,
          password
        );
      });

    assertEquals("user-duplicate-id", ex.errorCode());
  }

  /**
   * Creating a user with a duplicate email fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserDuplicateEmail()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var transaction =
      this.transactionOf(EIGION);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      createBadPassword();

    final var user =
      users.userCreate(
        reqId,
        "someone",
        "someone@example.com",
        now,
        password
      );

    final var ex =
      assertThrows(EIServerDatabaseException.class, () -> {
        users.userCreate(
          randomUUID(),
          "someoneElse",
          "someone@example.com",
          now,
          password
        );
      });

    assertEquals("user-duplicate-email", ex.errorCode());
  }

  /**
   * Creating a user with a duplicate name fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserDuplicateName()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var transaction =
      this.transactionOf(EIGION);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      createBadPassword();

    final var user =
      users.userCreate(
        reqId,
        "someone",
        "someone@example.com",
        now,
        password
      );

    final var ex =
      assertThrows(EIServerDatabaseException.class, () -> {
        users.userCreate(
          randomUUID(),
          "someone",
          "someone2@example.com",
          now,
          password
        );
      });

    assertEquals("user-duplicate-name", ex.errorCode());
  }

  /**
   * Banning a user works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserBan()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var transaction =
      this.transactionOf(EIGION);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var id =
      randomUUID();
    final var now =
      now();

    final var password =
      createBadPassword();

    var user =
      users.userCreate(
        id,
        "someone",
        "someone@example.com",
        now,
        password
      );

    transaction.userIdSet(user.id());

    final var expires = now().plusDays(1L);
    users.userBan(id, Optional.of(expires), "Did something bad.");

    user = users.userGet(id).orElseThrow();
    assertEquals("Did something bad.", user.ban().orElseThrow().reason());
    assertEquals(
      expires.toEpochSecond(),
      user.ban().orElseThrow().expires().orElseThrow().toEpochSecond()
    );

    users.userBan(id, Optional.of(expires), "Did something else bad.");
    user = users.userGet(id).orElseThrow();
    assertEquals("Did something else bad.", user.ban().orElseThrow().reason());
    assertEquals(
      expires.toEpochSecond(),
      user.ban().orElseThrow().expires().orElseThrow().toEpochSecond()
    );

    users.userUnban(id);
    user = users.userGet(id).orElseThrow();
    assertEquals(Optional.empty(), user.ban());

    checkAuditLog(
      transaction,
      new ExpectedEvent("USER_CREATED", id.toString()),
      new ExpectedEvent("USER_BANNED", id + ": Did something bad."),
      new ExpectedEvent("USER_BANNED", id + ": Did something else bad."),
      new ExpectedEvent("USER_UNBANNED", id.toString())
    );
  }

  /**
   * Logging in works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserLogin()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var transaction =
      this.transactionOf(EIGION);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var id =
      randomUUID();
    final var now =
      now();

    final var password =
      createBadPassword();

    final var user =
      users.userCreate(
        id,
        "someone",
        "someone@example.com",
        now,
        password
      );

    users.userLogin(user.id(), "127.0.0.1");

    checkAuditLog(
      transaction,
      new ExpectedEvent("USER_CREATED", id.toString()),
      new ExpectedEvent("USER_LOGGED_IN", "127.0.0.1")
    );
  }

  /**
   * Logging in fails for nonexistent users.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserLoginNonexistent()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var transaction =
      this.transactionOf(EIGION);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var ex =
      assertThrows(EIServerDatabaseException.class, () -> {
        users.userLogin(randomUUID(), "127.0.0.1");
      });
    assertEquals("user-nonexistent", ex.errorCode());
  }

  private static EIPassword createBadPassword()
    throws EIPasswordException
  {
    return EIPasswordAlgorithmPBKDF2HmacSHA256.create()
      .createHashed("12345678");
  }

  private record ExpectedEvent(
    String type,
    String message)
  {

  }
}
