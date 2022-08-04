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
import com.io7m.eigion.server.database.api.EIServerDatabaseAdminsQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.ADMIN_DUPLICATE_EMAIL;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.ADMIN_DUPLICATE_ID;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.ADMIN_DUPLICATE_NAME;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.ADMIN_NONEXISTENT;
import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.EIGION;
import static java.time.OffsetDateTime.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class EIServerDatabaseAdminsTest extends EIWithDatabaseContract
{
  /**
   * Setting the transaction admin to a nonexistent admin fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminSetNonexistent()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(EIGION);

    final var ex =
      assertThrows(EIServerDatabaseException.class, () -> {
        transaction.adminIdSet(randomUUID());
      });
    assertEquals(ADMIN_NONEXISTENT, ex.errorCode());
  }

  /**
   * Creating an admin works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdmin()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(EIGION);
    final var admins =
      transaction.queries(EIServerDatabaseAdminsQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      createBadPassword();

    var admin =
      admins.adminCreate(
        reqId,
        "someone",
        "someone@example.com",
        now,
        password,
        Set.of()
      );

    assertEquals("someone", admin.name().value());
    assertEquals(reqId, admin.id());
    assertEquals("someone@example.com", admin.email().value());
    assertEquals(now.toEpochSecond(), admin.created().toEpochSecond());
    assertEquals(now.toEpochSecond(), admin.lastLoginTime().toEpochSecond());

    admin = admins.adminGet(reqId).orElseThrow();
    assertEquals("someone", admin.name().value());
    assertEquals(reqId, admin.id());
    assertEquals("someone@example.com", admin.email().value());
    assertEquals(now.toEpochSecond(), admin.created().toEpochSecond());
    assertEquals(now.toEpochSecond(), admin.lastLoginTime().toEpochSecond());

    admin = admins.adminGetForEmail("someone@example.com").orElseThrow();
    assertEquals("someone", admin.name().value());
    assertEquals(reqId, admin.id());
    assertEquals("someone@example.com", admin.email().value());
    assertEquals(now.toEpochSecond(), admin.created().toEpochSecond());
    assertEquals(now.toEpochSecond(), admin.lastLoginTime().toEpochSecond());

    admin = admins.adminGetForName("someone").orElseThrow();
    assertEquals("someone", admin.name().value());
    assertEquals(reqId, admin.id());
    assertEquals("someone@example.com", admin.email().value());
    assertEquals(now.toEpochSecond(), admin.created().toEpochSecond());
    assertEquals(now.toEpochSecond(), admin.lastLoginTime().toEpochSecond());

    checkAuditLog(
      transaction,
      new ExpectedEvent("ADMIN_CREATED", reqId.toString())
    );
  }

  /**
   * Creating an admin with a duplicate ID fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminDuplicateId()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(EIGION);
    final var admins =
      transaction.queries(EIServerDatabaseAdminsQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      createBadPassword();

    final var admin =
      admins.adminCreate(
        reqId,
        "someone",
        "someone@example.com",
        now,
        password,
        Set.of()
      );

    final var ex =
      assertThrows(EIServerDatabaseException.class, () -> {
        admins.adminCreate(
          reqId,
          "someoneElse",
          "someone2@example.com",
          now,
          password,
          Set.of()
        );
      });

    assertEquals(ADMIN_DUPLICATE_ID, ex.errorCode());
  }

  /**
   * Creating an admin with a duplicate email fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminDuplicateEmail()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(EIGION);
    final var admins =
      transaction.queries(EIServerDatabaseAdminsQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      createBadPassword();

    final var admin =
      admins.adminCreate(
        reqId,
        "someone",
        "someone@example.com",
        now,
        password,
        Set.of()
      );

    final var ex =
      assertThrows(EIServerDatabaseException.class, () -> {
        admins.adminCreate(
          randomUUID(),
          "someoneElse",
          "someone@example.com",
          now,
          password,
          Set.of()
        );
      });

    assertEquals(ADMIN_DUPLICATE_EMAIL, ex.errorCode());
  }

  /**
   * Creating an admin with a duplicate name fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminDuplicateName()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(EIGION);
    final var admins =
      transaction.queries(EIServerDatabaseAdminsQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      createBadPassword();

    final var admin =
      admins.adminCreate(
        reqId,
        "someone",
        "someone@example.com",
        now,
        password,
        Set.of()
      );

    final var ex =
      assertThrows(EIServerDatabaseException.class, () -> {
        admins.adminCreate(
          randomUUID(),
          "someone",
          "someone2@example.com",
          now,
          password,
          Set.of()
        );
      });

    assertEquals(ADMIN_DUPLICATE_NAME, ex.errorCode());
  }

  /**
   * Logging in works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminLogin()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(EIGION);
    final var admins =
      transaction.queries(EIServerDatabaseAdminsQueriesType.class);

    final var id =
      randomUUID();
    final var now =
      now();

    final var password =
      createBadPassword();

    final var admin =
      admins.adminCreate(
        id,
        "someone",
        "someone@example.com",
        now,
        password,
        Set.of()
      );

    admins.adminLogin(admin.id(), "127.0.0.1");

    checkAuditLog(
      transaction,
      new ExpectedEvent("ADMIN_CREATED", id.toString()),
      new ExpectedEvent("ADMIN_LOGGED_IN", "127.0.0.1")
    );
  }

  /**
   * Logging in fails for nonexistent admins.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminLoginNonexistent()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(EIGION);
    final var admins =
      transaction.queries(EIServerDatabaseAdminsQueriesType.class);

    final var ex =
      assertThrows(EIServerDatabaseException.class, () -> {
        admins.adminLogin(randomUUID(), "127.0.0.1");
      });
    assertEquals(ADMIN_NONEXISTENT, ex.errorCode());
  }

  private static EIPassword createBadPassword()
    throws EIPasswordException
  {
    return EIPasswordAlgorithmPBKDF2HmacSHA256.create()
      .createHashed("12345678");
  }
}
