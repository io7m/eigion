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

import com.io7m.eigion.protocol.admin_api.v1.EISA1AdminSummary;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAdminSearch;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseAdminList;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseType;
import com.io7m.eigion.server.database.api.EIServerDatabaseAdminsQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.security.EISecAdminActionAdminRead;
import com.io7m.eigion.server.security.EISecPolicyResultDenied;
import com.io7m.eigion.server.security.EISecurity;
import com.io7m.eigion.server.security.EISecurityException;
import com.io7m.eigion.server.vanilla.internal.EIHTTPErrorStatusException;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandExecutionResult;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandExecutorType;

import static org.eclipse.jetty.http.HttpStatus.FORBIDDEN_403;

/**
 * A command to retrieve admins.
 */

public final class EIACmdAdminSearch
  implements EICommandExecutorType<EIACommandContext, EISA1CommandAdminSearch, EISA1ResponseType>
{
  /**
   * A command to retrieve admins.
   */

  public EIACmdAdminSearch()
  {

  }

  @Override
  public EICommandExecutionResult<EISA1ResponseType> execute(
    final EIACommandContext context,
    final EISA1CommandAdminSearch command)
    throws
    EIServerDatabaseException,
    EISecurityException,
    EIHTTPErrorStatusException
  {
    if (EISecurity.check(new EISecAdminActionAdminRead(context.admin()))
      instanceof EISecPolicyResultDenied denied) {
      throw new EIHTTPErrorStatusException(
        FORBIDDEN_403,
        "admin-read",
        denied.message()
      );
    }

    final var q =
      context.transaction().queries(EIServerDatabaseAdminsQueriesType.class);
    final var admins =
      q.adminSearch(command.query());

    return new EICommandExecutionResult<>(
      200,
      new EISA1ResponseAdminList(
        context.requestId(),
        admins.stream()
          .map(EISA1AdminSummary::ofAdminSummary)
          .toList()
      ));
  }
}
