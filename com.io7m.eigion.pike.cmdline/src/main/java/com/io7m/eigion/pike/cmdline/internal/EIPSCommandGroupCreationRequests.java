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

package com.io7m.eigion.pike.cmdline.internal;

import com.io7m.eigion.pike.api.EIPClientException;
import org.jline.terminal.Terminal;

import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.Failed;
import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.InProgress;
import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.Succeeded;
import static com.io7m.eigion.pike.cmdline.internal.EIPSCommandResult.SUCCESS;

/**
 * List group creation requests.
 */

public final class EIPSCommandGroupCreationRequests
  extends EIPSAbstractCommand<EIPSCommandGroupCreationRequests.Parameters>
{
  /**
   * List group creation requests.
   *
   * @param inController The controller
   * @param inStrings    The string resources
   */

  public EIPSCommandGroupCreationRequests(
    final EIPSController inController,
    final EIPStrings inStrings)
  {
    super(inController, inStrings, "group-creation-requests");
  }

  @Override
  protected Parameters createEmptyParameters()
  {
    return new Parameters();
  }

  @Override
  protected EIPSCommandResult runActual(
    final Terminal terminal,
    final Parameters parameters)
    throws EIPClientException, InterruptedException
  {
    final var requests =
      this.controller()
        .client()
        .groupCreationRequests();

    final var writer = terminal.writer();

    final var str = this.strings();
    for (final var request : requests) {
      writer.println(
        str.format("groupCreateRequest.group", request.groupName().value())
      );
      writer.println(
        str.format("groupCreateRequest.token", request.token().value())
      );
      writer.println(
        str.format("groupCreateRequest.location", request.verificationURIs().get(0))
      );
      writer.println(
        str.format("groupCreateRequest.timeStarted", request.status().timeStarted())
      );

      final var status = request.status();
      writer.println(
        str.format("groupCreateRequest.status", status.name())
      );

      if (status instanceof InProgress) {
        // Nothing
      } else if (status instanceof Failed failed) {
        writer.println(
          str.format("groupCreateRequest.timeCompleted", failed.timeCompletedValue())
        );
        writer.println(
          str.format("groupCreateRequest.message", failed.message())
        );
      } else if (status instanceof Succeeded succeeded) {
        writer.println(
          str.format("groupCreateRequest.timeCompleted", succeeded.timeCompletedValue())
        );
      }

      writer.println();
    }
    return SUCCESS;
  }

  protected static final class Parameters
    implements EIPSParameterHolderType
  {
    Parameters()
    {

    }
  }
}
