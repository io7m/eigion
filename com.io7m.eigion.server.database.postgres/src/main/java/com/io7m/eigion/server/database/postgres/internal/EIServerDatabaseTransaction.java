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

import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseTransactionType;
import com.io7m.eigion.server.database.api.EIServerDatabaseUsersQueriesType;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.sql.SQLException;
import java.time.Clock;
import java.util.Objects;

import static org.jooq.SQLDialect.POSTGRES;

record EIServerDatabaseTransaction(
  EIServerDatabaseConnection connection)
  implements EIServerDatabaseTransactionType
{
  @Override
  public <T extends EIServerDatabaseQueriesType> T queries(
    final Class<T> queryClass)
    throws EIServerDatabaseException
  {
    if (Objects.equals(queryClass, EIServerDatabaseUsersQueriesType.class)) {
      return queryClass.cast(new EIServerDatabaseUsersQueries(this));
    }

    throw new EIServerDatabaseException(
      "Unsupported query type: %s".formatted(queryClass),
      "unsupported-query-class"
    );
  }

  public DSLContext createContext()
  {
    final var trConnection =
      this.connection();
    final var sqlConnection =
      trConnection.connection();
    final var settings =
      trConnection.settings();
    return DSL.using(sqlConnection, POSTGRES, settings);
  }

  public Clock clock()
  {
    return this.connection.clock();
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
    this.rollback();
  }
}
