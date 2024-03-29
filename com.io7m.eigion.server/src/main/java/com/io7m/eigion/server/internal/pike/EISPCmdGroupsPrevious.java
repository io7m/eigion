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


package com.io7m.eigion.server.internal.pike;

import com.io7m.eigion.error_codes.EIException;
import com.io7m.eigion.protocol.pike.EIPCommandGroupsPrevious;
import com.io7m.eigion.protocol.pike.EIPResponseGroups;
import com.io7m.eigion.protocol.pike.EIPResponseType;
import com.io7m.eigion.server.database.api.EISDatabaseGroupsQueriesType;

import java.util.Objects;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.USAGE_ERROR;

/**
 * EIPCommandGroupsPrevious
 */

public final class EISPCmdGroupsPrevious
  extends EISPCmdAbstract<EIPCommandGroupsPrevious>
{
  /**
   * EIPCommandGroupsPrevious
   */

  public EISPCmdGroupsPrevious()
  {

  }

  @Override
  protected EIPResponseType executeActual(
    final EISPCommandContext context,
    final EIPCommandGroupsPrevious command)
    throws EIException
  {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(command, "command");

    final var transaction =
      context.transaction();
    final var session =
      context.userSession();
    final var user =
      session.user();
    final var groups =
      transaction.queries(EISDatabaseGroupsQueriesType.class);

    final var search =
      session.groupRolesOwnSearch()
        .orElseThrow(() -> context.failFormatted(
          400, USAGE_ERROR, "errorSearchFirst")
        );

    return new EIPResponseGroups(
      context.requestId(),
      search.pagePrevious(groups)
    );
  }
}
