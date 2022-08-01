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

import com.io7m.eigion.protocol.admin_api.v1.EISA1Admin;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAdminGetByEmail;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseAdminGet;
import com.io7m.eigion.server.database.api.EIServerDatabaseAdminsQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.security.EISecActionAdminRead;
import com.io7m.eigion.server.security.EISecPolicyResultDenied;
import com.io7m.eigion.server.security.EISecurity;
import com.io7m.eigion.server.security.EISecurityException;
import com.io7m.eigion.server.vanilla.internal.EIHTTPErrorStatusException;

import static org.eclipse.jetty.http.HttpStatus.FORBIDDEN_403;

/**
 * A command to retrieve admins.
 */

public final class EIACmdAdminGetByEmail
  implements EIACommandExecutorType<EISA1CommandAdminGetByEmail>
{
  /**
   * A command to retrieve admins.
   */

  public EIACmdAdminGetByEmail()
  {

  }

  @Override
  public EIACommandExecutionResult execute(
    final EIACommandContext context,
    final EISA1CommandAdminGetByEmail command)
    throws
    EIServerDatabaseException,
    EISecurityException,
    EIHTTPErrorStatusException
  {
    if (EISecurity.check(new EISecActionAdminRead(context.admin()))
      instanceof EISecPolicyResultDenied denied) {
      throw new EIHTTPErrorStatusException(
        FORBIDDEN_403,
        "admin-read",
        denied.message()
      );
    }

    final var q =
      context.transaction().queries(EIServerDatabaseAdminsQueriesType.class);
    final var adminOpt =
      q.adminGetForEmail(command.email());

    if (adminOpt.isEmpty()) {
      return context.resultErrorFormatted(404, "notFound", "notFound");
    }

    final var admin = adminOpt.get();
    return new EIACommandExecutionResult(
      200,
      new EISA1ResponseAdminGet(
        context.requestId(),
        EISA1Admin.ofAdmin(admin))
    );
  }
}
