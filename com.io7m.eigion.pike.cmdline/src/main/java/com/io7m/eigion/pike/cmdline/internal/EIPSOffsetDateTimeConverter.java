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

import com.beust.jcommander.IStringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * A lax converter for {@code OffsetDateTime} values.
 */

public final class EIPSOffsetDateTimeConverter
  implements IStringConverter<OffsetDateTime>
{
  private final DateTimeFormatter parser;

  /**
   * A lax converter for {@code OffsetDateTime} values.
   */

  public EIPSOffsetDateTimeConverter()
  {
    this.parser = DateTimeFormatter.ofPattern("yyyy-MM-dd['T'HH:mm:ss[Z]]");
  }

  @Override
  public OffsetDateTime convert(
    final String text)
  {
    final var temporal =
      this.parser.parseBest(
        text,
        OffsetDateTime::from,
        LocalDateTime::from,
        LocalDate::from);

    if (temporal instanceof OffsetDateTime o) {
      return o;
    }
    if (temporal instanceof LocalDateTime d) {
      return d.atOffset(ZoneOffset.UTC);
    }
    if (temporal instanceof LocalDate d) {
      return d.atStartOfDay(ZoneId.of("UTC")).toOffsetDateTime();
    }

    throw new IllegalArgumentException(
      "Unparseable date: %s".formatted(text)
    );
  }
}
