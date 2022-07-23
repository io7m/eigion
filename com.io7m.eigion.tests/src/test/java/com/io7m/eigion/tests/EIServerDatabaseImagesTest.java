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
import com.io7m.eigion.model.EIPassword;
import com.io7m.eigion.model.EIPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.eigion.model.EIPasswordException;
import com.io7m.eigion.model.EISubsetMatch;
import com.io7m.eigion.server.database.api.EIServerDatabaseAuditQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseConfiguration;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseImagesQueriesType;
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

import static com.io7m.eigion.model.EIRedaction.redactionOpt;
import static com.io7m.eigion.model.EIRedactionRequest.redactionRequestOpt;
import static com.io7m.eigion.server.database.api.EIServerDatabaseCreate.CREATE_DATABASE;
import static com.io7m.eigion.server.database.api.EIServerDatabaseIncludeRedacted.EXCLUDE_REDACTED;
import static com.io7m.eigion.server.database.api.EIServerDatabaseIncludeRedacted.INCLUDE_REDACTED;
import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.EIGION;
import static com.io7m.eigion.server.database.api.EIServerDatabaseUpgrade.UPGRADE_DATABASE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.OffsetDateTime.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class EIServerDatabaseImagesTest
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

  private static EIPassword createBadPassword()
    throws EIPasswordException
  {
    return EIPasswordAlgorithmPBKDF2HmacSHA256.create()
      .createHashed("12345678");
  }

  /**
   * Creating images works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testImagesCreate()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var transaction =
      this.transactionOf(EIGION);
    final var images =
      transaction.queries(EIServerDatabaseImagesQueriesType.class);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var user =
      users.userCreate(
        "someone",
        "someone@example.com",
        createBadPassword());

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
    assertTrue(this.container.isRunning());

    final var transaction =
      this.transactionOf(EIGION);
    final var images =
      transaction.queries(EIServerDatabaseImagesQueriesType.class);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var user =
      users.userCreate(
        "someone",
        "someone@example.com",
        createBadPassword());

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
      new ExpectedEvent("USER_CREATED", user.id().toString()),
      new ExpectedEvent("IMAGE_CREATED", imageId.toString()),
      new ExpectedEvent("IMAGE_REDACTED", imageId.toString()),
      new ExpectedEvent("IMAGE_UNREDACTED", imageId.toString())
    );
  }

  private record ExpectedEvent(
    String type,
    String message)
  {

  }
}
