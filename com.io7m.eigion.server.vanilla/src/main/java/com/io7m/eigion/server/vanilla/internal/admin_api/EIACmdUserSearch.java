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

import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserSearch;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseUserList;
import com.io7m.eigion.protocol.admin_api.v1.EISA1UserSummary;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseUsersQueriesType;

/**
 * A command to retrieve users.
 */

public final class EIACmdUserSearch
  implements EIACommandExecutorType<EISA1CommandUserSearch>
{
  /**
   * A command to retrieve users.
   */

  public EIACmdUserSearch()
  {

  }

  @Override
  public EIACommandExecutionResult execute(
    final EIACommandContext context,
    final EISA1CommandUserSearch command)
    throws EIServerDatabaseException
  {
    final var q =
      context.transaction().queries(EIServerDatabaseUsersQueriesType.class);
    final var users =
      q.userSearch(command.query());

    return new EIACommandExecutionResult(
      200,
      new EISA1ResponseUserList(
        context.requestId(),
        users.stream()
          .map(EISA1UserSummary::ofUserSummary)
          .toList()
      ));
  }
}
