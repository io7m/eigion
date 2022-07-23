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
import com.io7m.eigion.amberjack.cmdline.EISExitException;
import org.jline.terminal.Terminal;

import java.net.URI;

import static com.io7m.eigion.amberjack.cmdline.internal.EISCommandResult.SUCCESS;

/**
 * Log in!
 */

public final class EISCommandLogin
  extends EISAbstractCommand<EISCommandLogin.Parameters>
{
  /**
   * Log in!
   *
   * @param inController The controller
   * @param inStrings    The string resources
   */

  public EISCommandLogin(
    final EISController inController,
    final EISStrings inStrings)
  {
    super(inController, inStrings, "login");
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
    throws EISExitException, EIAClientException, InterruptedException
  {
    this.controller()
      .client()
      .login(parameters.userName, parameters.password, parameters.server);

    return SUCCESS;
  }

  protected static final class Parameters
    implements EISParameterHolderType
  {
    @Parameter(
      description = "The username.",
      required = true,
      names = "--username")
    private String userName;

    @Parameter(
      description = "The password.",
      required = true,
      names = "--password")
    private String password;

    @Parameter(
      description = "The server base URI.",
      required = true,
      names = "--server")
    private URI server;

    Parameters()
    {

    }
  }
}
