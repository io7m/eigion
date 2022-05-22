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
import com.io7m.eigion.server.database.api.EIServerDatabaseTransactionType;
import org.jooq.exception.DataAccessException;
import org.postgresql.util.PSQLState;

import java.util.Objects;

/**
 * Functions to handle database exceptions.
 */

public final class EIServerDatabaseExceptions
{
  private EIServerDatabaseExceptions()
  {

  }

  /**
   * Handle a data access exception.
   *
   * @param transaction The transaction
   * @param e           The exception
   *
   * @return The resulting exception
   */

  public static EIServerDatabaseException handleDatabaseException(
    final EIServerDatabaseTransactionType transaction,
    final DataAccessException e)
  {
    final var m = e.getMessage();

    final EIServerDatabaseException result = switch (e.sqlState()) {
      case "42501" -> new EIServerDatabaseException(
        m,
        e,
        "operation-not-permitted");

      default -> {
        PSQLState actual = null;
        for (final var possible : PSQLState.values()) {
          if (Objects.equals(possible.getState(), e.sqlState())) {
            actual = possible;
            break;
          }
        }

        if (actual != null) {
          yield switch (actual) {
            case FOREIGN_KEY_VIOLATION -> new EIServerDatabaseException(
              m,
              e,
              "foreign-key-violation");
            case UNIQUE_VIOLATION -> new EIServerDatabaseException(
              m,
              e,
              "unique-violation");
            default -> new EIServerDatabaseException(m, e, "sql-error");
          };
        }

        yield new EIServerDatabaseException(m, e, "sql-error");
      }
    };

    try {
      transaction.rollback();
    } catch (final EIServerDatabaseException ex) {
      result.addSuppressed(ex);
    }
    return result;
  }
}
