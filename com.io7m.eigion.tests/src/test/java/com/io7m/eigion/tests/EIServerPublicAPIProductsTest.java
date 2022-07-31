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

import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIProductIdentifier;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandLogin;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseProductList;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseGroupsQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseProductsQueriesType;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.EIGION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class EIServerPublicAPIProductsTest extends EIServerContract
{
  /**
   * Listing products in an empty database works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testNoProducts()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var adminId =
      this.createAdminInitial("someone", "12345678");

    this.createUserSomeone(adminId);

    {
      final var r =
        this.postPublicBytes(
          "/public/1/0/login",
          this.messagesPublicV1().serialize(
            new EISP1CommandLogin("someone", "12345678"))
        );
      assertEquals(200, r.statusCode());
    }

    {
      final var rk =
        this.getPublic("/public/1/0/products/get");
      assertEquals(200, rk.statusCode());
      final var pk =
        this.parsePublic(rk, EISP1ResponseProductList.class);
    }
  }

  /**
   * Trying to list products starting at invalid identifiers fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testProductInvalidStart()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var adminId =
      this.createAdminInitial("someone", "12345678");

    this.createUserSomeone(adminId);

    {
      final var r =
        this.postPublicBytes(
          "/public/1/0/login",
          this.messagesPublicV1().serialize(
            new EISP1CommandLogin("someone", "12345678"))
        );
      assertEquals(200, r.statusCode());
    }

    {
      final var rk =
        this.getPublic("/public/1/0/products/get?start=x");
      assertEquals(400, rk.statusCode());
    }
  }

  /**
   * Creating and listing a product works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testProductListCreated()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var adminId =
      this.createAdminInitial("someone", "12345678");

    final var user =
      this.createUserSomeone(adminId);

    final var product =
      this.createProduct(adminId, user, 0);

    {
      final var r =
        this.postPublicBytes(
          "/public/1/0/login",
          this.messagesPublicV1().serialize(
            new EISP1CommandLogin("someone", "12345678"))
        );
      assertEquals(200, r.statusCode());
    }

    {
      final var rk =
        this.getPublic("/public/1/0/products/get");
      assertEquals(200, rk.statusCode());
      final var pk =
        this.parsePublic(rk, EISP1ResponseProductList.class);

      assertEquals(1, pk.products().size());
      assertEquals(product.group().value(), pk.products().get(0).group());
      assertEquals(product.name(), pk.products().get(0).name());
      assertEquals("Title 0000", pk.products().get(0).title());
    }
  }

  private EIProductIdentifier createProduct(
    final UUID adminId,
    final UUID user,
    final int number)
    throws EIServerDatabaseException
  {
    final var database = this.databases().mostRecent();
    try (var connection = database.openConnection(EIGION)) {
      try (var transaction = connection.openTransaction()) {
        final var groups =
          transaction.queries(EIServerDatabaseGroupsQueriesType.class);
        final var products =
          transaction.queries(EIServerDatabaseProductsQueriesType.class);

        final var identifier =
          createProductIdentifier(number);

        final var groupName = identifier.group();
        if (!groups.groupExists(groupName)) {
          transaction.adminIdSet(adminId);
          groups.groupCreate(groupName, user);
        }

        transaction.userIdSet(user);
        products.productCreate(identifier);
        products.productSetTitle(
          identifier,
          String.format("Title %04d", Integer.valueOf(number))
        );

        transaction.commit();
        return identifier;
      }
    }
  }

  private static EIProductIdentifier createProductIdentifier(
    final int number)
  {
    return new EIProductIdentifier(
      new EIGroupName("com.eigion.tests"),
      String.format("product%04d", Integer.valueOf(number))
    );
  }
}
