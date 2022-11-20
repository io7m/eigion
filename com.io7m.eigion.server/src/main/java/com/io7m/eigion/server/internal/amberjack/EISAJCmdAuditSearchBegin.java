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

package com.io7m.eigion.server.internal.amberjack;

import com.io7m.eigion.error_codes.EIException;
import com.io7m.eigion.protocol.amberjack.EIAJCommandAuditSearchBegin;
import com.io7m.eigion.protocol.amberjack.EIAJResponseAuditSearch;
import com.io7m.eigion.protocol.amberjack.EIAJResponseType;
import com.io7m.eigion.server.database.api.EISDatabaseAuditQueriesType;
import com.io7m.eigion.server.internal.amberjack.security.EISecAJActionAuditRead;
import com.io7m.eigion.server.internal.amberjack.security.EISecAJPolicy;

/**
 * EIAJCommandAuditSearchBegin
 */

public final class EISAJCmdAuditSearchBegin
  extends EISAJCmdAbstract<EIAJCommandAuditSearchBegin>
{
  /**
   * EIAJCommandAuditSearchBegin
   */

  public EISAJCmdAuditSearchBegin()
  {

  }

  @Override
  protected EIAJResponseType executeActual(
    final EISAJCommandContext context,
    final EIAJCommandAuditSearchBegin command)
    throws EIException
  {
    final var session = context.userSession();
    final var user = session.user();
    EISecAJPolicy.policy().check(new EISecAJActionAuditRead(user));

    final var transaction =
      context.transaction();
    final var audit =
      transaction.queries(EISDatabaseAuditQueriesType.class);
    final var search =
      audit.auditEventsSearch(command.parameters());
    final var page =
      search.pageCurrent(audit);

    session.setAuditSearch(search);
    return new EIAJResponseAuditSearch(context.requestId(), page);
  }
}
