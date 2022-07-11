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
import com.io7m.eigion.model.EIChange;
import com.io7m.eigion.model.EIChangeTicket;
import com.io7m.eigion.model.EICreation;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIPassword;
import com.io7m.eigion.model.EIPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.eigion.model.EIPasswordException;
import com.io7m.eigion.model.EIProductBundleDependency;
import com.io7m.eigion.model.EIProductCategory;
import com.io7m.eigion.model.EIProductDependency;
import com.io7m.eigion.model.EIProductIdentifier;
import com.io7m.eigion.model.EIProductRelease;
import com.io7m.eigion.model.EIProductSummary;
import com.io7m.eigion.model.EIProductVersion;
import com.io7m.eigion.model.EIRedaction;
import com.io7m.eigion.model.EIRedactionRequest;
import com.io7m.eigion.model.EIRichText;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.server.database.api.EIServerDatabaseAuditQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseConfiguration;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseGroupsQueriesType;
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

import java.math.BigInteger;
import java.net.URI;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static com.io7m.eigion.model.EIRedaction.redactionOpt;
import static com.io7m.eigion.model.EIRedactionRequest.redactionRequest;
import static com.io7m.eigion.model.EIRedactionRequest.redactionRequestOpt;
import static com.io7m.eigion.server.database.api.EIServerDatabaseCreate.CREATE_DATABASE;
import static com.io7m.eigion.server.database.api.EIServerDatabaseIncludeRedacted.EXCLUDE_REDACTED;
import static com.io7m.eigion.server.database.api.EIServerDatabaseIncludeRedacted.INCLUDE_REDACTED;
import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.EIGION;
import static com.io7m.eigion.server.database.api.EIServerDatabaseUpgrade.UPGRADE_DATABASE;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.TWO;
import static java.nio.charset.StandardCharsets.UTF_8;
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
      this.transactionOf(EIGION);
    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var user =
      users.userCreate(
        "someone",
        "someone@example.com",
        createBadPassword()
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
      this.transactionOf(EIGION);
    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);
    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var id =
      new EIProductIdentifier("com.io7m.ex", "com.q");

    final var user =
      users.userCreate(
        "someone",
        "someone@example.com",
        createBadPassword());

    transaction.userIdSet(user.id());

    groups.groupCreate(id.groupName());
    final var product = products.productCreate(id);

    assertEquals(id, product.id());
    assertEquals(List.of(), product.releases());
    assertEquals(Set.of(), product.description().categories());
    assertEquals(List.of(), product.description().links());

    checkAuditLog(
      transaction,
      new ExpectedEvent("USER_CREATED", user.id().toString()),
      new ExpectedEvent("GROUP_CREATED", "com.io7m.ex"),
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
      this.transactionOf(EIGION);
    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);
    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var id =
      new EIProductIdentifier("com.io7m.ex", "com.q");
    final var user =
      users.userCreate(
        "someone",
        "someone@example.com",
        createBadPassword());

    transaction.userIdSet(user.id());

    groups.groupCreate(id.groupName());
    final var product = products.productCreate(id);

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

  private EIUser createTestUser()
    throws EIServerDatabaseException,
    EIPasswordException
  {
    final EIUser user;
    {
      final var transaction =
        this.transactionOf(EIGION);
      final var users =
        transaction.queries(EIServerDatabaseUsersQueriesType.class);
      final var p =
        createBadPassword();
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
      this.transactionOf(EIGION);
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
      this.transactionOf(EIGION);
    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);
    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var id =
      new EIProductIdentifier("com.io7m.ex", "com.q");
    final var user =
      users.userCreate(
        "someone",
        "someone@example.com",
        createBadPassword());

    transaction.userIdSet(user.id());

    groups.groupCreate(id.groupName());
    final var product = products.productCreate(id);

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
      new ExpectedEvent("GROUP_CREATED", "com.io7m.ex"),
      new ExpectedEvent("PRODUCT_CREATED", "com.io7m.ex:com.q"),
      new ExpectedEvent("PRODUCT_REDACTED", "com.io7m.ex:com.q: X"),
      new ExpectedEvent("PRODUCT_UNREDACTED", "com.io7m.ex:com.q")
    );
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
      this.transactionOf(EIGION);
    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);
    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var id =
      new EIProductIdentifier("com.io7m.ex", "com.q");

    final var user =
      users.userCreate(
        "someone",
        "someone@example.com",
        createBadPassword());

    transaction.userIdSet(user.id());

    groups.groupCreate(id.groupName());

    final var product = products.productCreate(id);
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
      new ExpectedEvent("GROUP_CREATED", "com.io7m.ex"),
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
   * Setting product titles works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testProductTitleSet()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var user =
      this.createTestUser();

    final var transaction =
      this.transactionOf(EIGION);
    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);
    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    transaction.userIdSet(user.id());

    final var id =
      new EIProductIdentifier("com.io7m.ex", "com.q");

    groups.groupCreate(id.groupName());
    products.productCreate(id);
    products.productSetTitle(id, "Title");

    {
      final var p = products.product(id, INCLUDE_REDACTED);
      assertEquals("Title", p.description().title());
    }

    checkAuditLog(
      transaction,
      new ExpectedEvent("USER_CREATED", user.id().toString()),
      new ExpectedEvent("GROUP_CREATED", "com.io7m.ex"),
      new ExpectedEvent("PRODUCT_CREATED", "com.io7m.ex:com.q"),
      new ExpectedEvent("PRODUCT_TITLE_SET", "com.io7m.ex:com.q:Title")
    );
  }

  /**
   * Setting product descriptions works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testProductDescriptionSet()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var user =
      this.createTestUser();

    final var transaction =
      this.transactionOf(EIGION);
    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);
    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    transaction.userIdSet(user.id());

    final var id =
      new EIProductIdentifier("com.io7m.ex", "com.q");

    groups.groupCreate(id.groupName());
    products.productCreate(id);
    products.productSetDescription(
      id,
      new EIRichText("text/plain", "Description"));

    {
      final var p = products.product(id, INCLUDE_REDACTED);
      final var rich = p.description().description();
      assertEquals("Description", rich.text());
      assertEquals("text/plain", rich.contentType());
    }

    checkAuditLog(
      transaction,
      new ExpectedEvent("USER_CREATED", user.id().toString()),
      new ExpectedEvent("GROUP_CREATED", "com.io7m.ex"),
      new ExpectedEvent("PRODUCT_CREATED", "com.io7m.ex:com.q"),
      new ExpectedEvent("PRODUCT_DESCRIPTION_SET", "com.io7m.ex:com.q")
    );
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

  /**
   * Creating product releases works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testProductReleaseCreate()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var user =
      this.createTestUser();

    final var transaction =
      this.transactionOf(EIGION);
    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);
    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    transaction.userIdSet(user.id());

    final var id =
      new EIProductIdentifier("com.io7m.ex", "com.q");
    final var dep0 =
      new EIProductIdentifier("com.io7m.ex", "com.w");
    final var dep1 =
      new EIProductIdentifier("com.io7m.ex", "com.x");

    groups.groupCreate(id.groupName());
    products.productCreate(id);

    final var v0 =
      new EIProductVersion(ONE, TWO, TEN, Optional.empty());
    final var v1 =
      new EIProductVersion(ONE, ONE, TWO, Optional.empty());
    final var hash0 =
      new EIHash(
        "SHA-256",
        "5891B5B522D5DF086D0FF0B110FBD9D21BB4FC7163AF34D08286A2E846F6BE03");

    final var change0 =
      new EIChange(
        "Changed something",
        List.of(
          new EIChangeTicket(
            "1",
            URI.create("https://www.example.com/tickets/1")
          )
        )
      );

    final var release =
      new EIProductRelease(
        v0,
        List.of(new EIProductDependency(dep0, v1)),
        List.of(new EIProductBundleDependency(dep1, v1, hash0, List.of())),
        List.of(change0),
        Optional.empty(),
        EICreation.zero()
      );

    products.productReleaseCreate(id, release);

    {
      final var p = products.product(id, INCLUDE_REDACTED);
      final var r = p.releases().get(0);
      assertEquals(release.version(), r.version());
      assertEquals(release.changes(), r.changes());
      assertEquals(release.productDependencies(), r.productDependencies());
      assertEquals(release.bundleDependencies(), r.bundleDependencies());
      assertEquals(release.redaction(), r.redaction());
    }

    {
      final var ex =
        assertThrows(EIServerDatabaseException.class, () -> {
          products.productReleaseCreate(id, release);
        });
      assertEquals("release-duplicate", ex.errorCode());
    }

    checkAuditLog(
      transaction,
      new ExpectedEvent("USER_CREATED", user.id().toString()),
      new ExpectedEvent("GROUP_CREATED", "com.io7m.ex"),
      new ExpectedEvent("PRODUCT_CREATED", "com.io7m.ex:com.q"),
      new ExpectedEvent("PRODUCT_RELEASE_CREATED", "com.io7m.ex:com.q:1.2.10")
    );
  }

  /**
   * Redacting product releases works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testProductReleaseRedaction()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var user =
      this.createTestUser();

    final var transaction =
      this.transactionOf(EIGION);
    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);
    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    transaction.userIdSet(user.id());

    final var id =
      new EIProductIdentifier("com.io7m.ex", "com.q");
    final var dep0 =
      new EIProductIdentifier("com.io7m.ex", "com.w");
    final var dep1 =
      new EIProductIdentifier("com.io7m.ex", "com.x");

    groups.groupCreate(id.groupName());
    products.productCreate(id);

    final var v0 =
      new EIProductVersion(ONE, TWO, TEN, Optional.empty());
    final var v1 =
      new EIProductVersion(ONE, ONE, TWO, Optional.empty());
    final var hash0 =
      new EIHash(
        "SHA-256",
        "5891B5B522D5DF086D0FF0B110FBD9D21BB4FC7163AF34D08286A2E846F6BE03");

    final var change0 =
      new EIChange(
        "Changed something",
        List.of(
          new EIChangeTicket(
            "1",
            URI.create("https://www.example.com/tickets/1")
          )
        )
      );

    final var release =
      new EIProductRelease(
        v0,
        List.of(new EIProductDependency(dep0, v1)),
        List.of(new EIProductBundleDependency(dep1, v1, hash0, List.of())),
        List.of(change0),
        Optional.empty(),
        EICreation.zero()
      );

    products.productReleaseCreate(id, release);
    transaction.commit();

    products.productReleaseRedact(
      id, v0, Optional.of(new EIRedactionRequest(timeNow(), "X"))
    );
    transaction.commit();

    {
      final var p = products.product(id, EXCLUDE_REDACTED);
      assertEquals(List.of(), p.releases());
    }

    products.productReleaseRedact(
      id, v0, Optional.empty()
    );
    transaction.commit();

    {
      final var p = products.product(id, EXCLUDE_REDACTED);
      assertEquals(1, p.releases().size());
    }

    checkAuditLog(
      transaction,
      new ExpectedEvent("USER_CREATED", user.id().toString()),
      new ExpectedEvent("GROUP_CREATED", "com.io7m.ex"),
      new ExpectedEvent("PRODUCT_CREATED", "com.io7m.ex:com.q"),
      new ExpectedEvent("PRODUCT_RELEASE_CREATED", "com.io7m.ex:com.q:1.2.10")
    );
  }

  /**
   * Product summary retrievals work.
   *
   * @throws Exception On errors
   */

  @Test
  public void testProductSummaries()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var user =
      this.createTestUser();

    final var transaction =
      this.transactionOf(EIGION);
    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);
    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    transaction.userIdSet(user.id());

    groups.groupCreate(new EIGroupName("com.io7m.ex"));

    final Function<Integer, EIProductIdentifier> idFor = i -> {
      return new EIProductIdentifier(
        "com.io7m.ex",
        String.format("com.q%04d", i)
      );
    };

    for (int index = 0; index < 100; ++index) {
      final var id = idFor.apply(Integer.valueOf(index));
      products.productCreate(id);
      products.productSetTitle(id, "Title " + index);

      if (index == 30 || index == 46) {
        products.productRedact(
          id, Optional.of(new EIRedactionRequest(timeNow(), "Broken!"))
        );
      }
    }

    transaction.commit();

    EIProductIdentifier next;
    final var limit = BigInteger.valueOf(20L);

    {
      final var page =
        products.productSummaries(Optional.empty(), limit);
      assertEquals(20, page.items().size());
      assertEquals(
        page.items().stream().map(EIProductSummary::id).toList(),
        page.items().stream().map(EIProductSummary::id).sorted().toList()
      );
      next = page.lastKey().orElseThrow();
    }

    {
      final var page =
        products.productSummaries(Optional.of(next), limit);
      assertEquals(20, page.items().size());
      assertEquals(
        page.items().stream().map(EIProductSummary::id).toList(),
        page.items().stream().map(EIProductSummary::id).sorted().toList()
      );
      next = page.lastKey().orElseThrow();
    }

    {
      final var page =
        products.productSummaries(Optional.of(next), limit);
      assertEquals(20, page.items().size());
      assertEquals(
        page.items().stream().map(EIProductSummary::id).toList(),
        page.items().stream().map(EIProductSummary::id).sorted().toList()
      );
      next = page.lastKey().orElseThrow();
    }

    {
      final var page =
        products.productSummaries(Optional.of(next), limit);
      assertEquals(20, page.items().size());
      assertEquals(
        page.items().stream().map(EIProductSummary::id).toList(),
        page.items().stream().map(EIProductSummary::id).sorted().toList()
      );
      next = page.lastKey().orElseThrow();
    }

    {
      final var page =
        products.productSummaries(Optional.of(next), limit);
      assertEquals(18, page.items().size());
      assertEquals(
        page.items().stream().map(EIProductSummary::id).toList(),
        page.items().stream().map(EIProductSummary::id).sorted().toList()
      );
      next = page.lastKey().orElseThrow();
    }

    {
      final var page =
        products.productSummaries(Optional.of(next), limit);
      assertEquals(0, page.items().size());
      assertEquals(
        page.items().stream().map(EIProductSummary::id).toList(),
        page.items().stream().map(EIProductSummary::id).sorted().toList()
      );
      assertEquals(Optional.empty(), page.lastKey());
    }
  }

  private record ExpectedEvent(
    String type,
    String message)
  {

  }
}
