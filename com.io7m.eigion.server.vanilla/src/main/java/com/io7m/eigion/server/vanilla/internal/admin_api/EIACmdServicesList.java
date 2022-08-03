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

import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandServicesList;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseServiceList;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseType;
import com.io7m.eigion.protocol.admin_api.v1.EISA1Service;
import com.io7m.eigion.server.security.EISecAdminActionServicesRead;
import com.io7m.eigion.server.security.EISecPolicyResultDenied;
import com.io7m.eigion.server.security.EISecurity;
import com.io7m.eigion.server.security.EISecurityException;
import com.io7m.eigion.server.vanilla.internal.EIHTTPErrorStatusException;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandExecutionResult;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandExecutorType;
import com.io7m.eigion.services.api.EIServiceType;

import static org.eclipse.jetty.http.HttpStatus.FORBIDDEN_403;

/**
 * A command to retrieve services.
 */

public final class EIACmdServicesList
  implements EICommandExecutorType<EIACommandContext, EISA1CommandServicesList, EISA1ResponseType>
{
  /**
   * A command to retrieve services.
   */

  public EIACmdServicesList()
  {

  }

  private static EISA1Service mapService(
    final EIServiceType s)
  {
    return new EISA1Service(
      s.getClass().getCanonicalName(),
      s.description());
  }

  @Override
  public EICommandExecutionResult<EISA1ResponseType> execute(
    final EIACommandContext context,
    final EISA1CommandServicesList command)
    throws EIHTTPErrorStatusException, EISecurityException
  {
    if (EISecurity.check(new EISecAdminActionServicesRead(context.admin()))
      instanceof EISecPolicyResultDenied denied) {
      throw new EIHTTPErrorStatusException(
        FORBIDDEN_403,
        "service-read",
        denied.message()
      );
    }

    return new EICommandExecutionResult<>(
      200,
      new EISA1ResponseServiceList(
        context.requestId(),
        context.services()
          .services()
          .stream()
          .map(EIACmdServicesList::mapService)
          .toList()
      )
    );
  }
}
