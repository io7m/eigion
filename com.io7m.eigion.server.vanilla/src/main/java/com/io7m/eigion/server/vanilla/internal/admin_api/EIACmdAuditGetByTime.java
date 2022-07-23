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


package com.io7m.eigion.server.vanilla.internal.admin_api;

import com.io7m.eigion.protocol.admin_api.v1.EISA1AuditEvent;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAuditGetByTime;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseAuditGet;
import com.io7m.eigion.server.database.api.EIServerDatabaseAuditQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;

/**
 * A command to retrieve audit logs by a time range.
 */

public final class EIACmdAuditGetByTime
  implements EIACommandExecutorType<EISA1CommandAuditGetByTime>
{
  /**
   * A command to retrieve audit logs by a time range.
   */

  public EIACmdAuditGetByTime()
  {

  }

  @Override
  public EIACommandExecutionResult execute(
    final EIACommandContext context,
    final EISA1CommandAuditGetByTime command)
    throws EIServerDatabaseException
  {
    final var auditQueries =
      context.transaction().queries(EIServerDatabaseAuditQueriesType.class);

    final var events =
      auditQueries.auditEvents(
        command.fromInclusive(),
        command.toInclusive()
      );

    return new EIACommandExecutionResult(
      200,
      new EISA1ResponseAuditGet(
        context.requestId(),
        events.stream()
          .map(EISA1AuditEvent::ofAuditEvent)
          .toList()
      )
    );
  }
}
