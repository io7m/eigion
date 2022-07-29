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
import com.io7m.eigion.model.EIAdminSummary;
import org.jline.terminal.Terminal;

import java.util.List;

import static com.io7m.eigion.amberjack.cmdline.internal.EISCommandResult.SUCCESS;

/**
 * Search for a admin.
 */

public final class EISCommandAdminSearch
  extends EISAbstractCommand<EISCommandAdminSearch.Parameters>
{
  /**
   * Search for a admin.
   *
   * @param inController The controller
   * @param inStrings    The string resources
   */

  public EISCommandAdminSearch(
    final EISController inController,
    final EISStrings inStrings)
  {
    super(inController, inStrings, "admin-search");
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

    final List<EIAdminSummary> admins =
      this.controller()
        .client()
        .adminSearch(parameters.query);

    for (final var admin : admins) {
      writer.printf("%s | %s | %s%n", admin.id(), admin.name(), admin.email());
    }

    return SUCCESS;
  }

  protected static final class Parameters
    implements EISParameterHolderType
  {
    @Parameter(
      description = "The search query.",
      required = true,
      names = "--query")
    private String query;

    Parameters()
    {

    }
  }
}
