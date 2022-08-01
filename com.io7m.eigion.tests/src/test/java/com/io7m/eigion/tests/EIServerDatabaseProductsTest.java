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
import com.io7m.eigion.model.EIUserDisplayName;
import com.io7m.eigion.model.EIUserEmail;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseGroupsQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseProductsQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseUsersQueriesType;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigInteger;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static com.io7m.eigion.model.EIRedactionRequest.redactionRequest;
import static com.io7m.eigion.model.EIRedactionRequest.redactionRequestOpt;
import static com.io7m.eigion.server.database.api.EIServerDatabaseIncludeRedacted.EXCLUDE_REDACTED;
import static com.io7m.eigion.server.database.api.EIServerDatabaseIncludeRedacted.INCLUDE_REDACTED;
import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.EIGION;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.TWO;
import static java.time.OffsetDateTime.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class EIServerDatabaseProductsTest extends EIWithDatabaseContract
{
  private static final EIGroupName EXAMPLE_GROUP =
    new EIGroupName("com.io7m.ex");

  /**
   * Creating and redacting categories works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCategories()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var user =
      users.userCreate(
        new EIUserDisplayName("someone"),
        new EIUserEmail("someone@example.com"),
        databaseGenerateBadPassword()
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
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
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
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);
    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var id =
      new EIProductIdentifier(EXAMPLE_GROUP, "com.q");

    final var user =
      users.userCreate(
        new EIUserDisplayName("someone"),
        new EIUserEmail("someone@example.com"),
        databaseGenerateBadPassword());

    groups.groupCreate(id.group(), user.id());

    transaction.userIdSet(user.id());
    final var product = products.productCreate(id);

    assertEquals(id, product.id());
    assertEquals(List.of(), product.releases());
    assertEquals(Set.of(), product.description().categories());
    assertEquals(List.of(), product.description().links());

    checkAuditLog(
      transaction,
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
      new ExpectedEvent("USER_CREATED", user.id().toString()),
      new ExpectedEvent("GROUP_CREATED", EXAMPLE_GROUP.value()),
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
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);
    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var id =
      new EIProductIdentifier(EXAMPLE_GROUP, "com.q");
    final var user =
      users.userCreate(
        new EIUserDisplayName("someone"),
        new EIUserEmail("someone@example.com"),
        databaseGenerateBadPassword());

    groups.groupCreate(id.group(), user.id());

    transaction.userIdSet(user.id());
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

  private EIUser createTestUser(
    final UUID adminId)
    throws EIServerDatabaseException,
    EIPasswordException
  {
    final EIUser user;
    {
      final var transaction =
        this.transactionOf(EIGION);

      transaction.adminIdSet(adminId);

      final var users =
        transaction.queries(EIServerDatabaseUsersQueriesType.class);
      final var p =
        databaseGenerateBadPassword();
      user = users.userCreate(
        new EIUserDisplayName("someone"),
        new EIUserEmail("someone@example.com"),
        p
      );
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
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var user =
      this.createTestUser(adminId);
    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);

    transaction.userIdSet(user.id());

    var ex = assertThrows(EIServerDatabaseException.class, () -> {
      products.productRedact(
        new EIProductIdentifier(EXAMPLE_GROUP, "com.q"),
        Optional.empty()
      );
    });

    assertEquals("product-nonexistent", ex.errorCode());

    final var redaction =
      redactionRequest(timeNow(), "X");

    ex = assertThrows(EIServerDatabaseException.class, () -> {
      products.productRedact(
        new EIProductIdentifier(EXAMPLE_GROUP, "com.q"),
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
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);
    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var id =
      new EIProductIdentifier(EXAMPLE_GROUP, "com.q");
    final var user =
      users.userCreate(
        new EIUserDisplayName("someone"),
        new EIUserEmail("someone@example.com"),
        databaseGenerateBadPassword()
      );

    groups.groupCreate(id.group(), user.id());

    transaction.userIdSet(user.id());
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
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
      new ExpectedEvent("USER_CREATED", user.id().toString()),
      new ExpectedEvent("GROUP_CREATED", EXAMPLE_GROUP.value()),
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
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);
    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var id =
      new EIProductIdentifier(EXAMPLE_GROUP, "com.q");

    final var user =
      users.userCreate(
        new EIUserDisplayName("someone"),
        new EIUserEmail("someone@example.com"),
        databaseGenerateBadPassword()
      );

    groups.groupCreate(id.group(), user.id());

    transaction.userIdSet(user.id());
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
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
      new ExpectedEvent("USER_CREATED", user.id().toString()),
      new ExpectedEvent("GROUP_CREATED", EXAMPLE_GROUP.value()),
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
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    final var user =
      this.createTestUser(adminId);

    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);
    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var id =
      new EIProductIdentifier(EXAMPLE_GROUP, "com.q");

    transaction.adminIdSet(adminId);
    groups.groupCreate(id.group(), user.id());

    transaction.userIdSet(user.id());
    products.productCreate(id);
    products.productSetTitle(id, "Title");

    {
      final var p = products.product(id, INCLUDE_REDACTED);
      assertEquals("Title", p.description().title());
    }

    checkAuditLog(
      transaction,
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
      new ExpectedEvent("USER_CREATED", user.id().toString()),
      new ExpectedEvent("GROUP_CREATED", EXAMPLE_GROUP.value()),
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
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var user =
      this.createTestUser(adminId);

    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);
    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var id =
      new EIProductIdentifier(EXAMPLE_GROUP, "com.q");

    groups.groupCreate(id.group(), user.id());

    transaction.userIdSet(user.id());
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
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
      new ExpectedEvent("USER_CREATED", user.id().toString()),
      new ExpectedEvent("GROUP_CREATED", EXAMPLE_GROUP.value()),
      new ExpectedEvent("PRODUCT_CREATED", "com.io7m.ex:com.q"),
      new ExpectedEvent("PRODUCT_DESCRIPTION_SET", "com.io7m.ex:com.q")
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
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var user =
      this.createTestUser(adminId);

    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);
    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var id =
      new EIProductIdentifier(EXAMPLE_GROUP, "com.q");
    final var dep0 =
      new EIProductIdentifier(EXAMPLE_GROUP, "com.w");
    final var dep1 =
      new EIProductIdentifier(EXAMPLE_GROUP, "com.x");

    groups.groupCreate(id.group(), user.id());

    transaction.userIdSet(user.id());
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
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
      new ExpectedEvent("USER_CREATED", user.id().toString()),
      new ExpectedEvent("GROUP_CREATED", EXAMPLE_GROUP.value()),
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
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var user =
      this.createTestUser(adminId);

    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);
    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var id =
      new EIProductIdentifier(EXAMPLE_GROUP, "com.q");
    final var dep0 =
      new EIProductIdentifier(EXAMPLE_GROUP, "com.w");
    final var dep1 =
      new EIProductIdentifier(EXAMPLE_GROUP, "com.x");

    groups.groupCreate(id.group(), user.id());

    transaction.userIdSet(user.id());
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
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
      new ExpectedEvent("USER_CREATED", user.id().toString()),
      new ExpectedEvent("GROUP_CREATED", EXAMPLE_GROUP.value()),
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
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var user =
      this.createTestUser(adminId);

    final var products =
      transaction.queries(EIServerDatabaseProductsQueriesType.class);
    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    groups.groupCreate(EXAMPLE_GROUP, user.id());

    transaction.userIdSet(user.id());
    final Function<Integer, EIProductIdentifier> idFor = i -> {
      return new EIProductIdentifier(
        EXAMPLE_GROUP,
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
}
