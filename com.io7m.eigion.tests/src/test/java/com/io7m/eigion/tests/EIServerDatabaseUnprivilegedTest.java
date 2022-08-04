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
import com.io7m.eigion.model.EICreation;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.eigion.model.EIProductCategory;
import com.io7m.eigion.model.EIProductIdentifier;
import com.io7m.eigion.model.EIProductRelease;
import com.io7m.eigion.model.EIProductVersion;
import com.io7m.eigion.model.EIRichText;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.model.EIUserDisplayName;
import com.io7m.eigion.model.EIUserEmail;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseImagesQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseProductsQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseUsersQueriesType;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.OPERATION_NOT_PERMITTED;
import static com.io7m.eigion.model.EIRedactionRequest.redactionRequest;
import static com.io7m.eigion.model.EIRedactionRequest.redactionRequestOpt;
import static com.io7m.eigion.server.database.api.EIServerDatabaseIncludeRedacted.INCLUDE_REDACTED;
import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.EIGION;
import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.NONE;
import static java.math.BigInteger.ZERO;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.OffsetDateTime.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@Testcontainers(disabledWithoutDocker = true)
public final class EIServerDatabaseUnprivilegedTest extends
  EIWithDatabaseContract
{
  private static final EIGroupName EXAMPLE_GROUP =
    new EIGroupName("com.io7m.ex");
  private static final String EXAMPLE_ARTIFACT =
    "com.q";

  private EIUser createTestUser(
    final UUID adminId)
    throws Exception
  {
    final EIUser user;
    {
      final var transaction =
        this.transactionOf(EIGION);

      transaction.adminIdSet(adminId);

      final var users =
        transaction.queries(EIServerDatabaseUsersQueriesType.class);
      final var p =
        EIPasswordAlgorithmPBKDF2HmacSHA256.create()
          .createHashed("12345678");

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
   * Unprivileged contexts cannot execute code.
   *
   * @throws Exception On errors
   */

  @TestFactory
  public Stream<DynamicTest> testUnprivileged()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var user =
      this.createTestUser(adminId);

    final var c =
      this.connectionOf(NONE);

    return Stream.<ExecutableType>of(
      () -> {
        try (var transaction = c.openTransaction()) {
          final var images =
            transaction.queries(EIServerDatabaseImagesQueriesType.class);
          transaction.userIdSet(user.id());

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              images.imageCreate(
                randomUUID(),
                EIHash.sha256Of("hello".getBytes(UTF_8))
              );
            });
          assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var images =
            transaction.queries(EIServerDatabaseImagesQueriesType.class);
          transaction.userIdSet(user.id());

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              images.imageGet(randomUUID(), INCLUDE_REDACTED);
            });
          assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var images =
            transaction.queries(EIServerDatabaseImagesQueriesType.class);
          transaction.userIdSet(user.id());

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              images.imageRedact(randomUUID(), Optional.empty());
            });
          assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var products =
            transaction.queries(EIServerDatabaseProductsQueriesType.class);
          transaction.userIdSet(user.id());

          final var id =
            new EIProductIdentifier(EXAMPLE_GROUP, EXAMPLE_ARTIFACT);
          final var category0 =
            new EIProductCategory("Cat0", Optional.empty());

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              products.productCategoryAdd(id, category0);
            });
          assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var products =
            transaction.queries(EIServerDatabaseProductsQueriesType.class);
          transaction.userIdSet(user.id());

          final var id =
            new EIProductIdentifier(EXAMPLE_GROUP, EXAMPLE_ARTIFACT);
          final var category0 =
            new EIProductCategory("Cat0", Optional.empty());

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              products.productCategoryRemove(id, category0);
            });
          assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var products =
            transaction.queries(EIServerDatabaseProductsQueriesType.class);
          transaction.userIdSet(user.id());

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              products.productsAll(INCLUDE_REDACTED);
            });
          assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var products =
            transaction.queries(EIServerDatabaseProductsQueriesType.class);
          transaction.userIdSet(user.id());

          final var id =
            new EIProductIdentifier(EXAMPLE_GROUP, EXAMPLE_ARTIFACT);
          final var redaction =
            redactionRequest(timeNow(), "X");

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              products.productRedact(id, Optional.of(redaction));
            });
          assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var products =
            transaction.queries(EIServerDatabaseProductsQueriesType.class);
          transaction.userIdSet(user.id());

          final var id =
            new EIProductIdentifier(EXAMPLE_GROUP, EXAMPLE_ARTIFACT);

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              products.productCreate(id);
            });
          assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var products =
            transaction.queries(EIServerDatabaseProductsQueriesType.class);
          transaction.userIdSet(user.id());

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              products.categoryCreate("Category 0");
            });
          assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var products =
            transaction.queries(EIServerDatabaseProductsQueriesType.class);
          transaction.userIdSet(user.id());

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              products.categoryRedact(
                "Category 0",
                redactionRequestOpt(now(), "X"));
            });
          assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var products =
            transaction.queries(EIServerDatabaseProductsQueriesType.class);
          transaction.userIdSet(user.id());

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              products.categories(INCLUDE_REDACTED);
            });
          assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var users =
            transaction.queries(EIServerDatabaseUsersQueriesType.class);
          transaction.adminIdSet(adminId);

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              users.userCreate(
                randomUUID(),
                new EIUserDisplayName("someone"),
                new EIUserEmail("someone@example.com"),
                timeNow(),
                EIPasswordAlgorithmPBKDF2HmacSHA256.create()
                  .createHashed("12345678")
              );
            });
          assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var users =
            transaction.queries(EIServerDatabaseUsersQueriesType.class);

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              users.userGet(randomUUID());
            });
          assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var users =
            transaction.queries(EIServerDatabaseUsersQueriesType.class);
          transaction.userIdSet(user.id());

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              users.userGetForEmail(new EIUserEmail("someone@example.com"));
            });
          assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var users =
            transaction.queries(EIServerDatabaseUsersQueriesType.class);
          transaction.userIdSet(user.id());

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              users.userGetForName(new EIUserDisplayName("someone"));
            });
          assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var users =
            transaction.queries(EIServerDatabaseUsersQueriesType.class);
          transaction.adminIdSet(adminId);

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              users.userBan(
                randomUUID(),
                Optional.of(now()),
                "reason");
            });
          assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var users =
            transaction.queries(EIServerDatabaseUsersQueriesType.class);
          transaction.adminIdSet(adminId);

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              users.userUnban(randomUUID());
            });
          assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var products =
            transaction.queries(EIServerDatabaseProductsQueriesType.class);
          transaction.userIdSet(user.id());

          final var id =
            new EIProductIdentifier(EXAMPLE_GROUP, EXAMPLE_ARTIFACT);
          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              products.productSetTitle(id, "title");
            });
          assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var products =
            transaction.queries(EIServerDatabaseProductsQueriesType.class);
          transaction.userIdSet(user.id());

          final var id =
            new EIProductIdentifier(EXAMPLE_GROUP, EXAMPLE_ARTIFACT);
          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              products.productSetDescription(id, new EIRichText("x", "y"));
            });
          assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var products =
            transaction.queries(EIServerDatabaseProductsQueriesType.class);
          transaction.userIdSet(user.id());

          final var id =
            new EIProductIdentifier(EXAMPLE_GROUP, EXAMPLE_ARTIFACT);
          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              products.product(id, INCLUDE_REDACTED);
            });
          assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var images =
            transaction.queries(EIServerDatabaseImagesQueriesType.class);
          transaction.userIdSet(user.id());

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              images.imageCreate(
                randomUUID(),
                EIHash.sha256Of("hello".getBytes(UTF_8)));
            });
          assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var images =
            transaction.queries(EIServerDatabaseImagesQueriesType.class);
          transaction.userIdSet(user.id());

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              images.imageRedact(randomUUID(), Optional.empty());
            });
          assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var products =
            transaction.queries(EIServerDatabaseProductsQueriesType.class);
          transaction.userIdSet(user.id());

          final var id =
            new EIProductIdentifier(EXAMPLE_GROUP, EXAMPLE_ARTIFACT);

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              products.productReleaseCreate(
                id,
                new EIProductRelease(
                  new EIProductVersion(ZERO, ZERO, ZERO, Optional.empty()),
                  List.of(),
                  List.of(),
                  List.of(),
                  Optional.empty(),
                  EICreation.zero()
                )
              );
            });
          assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
        }
      }

    ).map(x -> dynamicTest("testUnprivileged_" + x, x::execute));
  }

  interface ExecutableType
  {
    void execute()
      throws Exception;
  }
}
