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

import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandGroupInviteSetStatus;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseGroupInviteSetStatus;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseType;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseGroupsQueriesType;
import com.io7m.eigion.server.security.EISecAdminActionGroupInviteSetStatus;
import com.io7m.eigion.server.security.EISecPolicyResultDenied;
import com.io7m.eigion.server.security.EISecurity;
import com.io7m.eigion.server.security.EISecurityException;
import com.io7m.eigion.server.vanilla.internal.EIHTTPErrorStatusException;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandExecutionResult;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandExecutorType;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.SECURITY_POLICY_DENIED;
import static org.eclipse.jetty.http.HttpStatus.FORBIDDEN_403;

/**
 * A command to set the status of group invites.
 */

public final class EIACmdGroupInviteSetStatus
  implements EICommandExecutorType<EIACommandContext, EISA1CommandGroupInviteSetStatus, EISA1ResponseType>
{
  /**
   * A command to set the status of group invites.
   */

  public EIACmdGroupInviteSetStatus()
  {

  }

  @Override
  public EICommandExecutionResult<EISA1ResponseType> execute(
    final EIACommandContext context,
    final EISA1CommandGroupInviteSetStatus command)
    throws
    EIHTTPErrorStatusException,
    EISecurityException,
    EIServerDatabaseException
  {
    if (EISecurity.check(new EISecAdminActionGroupInviteSetStatus(context.admin()))
      instanceof EISecPolicyResultDenied denied) {
      throw new EIHTTPErrorStatusException(
        FORBIDDEN_403,
        SECURITY_POLICY_DENIED,
        denied.message()
      );
    }

    final var transaction =
      context.transaction();
    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    transaction.adminIdSet(context.admin().id());

    groups.groupInviteSetStatusAdmin(
      new EIToken(command.token()),
      command.status().toStatus()
    );

    return new EICommandExecutionResult<>(
      200,
      new EISA1ResponseGroupInviteSetStatus(context.requestId())
    );
  }
}
