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

import com.io7m.eigion.model.EICreation;
import com.io7m.eigion.model.EIPassword;
import com.io7m.eigion.model.EIProductCategory;
import com.io7m.eigion.model.EIProductIdentifier;
import com.io7m.eigion.model.EIProductRelease;
import com.io7m.eigion.model.EIProductVersion;
import com.io7m.eigion.model.EIRichText;
import com.io7m.eigion.model.EIUser;
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
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.io7m.eigion.model.EIRedactionRequest.redactionRequest;
import static com.io7m.eigion.model.EIRedactionRequest.redactionRequestOpt;
import static com.io7m.eigion.server.database.api.EIServerDatabaseCreate.CREATE_DATABASE;
import static com.io7m.eigion.server.database.api.EIServerDatabaseIncludeRedacted.INCLUDE_REDACTED;
import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.EIGION;
import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.NONE;
import static com.io7m.eigion.server.database.api.EIServerDatabaseUpgrade.UPGRADE_DATABASE;
import static java.math.BigInteger.ZERO;
import static java.time.OffsetDateTime.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@Testcontainers(disabledWithoutDocker = true)
public final class EIServerDatabaseUnprivilegedTest
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

  private EIUser createTestUser()
    throws EIServerDatabaseException,
    NoSuchAlgorithmException,
    InvalidKeySpecException
  {
    final EIUser user;
    {
      final var transaction =
        this.transactionOf(EIGION);
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
   * Unprivileged contexts cannot execute code.
   *
   * @throws Exception On errors
   */

  @TestFactory
  public Stream<DynamicTest> testUnprivileged()
    throws Exception
  {
    assertTrue(this.container.isRunning());

    final var user = this.createTestUser();

    final var database =
      this.databaseOf(this.container);
    final var c =
      this.resources.add(database.openConnection(NONE));

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
                "text/plain",
                new byte[23]);
            });
          assertEquals("operation-not-permitted", ex.errorCode());
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
          assertEquals("operation-not-permitted", ex.errorCode());
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
          assertEquals("operation-not-permitted", ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var products =
            transaction.queries(EIServerDatabaseProductsQueriesType.class);
          transaction.userIdSet(user.id());

          final var id =
            new EIProductIdentifier("com.io7m.ex", "com.q");
          final var category0 =
            new EIProductCategory("Cat0", Optional.empty());

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              products.productCategoryAdd(id, category0);
            });
          assertEquals("operation-not-permitted", ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var products =
            transaction.queries(EIServerDatabaseProductsQueriesType.class);
          transaction.userIdSet(user.id());

          final var id =
            new EIProductIdentifier("com.io7m.ex", "com.q");
          final var category0 =
            new EIProductCategory("Cat0", Optional.empty());

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              products.productCategoryRemove(id, category0);
            });
          assertEquals("operation-not-permitted", ex.errorCode());
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
          assertEquals("operation-not-permitted", ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var products =
            transaction.queries(EIServerDatabaseProductsQueriesType.class);
          transaction.userIdSet(user.id());

          final var id =
            new EIProductIdentifier("com.io7m.ex", "com.q");
          final var redaction =
            redactionRequest(timeNow(), "X");

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              products.productRedact(id, Optional.of(redaction));
            });
          assertEquals("operation-not-permitted", ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var products =
            transaction.queries(EIServerDatabaseProductsQueriesType.class);
          transaction.userIdSet(user.id());

          final var id =
            new EIProductIdentifier("com.io7m.ex", "com.q");

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              products.productCreate(id);
            });
          assertEquals("operation-not-permitted", ex.errorCode());
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
          assertEquals("operation-not-permitted", ex.errorCode());
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
          assertEquals("operation-not-permitted", ex.errorCode());
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
          assertEquals("operation-not-permitted", ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var users =
            transaction.queries(EIServerDatabaseUsersQueriesType.class);
          transaction.userIdSet(user.id());

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              users.userCreate(
                randomUUID(),
                "someone",
                "someone@example.com",
                timeNow(),
                EIPassword.createHashed("12345678")
              );
            });
          assertEquals("operation-not-permitted", ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var users =
            transaction.queries(EIServerDatabaseUsersQueriesType.class);
          transaction.userIdSet(user.id());

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              users.userGet(randomUUID());
            });
          assertEquals("operation-not-permitted", ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var users =
            transaction.queries(EIServerDatabaseUsersQueriesType.class);
          transaction.userIdSet(user.id());

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              users.userGetForEmail("someone@example.com");
            });
          assertEquals("operation-not-permitted", ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var users =
            transaction.queries(EIServerDatabaseUsersQueriesType.class);
          transaction.userIdSet(user.id());

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              users.userGetForName("someone");
            });
          assertEquals("operation-not-permitted", ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var users =
            transaction.queries(EIServerDatabaseUsersQueriesType.class);
          transaction.userIdSet(user.id());

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              users.userBan(
                randomUUID(),
                Optional.of(now()),
                "reason");
            });
          assertEquals("operation-not-permitted", ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var users =
            transaction.queries(EIServerDatabaseUsersQueriesType.class);
          transaction.userIdSet(user.id());

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              users.userUnban(randomUUID());
            });
          assertEquals("operation-not-permitted", ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var products =
            transaction.queries(EIServerDatabaseProductsQueriesType.class);
          transaction.userIdSet(user.id());

          final var id =
            new EIProductIdentifier("com.io7m.ex", "com.q");
          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              products.productSetTitle(id, "title");
            });
          assertEquals("operation-not-permitted", ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var products =
            transaction.queries(EIServerDatabaseProductsQueriesType.class);
          transaction.userIdSet(user.id());

          final var id =
            new EIProductIdentifier("com.io7m.ex", "com.q");
          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              products.productSetDescription(id, new EIRichText("x", "y"));
            });
          assertEquals("operation-not-permitted", ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var products =
            transaction.queries(EIServerDatabaseProductsQueriesType.class);
          transaction.userIdSet(user.id());

          final var id =
            new EIProductIdentifier("com.io7m.ex", "com.q");
          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              products.product(id, INCLUDE_REDACTED);
            });
          assertEquals("operation-not-permitted", ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var images =
            transaction.queries(EIServerDatabaseImagesQueriesType.class);
          transaction.userIdSet(user.id());

          final var ex =
            assertThrows(EIServerDatabaseException.class, () -> {
              images.imageCreate(randomUUID(), "image/jpeg", new byte[32]);
            });
          assertEquals("operation-not-permitted", ex.errorCode());
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
          assertEquals("operation-not-permitted", ex.errorCode());
        }
      },

      () -> {
        try (var transaction = c.openTransaction()) {
          final var products =
            transaction.queries(EIServerDatabaseProductsQueriesType.class);
          transaction.userIdSet(user.id());

          final var id =
            new EIProductIdentifier("com.io7m.ex", "com.q");

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
          assertEquals("operation-not-permitted", ex.errorCode());
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
