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


package com.io7m.eigion.server.vanilla.internal.public_api;

import com.io7m.eigion.protocol.public_api.v1.EISP1CommandUserSelf;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseType;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseUserSelf;
import com.io7m.eigion.protocol.public_api.v1.EISP1User;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseUsersQueriesType;
import com.io7m.eigion.server.security.EISecurityException;
import com.io7m.eigion.server.vanilla.internal.EIHTTPErrorStatusException;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandExecutionResult;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandExecutorType;

import java.util.Objects;

/**
 * A request to retrieve the user's own user profile.
 */

public final class EIPCmdUserSelf
  implements EICommandExecutorType<
  EIPCommandContext,
  EISP1CommandUserSelf,
  EISP1ResponseType>
{
  /**
   * A request to list invites.
   */

  public EIPCmdUserSelf()
  {

  }

  @Override
  public EICommandExecutionResult<EISP1ResponseType> execute(
    final EIPCommandContext context,
    final EISP1CommandUserSelf command)
    throws
    EIServerDatabaseException,
    EIHTTPErrorStatusException,
    EISecurityException
  {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(command, "command");

    final var transaction =
      context.transaction();
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var userId = context.user().id();
    transaction.userIdSet(userId);
    final var user = users.userGetRequire(userId);

    return new EICommandExecutionResult<>(
      200,
      new EISP1ResponseUserSelf(context.requestId(), EISP1User.ofUser(user))
    );
  }
}
