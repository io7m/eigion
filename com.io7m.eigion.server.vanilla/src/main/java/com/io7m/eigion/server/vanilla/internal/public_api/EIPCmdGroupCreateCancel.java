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

import com.io7m.eigion.model.EIGroupCreationRequestStatusType.Cancelled;
import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupCreateCancel;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroupCreateCancel;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseType;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseGroupsQueriesType;
import com.io7m.eigion.server.security.EISecUserActionGroupCreateCancel;
import com.io7m.eigion.server.security.EISecPolicyResultDenied;
import com.io7m.eigion.server.security.EISecurity;
import com.io7m.eigion.server.security.EISecurityException;
import com.io7m.eigion.server.vanilla.internal.EIHTTPErrorStatusException;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandExecutionResult;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandExecutorType;

import java.util.Objects;

import static org.eclipse.jetty.http.HttpStatus.FORBIDDEN_403;

/**
 * A request to cancel creating a group.
 */

public final class EIPCmdGroupCreateCancel
  implements EICommandExecutorType<
  EIPCommandContext,
  EISP1CommandGroupCreateCancel,
  EISP1ResponseType>
{
  /**
   * A request to cancel creating a group.
   */

  public EIPCmdGroupCreateCancel()
  {

  }

  @Override
  public EICommandExecutionResult<EISP1ResponseType> execute(
    final EIPCommandContext context,
    final EISP1CommandGroupCreateCancel command)
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
    final var token =
      new EIToken(command.token());
    final var queries =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var existingOpt =
      queries.groupCreationRequest(token);

    if (existingOpt.isEmpty()) {
      return context.resultErrorFormatted(404, "not-found", "notFound");
    }

    final var existing =
      existingOpt.get();

    final var action =
      new EISecUserActionGroupCreateCancel(user, existing);

    if (EISecurity.check(action) instanceof EISecPolicyResultDenied denied) {
      throw new EIHTTPErrorStatusException(
        FORBIDDEN_403,
        "group-create-cancel",
        denied.message()
      );
    }

    final var cancelled =
      existing.withStatus(
        new Cancelled(existing.status().timeStarted(), context.now())
      );

    queries.groupCreationRequestComplete(cancelled);

    return new EICommandExecutionResult<>(
      200,
      new EISP1ResponseGroupCreateCancel(context.requestId())
    );
  }
}
