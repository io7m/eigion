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
import com.io7m.eigion.model.EISubsetMatch;
import org.jline.terminal.Terminal;

import java.time.OffsetDateTime;

import static com.io7m.eigion.amberjack.cmdline.internal.EISCommandResult.SUCCESS;

/**
 * A command to retrieve audit logs.
 */

public final class EISCommandAudit
  extends EISAbstractCommand<EISCommandAudit.Parameters>
{
  /**
   * A command to retrieve audit logs by a time range.
   *
   * @param inController The controller
   * @param inStrings    The string resources
   */

  public EISCommandAudit(
    final EISController inController,
    final EISStrings inStrings)
  {
    super(inController, inStrings, "audit");
  }

  @Override
  protected Parameters createEmptyParameters()
  {
    return new Parameters();
  }

  @Override
  protected EISCommandResult runActual(
    final Terminal terminal,
    final Parameters params)
    throws EIAClientException, InterruptedException
  {
    final var writer = terminal.writer();

    final var events =
      this.controller()
        .client()
        .auditGet(
          params.dateLower,
          params.dateUpper,
          new EISubsetMatch<>(params.ownerInclude, params.ownerExclude),
          new EISubsetMatch<>(params.typeInclude, params.typeExclude),
          new EISubsetMatch<>(params.messageInclude, params.messageExclude)
        );

    if (!events.isEmpty()) {
      writer.println("# id | owner | time | type | message");
      for (final var event : events) {
        writer.printf(
          "%s | %s | %s | %s | %s%n",
          Long.toUnsignedString(event.id()),
          event.owner(),
          event.time(),
          event.type(),
          event.message()
        );
      }
    }
    return SUCCESS;
  }

  protected static final class Parameters
    implements EISParameterHolderType
  {
    @Parameter(
      description = "The date lower bound.",
      required = true,
      converter = EISOffsetDateTimeConverter.class,
      names = "--dateLower")
    private OffsetDateTime dateLower;

    @Parameter(
      description = "The date upper bound.",
      required = false,
      converter = EISOffsetDateTimeConverter.class,
      names = "--dateUpper")
    private OffsetDateTime dateUpper = OffsetDateTime.now();

    @Parameter(
      description = "The owners to exclude (an empty string excludes nothing)",
      required = false,
      names = "--ownerExclude")
    private String ownerExclude = "";

    @Parameter(
      description = "The owners to include (an empty string includes everything)",
      required = false,
      names = "--ownerInclude")
    private String ownerInclude = "";

    @Parameter(
      description = "The types to exclude (an empty string excludes nothing)",
      required = false,
      names = "--typeExclude")
    private String typeExclude = "";

    @Parameter(
      description = "The types to include (an empty string includes everything)",
      required = false,
      names = "--typeInclude")
    private String typeInclude = "";

    @Parameter(
      description = "The messages to exclude (an empty string excludes nothing)",
      required = false,
      names = "--messageExclude")
    private String messageExclude = "";

    @Parameter(
      description = "The messages to include (an empty string includes everything)",
      required = false,
      names = "--messageInclude")
    private String messageInclude = "";

    Parameters()
    {

    }
  }
}
