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

package com.io7m.eigion.amberjack.cmdline.internal;

import com.beust.jcommander.Parameter;
import com.io7m.eigion.amberjack.api.EIAClientException;
import com.io7m.eigion.amberjack.cmdline.EIAExitException;
import com.io7m.eigion.model.EIGroupInviteStatus;
import com.io7m.eigion.model.EIToken;
import org.jline.terminal.Terminal;

import static com.io7m.eigion.amberjack.cmdline.internal.EISCommandResult.SUCCESS;

/**
 * Set the status of a group invite.
 */

public final class EISCommandGroupInviteSetStatus
  extends EISAbstractCommand<EISCommandGroupInviteSetStatus.Parameters>
{
  /**
   * Set the status of a group invite.
   *
   * @param inController The controller
   * @param inStrings    The string resources
   */

  public EISCommandGroupInviteSetStatus(
    final EISController inController,
    final EISStrings inStrings)
  {
    super(inController, inStrings, "group-invite-set-status");
  }

  @Override
  protected Parameters createEmptyParameters()
  {
    return new Parameters();
  }

  @Override
  protected EISCommandResult runActual(
    final Terminal terminal,
    final Parameters parameters)
    throws EIAExitException, EIAClientException, InterruptedException
  {
    this.controller()
      .client()
      .groupInviteSetStatus(
        new EIToken(parameters.token),
        parameters.status
      );

    return SUCCESS;
  }

  protected static final class Parameters
    implements EISParameterHolderType
  {
    @Parameter(
      description = "The invite token.",
      required = true,
      names = "--token")
    private String token;

    @Parameter(
      description = "The desired invite status.",
      required = true,
      names = "--status")
    private EIGroupInviteStatus status;

    Parameters()
    {

    }
  }
}
