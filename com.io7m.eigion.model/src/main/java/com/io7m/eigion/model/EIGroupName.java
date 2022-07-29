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

package com.io7m.eigion.model;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.UNICODE_CHARACTER_CLASS;

/**
 * The name of a group.
 *
 * @param value The name
 */

public record EIGroupName(String value)
  implements Comparable<EIGroupName>
{
  /**
   * The pattern that defines a valid group name segment.
   */

  public static final Pattern VALID_GROUP_NAME_SEGMENT =
    Pattern.compile(
      "[\\p{Alpha}][\\p{Alpha}\\p{Digit}_-]{0,255}",
      UNICODE_CHARACTER_CLASS
    );

  /**
   * The name of a group.
   *
   * @param value The name
   */

  public EIGroupName
  {
    Objects.requireNonNull(value, "value");

    final var segments = List.of(value.split("\\."));
    if (value.length() > 255 || segments.isEmpty()) {
      throw invalid(value);
    }

    for (final var segment : segments) {
      if (!VALID_GROUP_NAME_SEGMENT.matcher(segment).matches()) {
        throw invalid(value);
      }
    }
  }

  private static EIValidityException invalid(
    final String text)
  {
    return new EIValidityException(
      String.format(
        "Group name '%s' must consist of >= 1 repetitions of %s, and be <= 255 characters long",
        text,
        VALID_GROUP_NAME_SEGMENT)
    );
  }

  @Override
  public String toString()
  {
    return this.value;
  }

  @Override
  public int compareTo(final EIGroupName other)
  {
    return Comparator.comparing(EIGroupName::value)
      .compare(this, other);
  }
}
