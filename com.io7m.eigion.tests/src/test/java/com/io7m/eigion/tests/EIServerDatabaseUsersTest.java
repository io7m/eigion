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

import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseUsersQueriesType;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.EIGION;
import static java.time.OffsetDateTime.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class EIServerDatabaseUsersTest extends EIWithDatabaseContract
{
  /**
   * Setting the transaction user to a nonexistent user fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserSetNonexistent()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

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
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      databaseGenerateBadPassword();

    var user =
      users.userCreate(
        reqId,
        "someone",
        "someone@example.com",
        now,
        password
      );

    assertEquals("someone", user.name().value());
    assertEquals(reqId, user.id());
    assertEquals("someone@example.com", user.email().value());
    assertEquals(now.toEpochSecond(), user.created().toEpochSecond());
    assertEquals(now.toEpochSecond(), user.lastLoginTime().toEpochSecond());
    assertFalse(user.ban().isPresent());

    user = users.userGet(reqId).orElseThrow();
    assertEquals("someone", user.name().value());
    assertEquals(reqId, user.id());
    assertEquals("someone@example.com", user.email().value());
    assertEquals(now.toEpochSecond(), user.created().toEpochSecond());
    assertEquals(now.toEpochSecond(), user.lastLoginTime().toEpochSecond());
    assertFalse(user.ban().isPresent());

    user = users.userGetForEmail("someone@example.com").orElseThrow();
    assertEquals("someone", user.name().value());
    assertEquals(reqId, user.id());
    assertEquals("someone@example.com", user.email().value());
    assertEquals(now.toEpochSecond(), user.created().toEpochSecond());
    assertEquals(now.toEpochSecond(), user.lastLoginTime().toEpochSecond());
    assertFalse(user.ban().isPresent());

    user = users.userGetForName("someone").orElseThrow();
    assertEquals("someone", user.name().value());
    assertEquals(reqId, user.id());
    assertEquals("someone@example.com", user.email().value());
    assertEquals(now.toEpochSecond(), user.created().toEpochSecond());
    assertEquals(now.toEpochSecond(), user.lastLoginTime().toEpochSecond());
    assertFalse(user.ban().isPresent());

    checkAuditLog(
      transaction,
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
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
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      databaseGenerateBadPassword();

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
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      databaseGenerateBadPassword();

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
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      databaseGenerateBadPassword();

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
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var id =
      randomUUID();
    final var now =
      now();

    final var password =
      databaseGenerateBadPassword();

    var user =
      users.userCreate(
        id,
        "someone",
        "someone@example.com",
        now,
        password
      );

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
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
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
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var id =
      randomUUID();
    final var now =
      now();

    final var password =
      databaseGenerateBadPassword();

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
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
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
    assertTrue(this.containerIsRunning());

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
}
