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

import com.io7m.eigion.server.database.api.EISDatabaseConnectionType;
import com.io7m.eigion.server.database.api.EISDatabaseException;
import com.io7m.eigion.server.database.api.EISDatabaseRole;
import com.io7m.eigion.server.database.api.EISDatabaseTransactionType;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;

import java.sql.Connection;
import java.sql.SQLException;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.SQL_ERROR;

record EISDatabaseConnection(
  EISDatabase database,
  Connection connection,
  EISDatabaseRole role,
  Span connectionSpan)
  implements EISDatabaseConnectionType
{
  @Override
  public EISDatabaseTransactionType openTransaction()
    throws EISDatabaseException
  {
    final var transactionSpan =
      this.database.tracer()
        .spanBuilder("EISDatabaseTransaction")
        .setParent(Context.current().with(this.connectionSpan))
        .startSpan();

    try {
      final var t =
        new EISDatabaseTransaction(
          this,
          transactionSpan
        );

      this.database.counterTransactions().add(1L);
      t.setRole(this.role);
      t.commit();
      return t;
    } catch (final SQLException e) {
      transactionSpan.recordException(e);
      transactionSpan.end();
      throw new EISDatabaseException(e.getMessage(), e, SQL_ERROR);
    }
  }

  @Override
  public void close()
    throws EISDatabaseException
  {
    try {
      if (!this.connection.isClosed()) {
        this.connection.close();
      }
    } catch (final SQLException e) {
      this.connectionSpan.recordException(e);
      throw new EISDatabaseException(e.getMessage(), e, SQL_ERROR);
    } finally {
      this.connectionSpan.end();
    }
  }
}
