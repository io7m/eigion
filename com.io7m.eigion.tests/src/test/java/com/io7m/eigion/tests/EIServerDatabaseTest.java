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
import com.io7m.eigion.model.EIProductCategory;
import com.io7m.eigion.model.EIProductIdentifier;
import com.io7m.eigion.model.EIRedaction;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.server.database.api.EIServerDatabaseAuditQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseConfiguration;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseImagesQueriesType;
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

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.io7m.eigion.model.EIRedaction.redactionOpt;
import static com.io7m.eigion.model.EIRedactionRequest.redactionRequest;
import static com.io7m.eigion.model.EIRedactionRequest.redactionRequestOpt;
import static com.io7m.eigion.server.database.api.EIServerDatabaseCreate.CREATE_DATABASE;
import static com.io7m.eigion.server.database.api.EIServerDatabaseIncludeRedacted.EXCLUDE_REDACTED;
import static com.io7m.eigion.server.database.api.EIServerDatabaseIncludeRedacted.INCLUDE_REDACTED;
import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.EIGION;
import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.NONE;
import static com.io7m.eigion.server.database.api.EIServerDatabaseUpgrade.UPGRADE_DATABASE;
import static java.time.OffsetDateTime.now;
import static java.util.UUID.randomUUID;
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

  private static void checkAuditLog(
    final EIServerDatabaseTransactionType transaction,
    final ExpectedEvent... expectedEvents)
    throws EIServerDatabaseException
  {
    final var audit =
      transaction.queries(EIServerDatabaseAuditQueriesType.class);
    final var events =
      audit.auditEvents(timeNow().minusYears(1L), timeNow().plusYears(1L));

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
  public void testUserUnprivileged()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var user =
      this.createTestUser();
    final var transaction =
      this.transactionOf(this.container, NONE);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    transaction.userIdSet(user.id());

    var ex =
      assertThrows(EIServerDatabaseException.class, () -> {
        users.userCreate(
          randomUUID(),
          "someone",
          "someone@example.com",
          timeNow(),
          EIPassword.createHashed("12345678")
        );
      });
    assertEquals("sql-error", ex.errorCode());

    ex = assertThrows(EIServerDatabaseException.class, () -> {
      users.userGet(randomUUID());
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
        randomUUID(),
        Optional.of(now()),
        "reason");
    });
    assertEquals("sql-error", ex.errorCode());

    ex = assertThrows(EIServerDatabaseException.class, () -> {
      users.userUnban(randomUUID());
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
      randomUUID();
    final var now =
      now();

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
      this.transactionOf(this.container, EIGION);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();

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
      randomUUID();
    final var now =
      now();

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
      this.transactionOf(this.container, EIGION);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();

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
      this.transactionOf(this.container, EIGION);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var id =
      randomUUID();
    final var now =
      now();

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
   * The none role cannot manipulate categories.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCategoriesUnprivileged()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var user =
      this.createTestUser();
    final var transaction =
      this.transactionOf(this.container, NONE);
    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);

    transaction.userIdSet(user.id());

    var ex =
      assertThrows(EIServerDatabaseException.class, () -> {
        products.categoryCreate("Category 0");
      });
    assertEquals("sql-error", ex.errorCode());

    ex = assertThrows(EIServerDatabaseException.class, () -> {
      products.categoryRedact(
        "Category 0",
        redactionRequestOpt(now(), "X"));
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
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var user =
      users.userCreate(
        "someone",
        "someone@example.com",
        EIPassword.createHashed("12345678")
      );

    transaction.userIdSet(user.id());

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

    final var redaction =
      EIRedaction.redaction(user.id(), timeNow(), "X");
    final var redactionReq =
      redactionRequestOpt(timeNow(), "X");

    products.categoryRedact(category0, redactionReq);

    assertEquals(
      Set.of(
        new EIProductCategory("Category 0", Optional.of(redaction)),
        category1,
        category2),
      products.categories(INCLUDE_REDACTED)
    );
    assertEquals(
      Set.of(category1, category2),
      products.categories(EXCLUDE_REDACTED)
    );

    products.categoryRedact(category1, redactionReq);

    assertEquals(
      Set.of(
        new EIProductCategory("Category 0", Optional.of(redaction)),
        new EIProductCategory("Category 1", Optional.of(redaction)),
        category2),
      products.categories(INCLUDE_REDACTED)
    );
    assertEquals(
      Set.of(category2),
      products.categories(EXCLUDE_REDACTED)
    );

    products.categoryRedact(category0, Optional.empty());

    assertEquals(
      Set.of(
        category0,
        new EIProductCategory("Category 1", Optional.of(redaction)),
        category2),
      products.categories(INCLUDE_REDACTED)
    );
    assertEquals(
      Set.of(
        category0,
        category2),
      products.categories(EXCLUDE_REDACTED)
    );

    {
      final var ex =
        assertThrows(EIServerDatabaseException.class, () -> {
          products.categoryRedact(
            "Nonexistent",
            redactionReq
          );
        });
      assertEquals("category-nonexistent", ex.errorCode());
    }

    checkAuditLog(
      transaction,
      new ExpectedEvent("USER_CREATED", user.id().toString()),
      new ExpectedEvent("CATEGORY_CREATED", "Category 0"),
      new ExpectedEvent("CATEGORY_CREATED", "Category 1"),
      new ExpectedEvent("CATEGORY_CREATED", "Category 2"),
      new ExpectedEvent("CATEGORY_REDACTED", "Category 0"),
      new ExpectedEvent("CATEGORY_REDACTED", "Category 1"),
      new ExpectedEvent("CATEGORY_UNREDACTED", "Category 0")
    );
  }

  /**
   * Creating products works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testProductCreation()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var transaction =
      this.transactionOf(this.container, EIGION);
    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var id =
      new EIProductIdentifier("com.io7m.ex", "com.q");

    final var user =
      users.userCreate(
        "someone",
        "someone@example.com",
        EIPassword.createHashed("12345678"));

    transaction.userIdSet(user.id());

    final var product =
      products.productCreate(id);

    assertEquals(id, product.id());
    assertEquals(List.of(), product.releases());
    assertEquals(Set.of(), product.description().categories());
    assertEquals(List.of(), product.description().links());

    checkAuditLog(
      transaction,
      new ExpectedEvent("USER_CREATED", user.id().toString()),
      new ExpectedEvent("PRODUCT_CREATED", "com.io7m.ex:com.q")
    );
  }

  /**
   * Duplicate products fail.
   *
   * @throws Exception On errors
   */

  @Test
  public void testProductCreationDuplicate()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var transaction =
      this.transactionOf(this.container, EIGION);
    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var id =
      new EIProductIdentifier("com.io7m.ex", "com.q");
    final var user =
      users.userCreate(
        "someone",
        "someone@example.com",
        EIPassword.createHashed("12345678"));

    transaction.userIdSet(user.id());

    final var product =
      products.productCreate(id);

    assertEquals(
      Set.of(id),
      products.productsAll(INCLUDE_REDACTED)
    );
    assertEquals(
      Set.of(id),
      products.productsAll(EXCLUDE_REDACTED)
    );

    final var ex =
      assertThrows(EIServerDatabaseException.class, () -> {
        products.productCreate(id);
      });

    assertEquals("product-duplicate", ex.errorCode());
  }

  /**
   * Product privileges are required.
   *
   * @throws Exception On errors
   */

  @Test
  public void testProductCreationUnprivileged()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var user =
      this.createTestUser();
    final var transaction =
      this.transactionOf(this.container, NONE);
    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);
    final var id =
      new EIProductIdentifier("com.io7m.ex", "com.q");

    transaction.userIdSet(user.id());

    final var ex = assertThrows(EIServerDatabaseException.class, () -> {
      products.productCreate(id);
    });

    assertEquals("sql-error", ex.errorCode());
  }

  private EIUser createTestUser()
    throws
    EIServerDatabaseException,
    NoSuchAlgorithmException,
    InvalidKeySpecException
  {
    final EIUser user;
    {
      final var transaction =
        this.transactionOf(this.container, EIGION);
      final var users =
        transaction.queries(EIServerDatabaseUsersQueriesType.class);
      final var p =
        EIPassword.createHashed("12345678");
      user =
        users.userCreate("someone", "someone@example.com", p);
      transaction.commit();
    }
    return user;
  }

  /**
   * Redacting nonexistent products fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testProductRedactionNonexistent()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var user =
      this.createTestUser();
    final var transaction =
      this.transactionOf(this.container, EIGION);
    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);

    transaction.userIdSet(user.id());

    var ex = assertThrows(EIServerDatabaseException.class, () -> {
      products.productRedact(
        new EIProductIdentifier("com.io7m.ex", "com.q"),
        Optional.empty()
      );
    });

    assertEquals("product-nonexistent", ex.errorCode());

    final var redaction =
      redactionRequest(timeNow(), "X");

    ex = assertThrows(EIServerDatabaseException.class, () -> {
      products.productRedact(
        new EIProductIdentifier("com.io7m.ex", "com.q"),
        Optional.of(redaction)
      );
    });

    assertEquals("product-nonexistent", ex.errorCode());
  }

  /**
   * Redacting products works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testProductRedaction()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var transaction =
      this.transactionOf(this.container, EIGION);
    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var id =
      new EIProductIdentifier("com.io7m.ex", "com.q");
    final var user =
      users.userCreate(
        "someone",
        "someone@example.com",
        EIPassword.createHashed("12345678"));

    transaction.userIdSet(user.id());

    final var product =
      products.productCreate(id);

    assertEquals(
      Set.of(id),
      products.productsAll(INCLUDE_REDACTED)
    );
    assertEquals(
      Set.of(id),
      products.productsAll(EXCLUDE_REDACTED)
    );

    final var redaction =
      EIRedaction.redaction(user.id(), now(), "X");
    final var redactionReq =
      redactionRequestOpt(timeNow(), "X");

    products.productRedact(id, redactionReq);

    assertEquals(
      Set.of(id),
      products.productsAll(INCLUDE_REDACTED)
    );
    assertEquals(
      Set.of(),
      products.productsAll(EXCLUDE_REDACTED)
    );

    products.productRedact(id, Optional.empty());

    assertEquals(
      Set.of(id),
      products.productsAll(INCLUDE_REDACTED)
    );
    assertEquals(
      Set.of(id),
      products.productsAll(EXCLUDE_REDACTED)
    );

    checkAuditLog(
      transaction,
      new ExpectedEvent("USER_CREATED", user.id().toString()),
      new ExpectedEvent("PRODUCT_CREATED", "com.io7m.ex:com.q"),
      new ExpectedEvent("PRODUCT_REDACTED", "com.io7m.ex:com.q: X"),
      new ExpectedEvent("PRODUCT_UNREDACTED", "com.io7m.ex:com.q")
    );
  }

  /**
   * Product privileges are required.
   *
   * @throws Exception On errors
   */

  @Test
  public void testProductRedactionUnprivileged()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var user =
      this.createTestUser();
    final var transaction =
      this.transactionOf(this.container, NONE);
    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);

    transaction.userIdSet(user.id());

    final var id =
      new EIProductIdentifier("com.io7m.ex", "com.q");

    final var redaction =
      redactionRequest(timeNow(), "X");

    final var ex = assertThrows(EIServerDatabaseException.class, () -> {
      products.productRedact(id, Optional.of(redaction));
    });

    assertEquals("sql-error", ex.errorCode());
  }

  /**
   * Product privileges are required.
   *
   * @throws Exception On errors
   */

  @Test
  public void testProductListUnprivileged()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var transaction =
      this.transactionOf(this.container, NONE);
    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);

    final var ex = assertThrows(EIServerDatabaseException.class, () -> {
      products.productsAll(INCLUDE_REDACTED);
    });

    assertEquals("sql-error", ex.errorCode());
  }

  /**
   * Categorizing products works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testProductCategories()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var transaction =
      this.transactionOf(this.container, EIGION);
    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var id =
      new EIProductIdentifier("com.io7m.ex", "com.q");

    final var user =
      users.userCreate(
        "someone",
        "someone@example.com",
        EIPassword.createHashed("12345678"));

    transaction.userIdSet(user.id());

    final var product =
      products.productCreate(id);

    assertEquals(id, product.id());
    assertEquals(List.of(), product.releases());
    assertEquals(Set.of(), product.description().categories());

    final var category0 =
      products.categoryCreate("Cat0");
    final var category1 =
      products.categoryCreate("Cat1");

    products.productCategoryAdd(id, category0);
    products.productCategoryAdd(id, category1);

    {
      final var p = products.product(id, INCLUDE_REDACTED);
      assertEquals(Set.of(category0, category1), p.description().categories());
    }

    {
      final var p = products.product(id, EXCLUDE_REDACTED);
      assertEquals(Set.of(category0, category1), p.description().categories());
    }

    final var redaction =
      EIRedaction.redaction(user.id(), now(), "X");
    final var redactionReq =
      redactionRequestOpt(timeNow(), "X");

    products.categoryRedact(category0, redactionReq);

    {
      final var p = products.product(id, EXCLUDE_REDACTED);
      assertEquals(Set.of(category1), p.description().categories());
    }

    products.productCategoryRemove(id, category0);
    products.productCategoryRemove(id, category1);

    {
      final var p = products.product(id, INCLUDE_REDACTED);
      assertEquals(Set.of(), p.description().categories());
    }

    checkAuditLog(
      transaction,
      new ExpectedEvent("USER_CREATED", user.id().toString()),
      new ExpectedEvent("PRODUCT_CREATED", "com.io7m.ex:com.q"),
      new ExpectedEvent("CATEGORY_CREATED", category0.value()),
      new ExpectedEvent("CATEGORY_CREATED", category1.value()),
      new ExpectedEvent("PRODUCT_CATEGORY_ADDED", "com.io7m.ex:com.q:Cat0"),
      new ExpectedEvent("PRODUCT_CATEGORY_ADDED", "com.io7m.ex:com.q:Cat1"),
      new ExpectedEvent("CATEGORY_REDACTED", null),
      new ExpectedEvent("PRODUCT_CATEGORY_REMOVED", "com.io7m.ex:com.q:Cat0"),
      new ExpectedEvent("PRODUCT_CATEGORY_REMOVED", "com.io7m.ex:com.q:Cat1")
    );
  }

  /**
   * Categorizing products requires privileges.
   *
   * @throws Exception On errors
   */

  @Test
  public void testProductCategoriesUnprivileged()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var user =
      this.createTestUser();
    final var transaction =
      this.transactionOf(this.container, NONE);
    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);

    transaction.userIdSet(user.id());

    final var id =
      new EIProductIdentifier("com.io7m.ex", "com.q");
    final var category0 =
      new EIProductCategory("Cat0", Optional.empty());
    {
      final var ex = assertThrows(EIServerDatabaseException.class, () -> {
        products.productCategoryAdd(id, category0);
      });
      assertEquals("sql-error", ex.errorCode());
    }
    {
      final var ex = assertThrows(EIServerDatabaseException.class, () -> {
        products.productCategoryRemove(id, category0);
      });
      assertEquals("sql-error", ex.errorCode());
    }
  }

  /**
   * Accessing images requires privileges.
   *
   * @throws Exception On errors
   */

  @Test
  public void testImagesUnprivileged()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var user =
      this.createTestUser();
    final var transaction =
      this.transactionOf(this.container, NONE);
    final var images =
      transaction.queries(EIServerDatabaseImagesQueriesType.class);

    transaction.userIdSet(user.id());

    {
      final var ex = assertThrows(EIServerDatabaseException.class, () -> {
        images.imageCreate(
          randomUUID(),
          "text/plain",
          new byte[23]);
      });
      assertEquals("sql-error", ex.errorCode());
    }

    {
      final var ex = assertThrows(EIServerDatabaseException.class, () -> {
        images.imageGet(randomUUID(), INCLUDE_REDACTED);
      });
      assertEquals("sql-error", ex.errorCode());
    }

    {
      final var ex = assertThrows(EIServerDatabaseException.class, () -> {
        images.imageRedact(randomUUID(), Optional.empty());
      });
      assertEquals("sql-error", ex.errorCode());
    }
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
      this.transactionOf(this.container, EIGION);
    final var images =
      transaction.queries(EIServerDatabaseImagesQueriesType.class);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var user =
      users.userCreate(
        "someone",
        "someone@example.com",
        EIPassword.createHashed("12345678"));

    transaction.userIdSet(user.id());

    final var id = randomUUID();
    final var image =
      images.imageCreate(id, "image/jpeg", new byte[23]);

    assertEquals(id, image.id());
    assertEquals("image/jpeg", image.contentType());
    assertEquals(user.id(), image.creation().creator());

    final var image2 =
      images.imageGet(id, INCLUDE_REDACTED)
        .orElseThrow();

    assertEquals(image, image2);

    {
      final var ex = assertThrows(EIServerDatabaseException.class, () -> {
        images.imageCreate(id, "image/jpeg", new byte[23]);
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
      this.transactionOf(this.container, EIGION);
    final var images =
      transaction.queries(EIServerDatabaseImagesQueriesType.class);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var user =
      users.userCreate(
        "someone",
        "someone@example.com",
        EIPassword.createHashed("12345678"));

    transaction.userIdSet(user.id());

    final var imageId =
      randomUUID();
    final var image =
      images.imageCreate(imageId, "image/jpeg", new byte[23]);

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
