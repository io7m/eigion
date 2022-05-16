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
import com.io7m.eigion.server.database.api.EIServerDatabaseConfiguration;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseProductsQueriesType;
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

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.io7m.eigion.server.database.api.EIServerDatabaseCreate.CREATE_DATABASE;
import static com.io7m.eigion.server.database.api.EIServerDatabaseProductsQueriesType.IncludeRedacted.EXCLUDE_REDACTED;
import static com.io7m.eigion.server.database.api.EIServerDatabaseProductsQueriesType.IncludeRedacted.INCLUDE_REDACTED;
import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.EIGION;
import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.NONE;
import static com.io7m.eigion.server.database.api.EIServerDatabaseUpgrade.UPGRADE_DATABASE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class EIServerDatabaseTest
{
  private static final EIServerDatabases DATABASES =
    new EIServerDatabases();

  @Container
  private final PostgreSQLContainer<?> container =
    new PostgreSQLContainer<>("postgres")
      .withDatabaseName("postgres")
      .withUsername("postgres")
      .withPassword("12345678");

  private CloseableCollectionType<ClosingResourceFailedException> resources;

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
          container.getFirstMappedPort(),
          "postgres",
          CREATE_DATABASE,
          UPGRADE_DATABASE,
          Clock.systemUTC()
        )
      ));
  }

  private EIServerDatabaseTransactionType transactionOf(
    final PostgreSQLContainer<?> container,
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
   * The none role cannot manipulate users.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserNone()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var transaction =
      this.transactionOf(this.container, NONE);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    var ex =
      assertThrows(EIServerDatabaseException.class, () -> {
        users.userCreate(
          UUID.randomUUID(),
          "someone",
          "someone@example.com",
          OffsetDateTime.now(),
          EIPassword.createHashed("12345678")
        );
      });
    assertEquals("sql-error", ex.errorCode());

    ex = assertThrows(EIServerDatabaseException.class, () -> {
      users.userGet(UUID.randomUUID());
    });
    assertEquals("sql-error", ex.errorCode());

    ex = assertThrows(EIServerDatabaseException.class, () -> {
      users.userGetForEmail("someone@example.com");
    });
    assertEquals("sql-error", ex.errorCode());

    ex = assertThrows(EIServerDatabaseException.class, () -> {
      users.userGetForName("someone");
    });
    assertEquals("sql-error", ex.errorCode());

    ex = assertThrows(EIServerDatabaseException.class, () -> {
      users.userBan(
        UUID.randomUUID(),
        Optional.of(OffsetDateTime.now()),
        "reason");
    });
    assertEquals("sql-error", ex.errorCode());

    ex = assertThrows(EIServerDatabaseException.class, () -> {
      users.userUnban(UUID.randomUUID());
    });
    assertEquals("sql-error", ex.errorCode());
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
      this.transactionOf(this.container, EIGION);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var reqId =
      UUID.randomUUID();
    final var now =
      OffsetDateTime.now();

    final var password =
      EIPassword.createHashed("12345678");

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
    assertEquals(now.toEpochSecond(), user.lastLogin().toEpochSecond());
    assertFalse(user.ban().isPresent());

    user = users.userGet(reqId).orElseThrow();
    assertEquals("someone", user.name());
    assertEquals(reqId, user.id());
    assertEquals("someone@example.com", user.email());
    assertEquals(now.toEpochSecond(), user.created().toEpochSecond());
    assertEquals(now.toEpochSecond(), user.lastLogin().toEpochSecond());
    assertFalse(user.ban().isPresent());

    user = users.userGetForEmail("someone@example.com").orElseThrow();
    assertEquals("someone", user.name());
    assertEquals(reqId, user.id());
    assertEquals("someone@example.com", user.email());
    assertEquals(now.toEpochSecond(), user.created().toEpochSecond());
    assertEquals(now.toEpochSecond(), user.lastLogin().toEpochSecond());
    assertFalse(user.ban().isPresent());

    user = users.userGetForName("someone").orElseThrow();
    assertEquals("someone", user.name());
    assertEquals(reqId, user.id());
    assertEquals("someone@example.com", user.email());
    assertEquals(now.toEpochSecond(), user.created().toEpochSecond());
    assertEquals(now.toEpochSecond(), user.lastLogin().toEpochSecond());
    assertFalse(user.ban().isPresent());
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
      this.transactionOf(this.container, EIGION);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var reqId =
      UUID.randomUUID();
    final var now =
      OffsetDateTime.now();

    final var password =
      EIPassword.createHashed("12345678");

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
      this.transactionOf(this.container, EIGION);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var reqId =
      UUID.randomUUID();
    final var now =
      OffsetDateTime.now();

    final var password =
      EIPassword.createHashed("12345678");

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
          UUID.randomUUID(),
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
      this.transactionOf(this.container, EIGION);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var reqId =
      UUID.randomUUID();
    final var now =
      OffsetDateTime.now();

    final var password =
      EIPassword.createHashed("12345678");

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
          UUID.randomUUID(),
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
      this.transactionOf(this.container, EIGION);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var id =
      UUID.randomUUID();
    final var now =
      OffsetDateTime.now();

    final var password =
      EIPassword.createHashed("12345678");

    var user =
      users.userCreate(
        id,
        "someone",
        "someone@example.com",
        now,
        password
      );

    final var expires = OffsetDateTime.now().plusDays(1L);
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
  }

  /**
   * The none role cannot manipulate categories.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCategoriesNone()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var transaction =
      this.transactionOf(this.container, NONE);
    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);

    var ex =
      assertThrows(EIServerDatabaseException.class, () -> {
        products.categoryCreate("Category 0");
      });
    assertEquals("sql-error", ex.errorCode());

    ex = assertThrows(EIServerDatabaseException.class, () -> {
      products.categoryRedact("Category 0", true);
    });
    assertEquals("sql-error", ex.errorCode());

    ex = assertThrows(EIServerDatabaseException.class, () -> {
      products.categories(INCLUDE_REDACTED);
    });
    assertEquals("sql-error", ex.errorCode());
  }

  /**
   * Creating and redacting categories works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCategories()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var transaction =
      this.transactionOf(this.container, EIGION);
    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);

    final var category0 =
      products.categoryCreate("Category 0");
    final var category1 =
      products.categoryCreate("Category 1");
    final var category2 =
      products.categoryCreate("Category 2");

    assertEquals(
      Set.of(category0, category1, category2),
      products.categories(INCLUDE_REDACTED)
    );
    assertEquals(
      Set.of(category0, category1, category2),
      products.categories(EXCLUDE_REDACTED)
    );

    products.categoryRedact(category0, true);

    assertEquals(
      Set.of(category0, category1, category2),
      products.categories(INCLUDE_REDACTED)
    );
    assertEquals(
      Set.of(category1, category2),
      products.categories(EXCLUDE_REDACTED)
    );

    products.categoryRedact(category1, true);

    assertEquals(
      Set.of(category0, category1, category2),
      products.categories(INCLUDE_REDACTED)
    );
    assertEquals(
      Set.of(category2),
      products.categories(EXCLUDE_REDACTED)
    );

    products.categoryRedact(category0, false);

    assertEquals(
      Set.of(category0, category1, category2),
      products.categories(INCLUDE_REDACTED)
    );
    assertEquals(
      Set.of(category0, category2),
      products.categories(EXCLUDE_REDACTED)
    );

    {
      final var ex =
        assertThrows(EIServerDatabaseException.class, () -> {
          products.categoryRedact("nonexistent", true);
        });
      assertEquals("category-nonexistent", ex.errorCode());
    }
  }
}
