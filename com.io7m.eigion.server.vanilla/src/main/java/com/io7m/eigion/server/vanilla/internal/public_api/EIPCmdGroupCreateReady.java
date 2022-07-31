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
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupCreateReady;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroupCreateReady;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseType;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseGroupsQueriesType;
import com.io7m.eigion.server.security.EISecActionGroupCreateReady;
import com.io7m.eigion.server.security.EISecPolicyResultDenied;
import com.io7m.eigion.server.security.EISecurity;
import com.io7m.eigion.server.security.EISecurityException;
import com.io7m.eigion.server.vanilla.internal.EIHTTPErrorStatusException;
import com.io7m.eigion.server.vanilla.internal.EIServerDomainChecking;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandExecutionResult;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandExecutorType;

import java.util.Objects;

import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.InProgress;
import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.NAME_IN_PROGRESS;
import static org.eclipse.jetty.http.HttpStatus.FORBIDDEN_403;

/**
 * A request to tell the user that the group creation request is ready for
 * checking.
 */

public final class EIPCmdGroupCreateReady
  implements EICommandExecutorType<
  EIPCommandContext,
  EISP1CommandGroupCreateReady,
  EISP1ResponseType>
{
  /**
   * A request to tell the user that the group creation request is ready for
   * checking.
   */

  public EIPCmdGroupCreateReady()
  {

  }

  @Override
  public EICommandExecutionResult<EISP1ResponseType> execute(
    final EIPCommandContext context,
    final EISP1CommandGroupCreateReady command)
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
    final var queries =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var token =
      new EIToken(command.token());
    final var existingOpt =
      queries.groupCreationRequest(token);

    if (existingOpt.isEmpty()) {
      return context.resultErrorFormatted(404, "not-found", "notFound");
    }

    final var existing =
      existingOpt.get();
    final var action =
      new EISecActionGroupCreateReady(user, existing);

    if (EISecurity.check(action) instanceof EISecPolicyResultDenied denied) {
      throw new EIHTTPErrorStatusException(
        FORBIDDEN_403,
        "group-create-ready",
        denied.message()
      );
    }

    final var status = existing.status();
    if (!(status instanceof InProgress)) {
      return context.resultErrorFormatted(
        400,
        "group-create-wrong-state",
        "cmd.groupCreateReady.notInProgress",
        NAME_IN_PROGRESS,
        status.name()
      );
    }

    final var domainChecker =
      context.services()
        .requireService(EIServerDomainChecking.class);

    domainChecker.check(existing);

    return new EICommandExecutionResult<>(
      200,
      new EISP1ResponseGroupCreateReady(context.requestId())
    );
  }
}
