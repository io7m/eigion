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

import com.beust.jcommander.Parameter;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.pike.api.EIPClientException;
import org.jline.terminal.Terminal;

import static com.io7m.eigion.pike.cmdline.internal.EIPSCommandResult.SUCCESS;

/**
 * Leave a group.
 */

public final class EIPSCommandGroupLeave
  extends EIPSAbstractCommand<EIPSCommandGroupLeave.Parameters>
{
  /**
   * Leave a group.
   *
   * @param inController The controller
   * @param inStrings    The string resources
   */

  public EIPSCommandGroupLeave(
    final EIPSController inController,
    final EIPStrings inStrings)
  {
    super(inController, inStrings, "group-leave");
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
    this.controller()
      .client()
      .groupLeave(new EIGroupName(parameters.groupName));

    return SUCCESS;
  }

  protected static final class Parameters
    implements EIPSParameterHolderType
  {
    @Parameter(
      description = "The group name.",
      required = true,
      names = "--group")
    private String groupName;

    Parameters()
    {

    }
  }
}
