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

import com.io7m.eigion.server.database.api.EISDatabaseException;
import com.io7m.eigion.server.database.api.EISDatabaseTransactionType;
import org.jooq.exception.DataAccessException;
import org.postgresql.util.PSQLState;

import java.util.Objects;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.OPERATION_NOT_PERMITTED;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.SQL_ERROR;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.SQL_ERROR_FOREIGN_KEY;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.SQL_ERROR_UNIQUE;

/**
 * Functions to handle database exceptions.
 */

public final class EISDatabaseExceptions
{
  private EISDatabaseExceptions()
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

  public static EISDatabaseException handleDatabaseException(
    final EISDatabaseTransactionType transaction,
    final DataAccessException e)
  {
    final var m = e.getMessage();

    final EISDatabaseException result = switch (e.sqlState()) {
      case "42501" -> {
        yield new EISDatabaseException(m, e, OPERATION_NOT_PERMITTED);
      }

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
            case FOREIGN_KEY_VIOLATION -> {
              yield new EISDatabaseException(m, e, SQL_ERROR_FOREIGN_KEY);
            }
            case UNIQUE_VIOLATION -> {
              yield new EISDatabaseException(m, e, SQL_ERROR_UNIQUE);
            }
            default -> new EISDatabaseException(m, e, SQL_ERROR);
          };
        }

        yield new EISDatabaseException(m, e, SQL_ERROR);
      }
    };

    try {
      transaction.rollback();
    } catch (final EISDatabaseException ex) {
      result.addSuppressed(ex);
    }
    return result;
  }
}
