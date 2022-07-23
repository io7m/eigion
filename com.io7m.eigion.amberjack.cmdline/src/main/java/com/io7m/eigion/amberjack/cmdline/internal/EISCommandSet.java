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
import org.jline.terminal.Terminal;

import static com.io7m.eigion.amberjack.cmdline.internal.EIControllerFlag.EXIT_ON_FAILED_COMMAND;
import static com.io7m.eigion.amberjack.cmdline.internal.EISCommandResult.SUCCESS;

/**
 * Find at most one user by different parameters.
 */

public final class EISCommandSet
  extends EISAbstractCommand<EISCommandSet.Parameters>
{
  /**
   * Find at most one user by different parameters.
   *
   * @param inController The controller
   * @param inStrings    The string resources
   */

  public EISCommandSet(
    final EISController inController,
    final EISStrings inStrings)
  {
    super(inController, inStrings, "set");
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
    throws EIAClientException, InterruptedException
  {
    final var c = this.controller();
    c.setFlag(EXIT_ON_FAILED_COMMAND, parameters.exitOnFailedCommand);
    return SUCCESS;
  }

  protected static final class Parameters
    implements EISParameterHolderType
  {
    @Parameter(
      description = "Enable/disable exiting on command failures.",
      required = false,
      arity = 1,
      names = "--exit-on-failed-command")
    private boolean exitOnFailedCommand;

    Parameters()
    {

    }
  }
}
