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

import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupInvite;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroupInvite;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseType;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseGroupsQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseUsersQueriesType;
import com.io7m.eigion.server.security.EISecUserActionGroupInvite;
import com.io7m.eigion.server.security.EISecPolicyResultDenied;
import com.io7m.eigion.server.security.EISecurity;
import com.io7m.eigion.server.security.EISecurityException;
import com.io7m.eigion.server.vanilla.internal.EIHTTPErrorStatusException;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandExecutionResult;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandExecutorType;

import java.util.Objects;
import java.util.Optional;

import static org.eclipse.jetty.http.HttpStatus.FORBIDDEN_403;

/**
 * A request to invite a user to a group.
 */

public final class EIPCmdGroupInvite
  implements EICommandExecutorType<
  EIPCommandContext,
  EISP1CommandGroupInvite,
  EISP1ResponseType>
{
  /**
   * A request to invite a user to a group.
   */

  public EIPCmdGroupInvite()
  {

  }

  @Override
  public EICommandExecutionResult<EISP1ResponseType> execute(
    final EIPCommandContext context,
    final EISP1CommandGroupInvite command)
    throws
    EIServerDatabaseException,
    EIHTTPErrorStatusException,
    EISecurityException
  {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(command, "command");

    final var transaction =
      context.transaction();
    final var userInviting =
      context.user();
    final var groupName =
      new EIGroupName(command.group());

    final var groupQueries =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);
    final var userQueries =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);
    final var userBeingInvited =
      userQueries.userGetRequire(command.userId());

    transaction.userIdSet(userInviting.id());

    final var userInvites =
      groupQueries.groupInvitesCreatedByUser(
        context.now().minusDays(1L),
        Optional.empty()
      );

    final var action =
      new EISecUserActionGroupInvite(
        userInviting,
        userBeingInvited,
        groupName,
        context.now(),
        userInvites
      );

    if (EISecurity.check(action) instanceof EISecPolicyResultDenied denied) {
      throw new EIHTTPErrorStatusException(
        FORBIDDEN_403,
        "group-invite",
        denied.message()
      );
    }

    final var invite =
      groupQueries.groupInvite(groupName, userBeingInvited.id());

    return new EICommandExecutionResult<>(
      200,
      new EISP1ResponseGroupInvite(context.requestId(), invite.token().value())
    );
  }
}
