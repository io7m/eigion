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

import com.io7m.eigion.server.database.api.EISDatabaseAuditQueriesType;
import com.io7m.eigion.server.database.api.EISDatabaseException;
import com.io7m.eigion.server.database.api.EISDatabaseQueriesType;
import com.io7m.eigion.server.database.api.EISDatabaseRole;
import com.io7m.eigion.server.database.api.EISDatabaseTransactionType;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.sql.SQLException;
import java.time.Clock;
import java.util.Objects;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.SQL_ERROR;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.SQL_ERROR_UNSUPPORTED_QUERY_CLASS;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.DB_SYSTEM;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.DbSystemValues.POSTGRESQL;
import static org.jooq.SQLDialect.POSTGRES;

final class EISDatabaseTransaction
  implements EISDatabaseTransactionType
{
  private final EISDatabaseConnection connection;
  private final Span transactionSpan;

  /**
   * @return The transaction span for metrics
   */

  public Span span()
  {
    return this.transactionSpan;
  }

  /**
   * Create a new query span for measuring query times.
   *
   * @param name The query name
   *
   * @return The query span
   */

  public Span createQuerySpan(
    final String name)
  {
    return this.tracer()
      .spanBuilder(name)
      .setParent(Context.current().with(this.transactionSpan))
      .setAttribute(DB_SYSTEM, POSTGRESQL)
      .setSpanKind(INTERNAL)
      .startSpan();
  }

  EISDatabaseTransaction(
    final EISDatabaseConnection inConnection,
    final Span inTransactionScope)
  {
    this.connection =
      Objects.requireNonNull(inConnection, "connection");
    this.transactionSpan =
      Objects.requireNonNull(inTransactionScope, "inMetricsScope");
  }

  void setRole(
    final EISDatabaseRole role)
    throws SQLException
  {
    switch (role) {
      case EIGION -> {
        try (var st =
               this.connection.connection()
                 .prepareStatement("set role eigion")) {
          st.execute();
        }
      }
      case NONE -> {
        try (var st =
               this.connection.connection()
                 .prepareStatement("set role eigion_none")) {
          st.execute();
        }
      }
    }
  }

  @Override
  public <T extends EISDatabaseQueriesType> T queries(
    final Class<T> qClass)
    throws EISDatabaseException
  {
    if (Objects.equals(qClass, EISDatabaseAuditQueriesType.class)) {
      return qClass.cast(new EISDatabaseAuditQueries(this));
    }

    throw new EISDatabaseException(
      "Unsupported query type: %s".formatted(qClass),
      SQL_ERROR_UNSUPPORTED_QUERY_CLASS
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
    throws EISDatabaseException
  {
    try {
      this.connection.connection().rollback();
      this.connection.database()
        .counterTransactionRollbacks()
        .add(1L);
    } catch (final SQLException e) {
      throw new EISDatabaseException(e.getMessage(), e, SQL_ERROR);
    }
  }

  @Override
  public void commit()
    throws EISDatabaseException
  {
    try {
      this.connection.connection().commit();
      this.connection.database()
        .counterTransactionCommits()
        .add(1L);
    } catch (final SQLException e) {
      throw new EISDatabaseException(e.getMessage(), e, SQL_ERROR);
    }
  }

  @Override
  public void close()
    throws EISDatabaseException
  {
    try {
      this.rollback();
    } catch (final Exception e) {
      this.transactionSpan.recordException(e);
      throw e;
    } finally {
      this.transactionSpan.end();
    }
  }

  /**
   * @return The metrics tracer
   */

  public Tracer tracer()
  {
    return this.connection.database().tracer();
  }
}
