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

import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupCreateRequests;
import com.io7m.eigion.protocol.public_api.v1.EISP1GroupCreationRequest;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroupCreateRequests;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseType;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseGroupsQueriesType;
import com.io7m.eigion.server.security.EISecurityException;
import com.io7m.eigion.server.vanilla.internal.EIHTTPErrorStatusException;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandExecutionResult;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandExecutorType;

import java.util.Objects;

/**
 * A request to list group creation requests.
 */

public final class EIPCmdGroupCreateRequests
  implements EICommandExecutorType<
  EIPCommandContext,
  EISP1CommandGroupCreateRequests,
  EISP1ResponseType>
{
  /**
   * A request to list group creation requests.
   */

  public EIPCmdGroupCreateRequests()
  {

  }

  @Override
  public EICommandExecutionResult<EISP1ResponseType> execute(
    final EIPCommandContext context,
    final EISP1CommandGroupCreateRequests command)
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

    final var requests =
      queries.groupCreationRequestsForUser(user.id());

    return new EICommandExecutionResult<>(
      200,
      new EISP1ResponseGroupCreateRequests(
        context.requestId(),
        requests.stream()
          .map(EISP1GroupCreationRequest::ofRequest)
          .toList()
      )
    );
  }
}
