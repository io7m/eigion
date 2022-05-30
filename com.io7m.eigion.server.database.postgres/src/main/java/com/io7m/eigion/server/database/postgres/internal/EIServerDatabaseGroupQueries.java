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

import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseGroupsQueriesType;
import com.io7m.jaffirm.core.Postconditions;
import org.jooq.exception.DataAccessException;

import java.util.Objects;

import static com.io7m.eigion.server.database.postgres.internal.EIServerDatabaseExceptions.handleDatabaseException;
import static com.io7m.eigion.server.database.postgres.internal.Tables.GROUPS;
import static com.io7m.eigion.server.database.postgres.internal.tables.Audit.AUDIT;

final class EIServerDatabaseGroupQueries
  extends EIBaseQueries
  implements EIServerDatabaseGroupsQueriesType
{
  EIServerDatabaseGroupQueries(
    final EIServerDatabaseTransaction inTransaction)
  {
    super(inTransaction);
  }

  @Override
  public void groupCreate(
    final EIGroupName name)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(name, "name");

    final var owner =
      this.transaction().userId();
    final var context =
      this.transaction().createContext();

    try {
      final var existing =
        context.fetchOptional(GROUPS, GROUPS.NAME.eq(name.value()));

      if (existing.isPresent()) {
        throw new EIServerDatabaseException(
          "Group already exists",
          "group-duplicate"
        );
      }

      final var timeNow =
        this.currentTime();

      final var inserted =
        context.insertInto(GROUPS)
          .set(GROUPS.NAME, name.value())
          .set(GROUPS.CREATED, timeNow)
          .set(GROUPS.CREATOR, owner)
          .execute();

      Postconditions.checkPostconditionV(
        inserted == 1,
        "Expected to insert 1 record (inserted %d)",
        Integer.valueOf(inserted)
      );

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, timeNow)
          .set(AUDIT.TYPE, "GROUP_CREATED")
          .set(AUDIT.USER_ID, owner)
          .set(AUDIT.MESSAGE, name.value());

      insertAuditRecord(audit);
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    }
  }

  @Override
  public boolean groupExists(
    final EIGroupName name)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(name, "name");

    final var context =
      this.transaction().createContext();

    try {
      return context.fetchOptional(GROUPS, GROUPS.NAME.eq(name.value()))
        .isPresent();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    }
  }
}
