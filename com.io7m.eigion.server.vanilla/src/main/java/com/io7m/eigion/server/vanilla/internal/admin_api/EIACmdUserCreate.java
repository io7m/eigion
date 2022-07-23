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

import com.io7m.eigion.model.EIPassword;
import com.io7m.eigion.model.EIPasswordException;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserCreate;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseUserCreate;
import com.io7m.eigion.protocol.admin_api.v1.EISA1User;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseUsersQueriesType;

import java.util.Objects;
import java.util.UUID;

/**
 * A command to create users.
 */

public final class EIACmdUserCreate
  implements EIACommandExecutorType<EISA1CommandUserCreate>
{
  /**
   * A command to create users.
   */

  public EIACmdUserCreate()
  {

  }

  @Override
  public EIACommandExecutionResult execute(
    final EIACommandContext context,
    final EISA1CommandUserCreate command)
    throws EIServerDatabaseException
  {
    final var userQueries =
      context.transaction().queries(EIServerDatabaseUsersQueriesType.class);

    final EIPassword password;
    try {
      password = command.password().toPassword();
    } catch (final EIPasswordException e) {
      return context.resultError(400, "protocol", e.getMessage());
    }

    final EIUser createdUser;
    try {
      createdUser = userQueries.userCreate(
        UUID.randomUUID(),
        command.name(),
        command.email(),
        context.now(),
        password
      );
    } catch (final EIServerDatabaseException e) {
      if (Objects.equals(e.errorCode(), "user-duplicate-name")) {
        return context.resultError(
          400,
          e.errorCode(),
          e.getMessage()
        );
      }
      throw e;
    }

    return new EIACommandExecutionResult(
      200,
      new EISA1ResponseUserCreate(
        context.requestId(),
        EISA1User.ofUser(createdUser)
      )
    );
  }
}
