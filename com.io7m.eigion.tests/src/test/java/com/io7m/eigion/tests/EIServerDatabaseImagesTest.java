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

import com.io7m.eigion.hash.EIHash;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseImagesQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseUsersQueriesType;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static com.io7m.eigion.model.EIRedaction.redactionOpt;
import static com.io7m.eigion.model.EIRedactionRequest.redactionRequestOpt;
import static com.io7m.eigion.server.database.api.EIServerDatabaseIncludeRedacted.EXCLUDE_REDACTED;
import static com.io7m.eigion.server.database.api.EIServerDatabaseIncludeRedacted.INCLUDE_REDACTED;
import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.EIGION;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class EIServerDatabaseImagesTest extends EIWithDatabaseContract
{
  /**
   * Creating images works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testImagesCreate()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);
    final var images =
      transaction.queries(EIServerDatabaseImagesQueriesType.class);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    transaction.adminIdSet(adminId);

    final var user =
      users.userCreate(
        "someone",
        "someone@example.com",
        databaseGenerateBadPassword());

    transaction.userIdSet(user.id());

    final var id =
      randomUUID();
    final var hash =
      EIHash.sha256Of("hello".getBytes(UTF_8));
    final var image =
      images.imageCreate(id, hash);

    assertEquals(id, image.id());
    assertEquals(hash, image.hash());
    assertEquals(user.id(), image.creation().creator());

    final var image2 =
      images.imageGet(id, INCLUDE_REDACTED)
        .orElseThrow();

    assertEquals(image.creation(), image2.creation());
    assertEquals(image.id(), image2.id());
    assertEquals(image.redaction(), image2.redaction());
    assertEquals(image.hash(), image2.hash());

    {
      final var ex = assertThrows(EIServerDatabaseException.class, () -> {
        images.imageCreate(id, hash);
      });
      assertEquals("image-duplicate", ex.errorCode());
    }

    checkAuditLog(
      transaction,
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
      new ExpectedEvent("USER_CREATED", user.id().toString()),
      new ExpectedEvent("IMAGE_CREATED", id.toString())
    );
  }

  /**
   * Redacting images works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testImagesRedact()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);
    final var images =
      transaction.queries(EIServerDatabaseImagesQueriesType.class);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    transaction.adminIdSet(adminId);

    final var user =
      users.userCreate(
        "someone",
        "someone@example.com",
        databaseGenerateBadPassword());

    transaction.userIdSet(user.id());

    final var imageId =
      randomUUID();
    final var hash =
      EIHash.sha256Of("hello".getBytes(UTF_8));
    final var image =
      images.imageCreate(imageId, hash);

    assertTrue(images.imageGet(imageId, INCLUDE_REDACTED).isPresent());
    assertTrue(images.imageGet(imageId, EXCLUDE_REDACTED).isPresent());

    final var time = timeNow();
    images.imageRedact(imageId, redactionRequestOpt(time, "X"));

    assertTrue(images.imageGet(imageId, INCLUDE_REDACTED).isPresent());
    assertFalse(images.imageGet(imageId, EXCLUDE_REDACTED).isPresent());
    assertEquals(
      redactionOpt(user.id(), time, "X"),
      images.imageGet(imageId, INCLUDE_REDACTED).orElseThrow().redaction()
    );

    images.imageRedact(imageId, Optional.empty());
    assertTrue(images.imageGet(imageId, INCLUDE_REDACTED).isPresent());
    assertTrue(images.imageGet(imageId, EXCLUDE_REDACTED).isPresent());

    assertEquals(
      Optional.empty(),
      images.imageGet(imageId, INCLUDE_REDACTED).orElseThrow().redaction()
    );

    {
      final var ex = assertThrows(EIServerDatabaseException.class, () -> {
        images.imageRedact(randomUUID(), Optional.empty());
      });
      assertEquals("image-nonexistent", ex.errorCode());
    }

    checkAuditLog(
      transaction,
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
      new ExpectedEvent("USER_CREATED", user.id().toString()),
      new ExpectedEvent("IMAGE_CREATED", imageId.toString()),
      new ExpectedEvent("IMAGE_REDACTED", imageId.toString()),
      new ExpectedEvent("IMAGE_UNREDACTED", imageId.toString())
    );
  }
}
