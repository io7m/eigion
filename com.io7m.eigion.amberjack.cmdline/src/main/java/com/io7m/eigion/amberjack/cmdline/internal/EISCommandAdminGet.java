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

import java.util.UUID;

import static com.io7m.eigion.amberjack.cmdline.internal.EISCommandResult.FAILURE;
import static com.io7m.eigion.amberjack.cmdline.internal.EISCommandResult.SUCCESS;
import static com.io7m.eigion.amberjack.cmdline.internal.EISAdmins.showAdmin;

/**
 * Find at most one admin by different parameters.
 */

public final class EISCommandAdminGet
  extends EISAbstractCommand<EISCommandAdminGet.Parameters>
{
  /**
   * Find at most one admin by different parameters.
   *
   * @param inController The controller
   * @param inStrings    The string resources
   */

  public EISCommandAdminGet(
    final EISController inController,
    final EISStrings inStrings)
  {
    super(inController, inStrings, "admin-get");
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
    final var writer = terminal.writer();
    if (parameters.name == null
        && parameters.email == null
        && parameters.id == null) {
      writer.println(this.strings().format("admin-get.oneRequired"));
      return FAILURE;
    }

    if (parameters.name != null) {
      this.controller()
        .client()
        .adminByName(parameters.name)
        .ifPresent(u -> showAdmin(this.strings(), terminal, u));
      return SUCCESS;
    }

    if (parameters.email != null) {
      this.controller()
        .client()
        .adminByEmail(parameters.email)
        .ifPresent(u -> showAdmin(this.strings(), terminal, u));
      return SUCCESS;
    }

    this.controller()
      .client()
      .adminById(parameters.id.toString())
      .ifPresent(u -> showAdmin(this.strings(), terminal, u));
    return SUCCESS;
  }

  protected static final class Parameters
    implements EISParameterHolderType
  {
    @Parameter(
      description = "The email address.",
      required = false,
      names = "--email")
    private String email;

    @Parameter(
      description = "The name.",
      required = false,
      names = "--name")
    private String name;

    @Parameter(
      description = "The ID.",
      required = false,
      converter = EISUUIDConverter.class,
      names = "--id")
    private UUID id;

    Parameters()
    {

    }
  }
}
