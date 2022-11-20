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
import com.io7m.eigion.server.database.api.EISDatabaseMaintenanceQueriesType;
import com.io7m.jdeferthrow.core.ExceptionTracker;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import static com.io7m.eigion.server.database.postgres.internal.EISDatabaseExceptions.DEFAULT_HANDLER;
import static com.io7m.eigion.server.database.postgres.internal.EISDatabaseExceptions.handleDatabaseException;
import static com.io7m.eigion.server.database.postgres.internal.Tables.GROUPS_CREATION_REQUESTS;

final class EISDatabaseMaintenanceQueries
  extends EISBaseQueries
  implements EISDatabaseMaintenanceQueriesType
{
  EISDatabaseMaintenanceQueries(
    final EISDatabaseTransaction inTransaction)
  {
    super(inTransaction);
  }

  @Override
  public void runMaintenance()
    throws EISDatabaseException
  {
    final var exceptions =
      new ExceptionTracker<EISDatabaseException>();

    try {
      this.runExpireGroupCreationRequests();
    } catch (final EISDatabaseException e) {
      exceptions.addException(e);
    }

    exceptions.throwIfNecessary();
  }

  private void runExpireGroupCreationRequests()
    throws EISDatabaseException
  {
    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "EISDatabaseMaintenanceQueries.runExpireGroupCreationRequests");

    try {
      final var timeNow =
        this.currentTime();
      final var startedTooOld =
        GROUPS_CREATION_REQUESTS.CREATED.lessThan(timeNow.minusDays(3L));
      final var completedTooOld =
        GROUPS_CREATION_REQUESTS.COMPLETED.lessThan(timeNow.minusDays(2L));
      final var conditions =
        DSL.or(startedTooOld, completedTooOld);

      final var deleted =
        context.deleteFrom(GROUPS_CREATION_REQUESTS)
          .where(conditions)
          .execute();

      querySpan.setAttribute(
        "eigion.maintenance.expiredGroupCreations",
        Integer.toUnsignedLong(deleted)
      );
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, DEFAULT_HANDLER);
    } finally {
      querySpan.end();
    }
  }
}
