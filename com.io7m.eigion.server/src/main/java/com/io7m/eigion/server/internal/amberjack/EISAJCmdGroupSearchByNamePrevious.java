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
import com.io7m.eigion.protocol.amberjack.EIAJCommandGroupSearchByNamePrevious;
import com.io7m.eigion.protocol.amberjack.EIAJResponseGroupSearch;
import com.io7m.eigion.protocol.amberjack.EIAJResponseType;
import com.io7m.eigion.server.database.api.EISDatabaseGroupsQueriesType;
import com.io7m.eigion.server.internal.amberjack.security.EISecAJActionGroupSearch;
import com.io7m.eigion.server.internal.amberjack.security.EISecAJPolicy;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.USAGE_ERROR;

/**
 * EIAJCommandGroupSearchByNamePrevious
 */

public final class EISAJCmdGroupSearchByNamePrevious
  extends EISAJCmdAbstract<EIAJCommandGroupSearchByNamePrevious>
{
  /**
   * EIAJCommandGroupSearchByNamePrevious
   */

  public EISAJCmdGroupSearchByNamePrevious()
  {

  }

  @Override
  protected EIAJResponseType executeActual(
    final EISAJCommandContext context,
    final EIAJCommandGroupSearchByNamePrevious command)
    throws EIException
  {
    final var session = context.userSession();
    final var user = session.user();
    EISecAJPolicy.policy().check(new EISecAJActionGroupSearch(user));

    final var search =
      session.groupSearchByName()
        .orElseThrow(() -> context.failFormatted(
          400, USAGE_ERROR, "errorSearchFirst")
        );

    final var transaction =
      context.transaction();
    final var audit =
      transaction.queries(EISDatabaseGroupsQueriesType.class);
    final var page =
      search.pagePrevious(audit);

    return new EIAJResponseGroupSearch(context.requestId(), page);
  }
}
