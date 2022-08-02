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

import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupInviteCancel;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupInviteRespond;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroupInviteCancel;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroupInviteRespond;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseType;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseGroupsQueriesType;
import com.io7m.eigion.server.security.EISecActionGroupInviteCancel;
import com.io7m.eigion.server.security.EISecPolicyResultDenied;
import com.io7m.eigion.server.security.EISecurity;
import com.io7m.eigion.server.security.EISecurityException;
import com.io7m.eigion.server.vanilla.internal.EIHTTPErrorStatusException;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandExecutionResult;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandExecutorType;

import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import static com.io7m.eigion.model.EIGroupInviteStatus.ACCEPTED;
import static com.io7m.eigion.model.EIGroupInviteStatus.CANCELLED;
import static com.io7m.eigion.model.EIGroupInviteStatus.IN_PROGRESS;
import static com.io7m.eigion.model.EIGroupInviteStatus.REJECTED;
import static org.eclipse.jetty.http.HttpStatus.FORBIDDEN_403;

/**
 * A request to respond to a group invite.
 */

public final class EIPCmdGroupInviteRespond
  implements EICommandExecutorType<
  EIPCommandContext,
  EISP1CommandGroupInviteRespond,
  EISP1ResponseType>
{
  /**
   * A request to respond to a group invite.
   */

  public EIPCmdGroupInviteRespond()
  {

  }

  @Override
  public EICommandExecutionResult<EISP1ResponseType> execute(
    final EIPCommandContext context,
    final EISP1CommandGroupInviteRespond command)
    throws
    EIServerDatabaseException,
    EIHTTPErrorStatusException,
    EISecurityException
  {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(command, "command");

    final var transaction =
      context.transaction();
    final var user =
      context.user();

    final var groupQueries =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var token =
      new EIToken(command.token());
    final var inviteOpt =
      groupQueries.groupInviteGet(token);

    if (inviteOpt.isEmpty()) {
      return context.resultErrorFormatted(404, "notFound", "notFound");
    }

    final var invite = inviteOpt.get();
    if (!Objects.equals(invite.userBeingInvited(), user.id())) {
      return context.resultErrorFormatted(
        400,
        "group-invite-invitee",
        "cmd.groupInviteRespond.wrongUser"
      );
    }

    transaction.userIdSet(user.id());

    if (command.accepted()) {
      groupQueries.groupInviteSetStatus(token, ACCEPTED);
      groupQueries.groupMembershipSet(invite.group(), user.id(), Set.of());
    } else {
      groupQueries.groupInviteSetStatus(token, REJECTED);
    }

    return new EICommandExecutionResult<>(
      200,
      new EISP1ResponseGroupInviteRespond(context.requestId())
    );
  }
}
