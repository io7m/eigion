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

import com.io7m.eigion.amberjack.api.EIAClientException;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.terminal.Terminal;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.List;

import static com.io7m.eigion.amberjack.cmdline.internal.EISCommandResult.FAILURE;
import static com.io7m.eigion.amberjack.cmdline.internal.EISCommandResult.SUCCESS;
import static java.time.ZoneOffset.UTC;

/**
 * A command to retrieve audit logs by a time range.
 */

public final class EISCommandAuditGetByTime
  extends EISAbstractCommand
{
  private final DateTimeFormatter parser;

  /**
   * A command to retrieve audit logs by a time range.
   *
   * @param inController The controller
   * @param inStrings    The string resources
   */

  public EISCommandAuditGetByTime(
    final EISController inController,
    final EISStrings inStrings)
  {
    super(inController, inStrings, "audit-by-time");

    this.parser = DateTimeFormatter.ofPattern("yyyy-MM-dd['T'HH:mm:ss[Z]]");
  }

  @Override
  public EISCommandResult run(
    final Terminal terminal,
    final List<String> arguments)
    throws EIAClientException, InterruptedException
  {
    final var writer = terminal.writer();
    if (arguments.size() < 1) {
      writer.println(
        this.strings().format("audit-by-time.missingParameters"));
      return FAILURE;
    }

    final var dateLowerText = arguments.get(0);

    final TemporalAccessor dateLowerTA;
    try {
      dateLowerTA =
        this.parser.parseBest(
          dateLowerText, OffsetDateTime::from, LocalDate::from);
    } catch (final DateTimeException e) {
      writer.write(this.strings().format("error", e.getMessage()));
      return FAILURE;
    }

    final TemporalAccessor dateUpperTA;
    if (arguments.size() >= 2) {
      final var dateUpperText = arguments.get(1);
      try {
        dateUpperTA =
          this.parser.parseBest(
            dateUpperText, OffsetDateTime::from, LocalDate::from);
      } catch (final DateTimeException e) {
        writer.write(this.strings().format("error", e.getMessage()));
        return FAILURE;
      }
    } else {
      dateUpperTA = OffsetDateTime.now();
    }

    final var dateUpper =
      temporalToOffsetDateTime(dateUpperTA);
    final var dateLower =
      temporalToOffsetDateTime(dateLowerTA);

    final var events =
      this.controller().client().auditGetByTime(dateLower, dateUpper);

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

  private static OffsetDateTime temporalToOffsetDateTime(
    final TemporalAccessor t)
  {
    final OffsetDateTime dateUpper;
    if (t instanceof OffsetDateTime o) {
      dateUpper = o;
    } else if (t instanceof LocalDate o) {
      dateUpper = o.atStartOfDay(UTC).toOffsetDateTime();
    } else {
      throw new IllegalStateException();
    }
    return dateUpper;
  }

  @Override
  public List<Completer> argumentCompleters(
    final Collection<EISCommandType> values)
  {
    return List.of(new NullCompleter());
  }
}
