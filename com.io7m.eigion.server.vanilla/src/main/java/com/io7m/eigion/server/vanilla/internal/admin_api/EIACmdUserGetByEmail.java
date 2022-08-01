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

import com.io7m.eigion.model.EIUserEmail;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserGetByEmail;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseUserGet;
import com.io7m.eigion.protocol.admin_api.v1.EISA1User;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseUsersQueriesType;
import com.io7m.eigion.server.security.EISecActionUserRead;
import com.io7m.eigion.server.security.EISecPolicyResultDenied;
import com.io7m.eigion.server.security.EISecurity;
import com.io7m.eigion.server.security.EISecurityException;
import com.io7m.eigion.server.vanilla.internal.EIHTTPErrorStatusException;

import static org.eclipse.jetty.http.HttpStatus.FORBIDDEN_403;

/**
 * A command to retrieve users.
 */

public final class EIACmdUserGetByEmail
  implements EIACommandExecutorType<EISA1CommandUserGetByEmail>
{
  /**
   * A command to retrieve users.
   */

  public EIACmdUserGetByEmail()
  {

  }

  @Override
  public EIACommandExecutionResult execute(
    final EIACommandContext context,
    final EISA1CommandUserGetByEmail command)
    throws
    EIServerDatabaseException,
    EISecurityException,
    EIHTTPErrorStatusException
  {
    if (EISecurity.check(new EISecActionUserRead(context.admin()))
      instanceof EISecPolicyResultDenied denied) {
      throw new EIHTTPErrorStatusException(
        FORBIDDEN_403,
        "user-read",
        denied.message()
      );
    }

    final var q =
      context.transaction().queries(EIServerDatabaseUsersQueriesType.class);
    final var userOpt =
      q.userGetForEmail(new EIUserEmail(command.email()));

    if (userOpt.isEmpty()) {
      return context.resultErrorFormatted(404, "notFound", "notFound");
    }

    final var user = userOpt.get();
    return new EIACommandExecutionResult(
      200,
      new EISA1ResponseUserGet(
        context.requestId(),
        EISA1User.ofUser(user))
    );
  }
}
