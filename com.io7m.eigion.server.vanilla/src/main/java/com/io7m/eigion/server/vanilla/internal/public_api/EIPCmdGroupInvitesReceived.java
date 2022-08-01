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

import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupInvitesReceived;
import com.io7m.eigion.protocol.public_api.v1.EISP1GroupInvite;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroupInvites;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseType;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseGroupsQueriesType;
import com.io7m.eigion.server.security.EISecurityException;
import com.io7m.eigion.server.vanilla.internal.EIHTTPErrorStatusException;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandExecutionResult;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandExecutorType;

import java.util.Objects;

/**
 * A request to list invites.
 */

public final class EIPCmdGroupInvitesReceived
  implements EICommandExecutorType<
  EIPCommandContext,
  EISP1CommandGroupInvitesReceived,
  EISP1ResponseType>
{
  /**
   * A request to list invites.
   */

  public EIPCmdGroupInvitesReceived()
  {

  }

  @Override
  public EICommandExecutionResult<EISP1ResponseType> execute(
    final EIPCommandContext context,
    final EISP1CommandGroupInvitesReceived command)
    throws
    EIServerDatabaseException,
    EIHTTPErrorStatusException,
    EISecurityException
  {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(command, "command");

    final var transaction =
      context.transaction();
    final var groupQueries =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    transaction.userIdSet(context.user().id());

    final var userInvites =
      groupQueries.groupInvitesReceivedByUser();

    final var invites =
      userInvites.stream()
        .map(EISP1GroupInvite::ofInvite)
        .toList();

    return new EICommandExecutionResult<>(
      200,
      new EISP1ResponseGroupInvites(context.requestId(), invites)
    );
  }
}
