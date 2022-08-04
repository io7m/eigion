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
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupGrant;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroupGrant;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseType;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseGroupsQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseUsersQueriesType;
import com.io7m.eigion.server.security.EISecPolicyResultDenied;
import com.io7m.eigion.server.security.EISecUserActionGroupGrant;
import com.io7m.eigion.server.security.EISecurity;
import com.io7m.eigion.server.security.EISecurityException;
import com.io7m.eigion.server.vanilla.internal.EIHTTPErrorStatusException;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandExecutionResult;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandExecutorType;

import java.util.HashSet;
import java.util.Objects;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.SECURITY_POLICY_DENIED;
import static org.eclipse.jetty.http.HttpStatus.FORBIDDEN_403;

/**
 * A request to grant a role within a group.
 */

public final class EIPCmdGroupGrant
  implements EICommandExecutorType<
  EIPCommandContext,
  EISP1CommandGroupGrant,
  EISP1ResponseType>
{
  /**
   * A request to leave a group.
   */

  public EIPCmdGroupGrant()
  {

  }

  @Override
  public EICommandExecutionResult<EISP1ResponseType> execute(
    final EIPCommandContext context,
    final EISP1CommandGroupGrant command)
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
    final var group =
      new EIGroupName(command.group());

    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);
    final var userReceiving =
      users.userGetRequire(command.userReceiving());
    final var role =
      command.role().toRole();
    final var action =
      new EISecUserActionGroupGrant(user, group, role, userReceiving);

    if (EISecurity.check(action) instanceof EISecPolicyResultDenied denied) {
      throw new EIHTTPErrorStatusException(
        FORBIDDEN_403,
        SECURITY_POLICY_DENIED,
        denied.message()
      );
    }

    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var existingRoles =
      userReceiving.groupMembership()
        .get(group);

    if (existingRoles == null) {
      throw new IllegalStateException(
        "Receiving user unexpectedly not in group!"
      );
    }

    final var newRoles = new HashSet<>(existingRoles);
    newRoles.add(role);
    groups.groupMembershipSet(group, userReceiving.id(), newRoles);

    return new EICommandExecutionResult<>(
      200,
      new EISP1ResponseGroupGrant(context.requestId())
    );
  }
}
