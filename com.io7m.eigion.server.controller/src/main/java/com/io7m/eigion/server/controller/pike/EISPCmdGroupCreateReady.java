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
import com.io7m.eigion.model.EIGroupCreationRequestStatusType.InProgress;
import com.io7m.eigion.protocol.pike.EIPCommandGroupCreateReady;
import com.io7m.eigion.protocol.pike.EIPResponseGroupCreateReady;
import com.io7m.eigion.protocol.pike.EIPResponseType;
import com.io7m.eigion.server.controller.pike.security.EISecPActionGroupCreateReady;
import com.io7m.eigion.server.controller.pike.security.EISecPPolicy;
import com.io7m.eigion.server.database.api.EISDatabaseGroupsQueriesType;
import com.io7m.eigion.server.service.domaincheck.EISDomainCheckingType;

import java.util.Objects;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.GROUP_REQUEST_NONEXISTENT;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.GROUP_REQUEST_WRONG_STATE;
import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.NAME_IN_PROGRESS;

/**
 * EIPCommandGroupCreateReady
 */

public final class EISPCmdGroupCreateReady
  extends EISPCmdAbstract<EIPCommandGroupCreateReady>
{
  /**
   * EIPCommandGroupCreateReady
   */

  public EISPCmdGroupCreateReady()
  {

  }

  @Override
  protected EIPResponseType executeActual(
    final EISPCommandContext context,
    final EIPCommandGroupCreateReady command)
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

    final var existingOpt =
      groups.groupCreationRequest(command.token());

    if (existingOpt.isEmpty()) {
      throw context.failFormatted(
        404,
        GROUP_REQUEST_NONEXISTENT,
        "notFound"
      );
    }

    final var existing =
      existingOpt.get();
    final var action =
      new EISecPActionGroupCreateReady(user, existing);

    EISecPPolicy.policy().check(action);

    final var status = existing.status();
    if (!(status instanceof InProgress)) {
      throw context.failFormatted(
        400,
        GROUP_REQUEST_WRONG_STATE,
        "groupCreationWrongState",
        NAME_IN_PROGRESS,
        status.name()
      );
    }

    final var domainChecker =
      context.services()
        .requireService(EISDomainCheckingType.class);

    domainChecker.check(existing);

    return new EIPResponseGroupCreateReady(context.requestId());
  }
}
