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


package com.io7m.eigion.server.controller.pike;

import com.io7m.eigion.error_codes.EIException;
import com.io7m.eigion.model.EIGroupCreationRequest;
import com.io7m.eigion.model.EIGroupCreationRequestStatusType;
import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.protocol.pike.EIPCommandGroupCreateBegin;
import com.io7m.eigion.protocol.pike.EIPResponseGroupCreateBegin;
import com.io7m.eigion.protocol.pike.EIPResponseType;
import com.io7m.eigion.server.controller.pike.security.EISecPActionGroupCreateBegin;
import com.io7m.eigion.server.controller.pike.security.EISecPPolicy;
import com.io7m.eigion.server.database.api.EISDatabaseGroupsQueriesType;

import java.util.Objects;

/**
 * EIPCommandGroupCreateBegin
 */

public final class EISPCmdGroupCreateBegin
  extends EISPCmdAbstract<EIPCommandGroupCreateBegin>
{
  /**
   * EIPCommandGroupCreateBegin
   */

  public EISPCmdGroupCreateBegin()
  {

  }

  @Override
  protected EIPResponseType executeActual(
    final EISPCommandContext context,
    final EIPCommandGroupCreateBegin command)
    throws EIException
  {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(command, "command");

    final var transaction =
      context.transaction();
    final var user =
      context.user();
    final var groups =
      transaction.queries(EISDatabaseGroupsQueriesType.class);

    final var existing =
      groups.groupCreationRequestsForUser(user.id());
    final var groupName =
      command.groupName();
    final var action =
      new EISecPActionGroupCreateBegin(
        user, context.now(), existing, groupName);

    EISecPPolicy.policy().check(action);

    final var request =
      new EIGroupCreationRequest(
        groupName,
        user.id(),
        EIToken.generate(),
        new EIGroupCreationRequestStatusType.InProgress(context.now())
      );

    groups.groupCreationRequestStart(request);

    return new EIPResponseGroupCreateBegin(
      context.requestId(),
      groupName,
      request.token(),
      request.verificationURIs().get(0)
    );
  }
}
