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

import com.io7m.eigion.model.EIAuditEvent;
import com.io7m.eigion.server.database.api.EIServerDatabaseAuditQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.postgres.internal.tables.records.AuditRecord;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import static com.io7m.eigion.server.database.postgres.internal.tables.Audit.AUDIT;

record EIServerDatabaseAuditQueries(
  EIServerDatabaseTransaction transaction)
  implements EIServerDatabaseAuditQueriesType
{
  private static EIAuditEvent toAuditEvent(
    final AuditRecord record)
  {
    return new EIAuditEvent(
      record.getId().longValue(),
      record.getUserId(),
      record.getTime(),
      record.getType(),
      record.getMessage()
    );
  }

  @Override
  public List<EIAuditEvent> auditEvents(
    final OffsetDateTime fromInclusive,
    final OffsetDateTime toInclusive)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(fromInclusive, "fromInclusive");
    Objects.requireNonNull(toInclusive, "toInclusive");

    final var context = this.transaction.createContext();
    try {
      try (var auditRecords = context.selectFrom(AUDIT)) {
        return auditRecords
          .where(AUDIT.TIME.ge(fromInclusive))
          .and(AUDIT.TIME.le(toInclusive))
          .stream()
          .map(EIServerDatabaseAuditQueries::toAuditEvent)
          .toList();
      }
    } catch (final Exception e) {
      throw new EIServerDatabaseException(e.getMessage(), e, "sql-error");
    }
  }
}
