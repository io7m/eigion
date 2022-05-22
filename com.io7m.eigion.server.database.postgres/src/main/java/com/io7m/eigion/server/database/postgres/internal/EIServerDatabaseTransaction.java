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

package com.io7m.eigion.server.database.postgres.internal;

import com.io7m.eigion.product.parser.api.EIProductsSerializersType;
import com.io7m.eigion.server.database.api.EIServerDatabaseAuditQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseImagesQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseProductsQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseTransactionType;
import com.io7m.eigion.server.database.api.EIServerDatabaseUsersQueriesType;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.sql.SQLException;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.eigion.server.database.postgres.internal.Tables.USERS;
import static org.jooq.SQLDialect.POSTGRES;

final class EIServerDatabaseTransaction
  implements EIServerDatabaseTransactionType
{
  private final EIServerDatabaseConnection connection;
  private UUID currentUserId;

  EIServerDatabaseTransaction(
    final EIServerDatabaseConnection inConnection)
  {
    this.connection =
      Objects.requireNonNull(inConnection, "connection");
  }

  @Override
  public void userIdSet(
    final UUID userId)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(userId, "userId");

    final var context = this.createContext();

    try {
      final var userOpt =
        context.select(USERS.ID)
          .from(USERS)
          .where(USERS.ID.eq(userId))
          .fetchOptional()
          .map(r -> r.getValue(USERS.ID));

      if (userOpt.isEmpty()) {
        throw new EIServerDatabaseException(
          "No such user: " + userId,
          "user-nonexistent"
        );
      }

      this.currentUserId = userId;
    } catch (final DataAccessException e) {
      throw new EIServerDatabaseException(e.getMessage(), e, "sql-error");
    }
  }

  @Override
  public UUID userId()
    throws EIServerDatabaseException
  {
    return Optional.ofNullable(this.currentUserId).orElseThrow(() -> {
      return new EIServerDatabaseException(
        "A user must be set before calling this method.",
        "user-unset"
      );
    });
  }

  @Override
  public <T extends EIServerDatabaseQueriesType> T queries(
    final Class<T> qClass)
    throws EIServerDatabaseException
  {
    if (Objects.equals(qClass, EIServerDatabaseUsersQueriesType.class)) {
      return qClass.cast(new EIServerDatabaseUsersQueries(this));
    }

    if (Objects.equals(qClass, EIServerDatabaseProductsQueriesType.class)) {
      return qClass.cast(new EIServerDatabaseProductsQueries(
        this,
        new EIServerDatabaseUsersQueries(this))
      );
    }

    if (Objects.equals(qClass, EIServerDatabaseAuditQueriesType.class)) {
      return qClass.cast(new EIServerDatabaseAuditQueries(this));
    }

    if (Objects.equals(qClass, EIServerDatabaseImagesQueriesType.class)) {
      return qClass.cast(new EIServerDatabaseImagesQueries(
        this,
        new EIServerDatabaseUsersQueries(this))
      );
    }

    throw new EIServerDatabaseException(
      "Unsupported query type: %s".formatted(qClass),
      "unsupported-query-class"
    );
  }

  public DSLContext createContext()
  {
    final var sqlConnection =
      this.connection.connection();
    final var settings =
      this.connection.database().settings();
    return DSL.using(sqlConnection, POSTGRES, settings);
  }

  public Clock clock()
  {
    return this.connection.database().clock();
  }

  @Override
  public void rollback()
    throws EIServerDatabaseException
  {
    try {
      this.connection.connection().rollback();
    } catch (final SQLException e) {
      throw new EIServerDatabaseException(e.getMessage(), e, "sql-error");
    }
  }

  @Override
  public void commit()
    throws EIServerDatabaseException
  {
    try {
      this.connection.connection().commit();
    } catch (final SQLException e) {
      throw new EIServerDatabaseException(e.getMessage(), e, "sql-error");
    }
  }

  @Override
  public void close()
    throws EIServerDatabaseException
  {
    this.currentUserId = null;
    this.rollback();
  }

  public EIProductsSerializersType productsSerializers()
  {
    return this.connection.database().productsSerializers();
  }
}
