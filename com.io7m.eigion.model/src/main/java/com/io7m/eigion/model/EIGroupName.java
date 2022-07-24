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
   * The pattern that defines a valid group name.
   */

  public static final Pattern VALID_GROUP_NAME =
    Pattern.compile(
      "([\\p{Alpha}][\\p{Alpha}\\p{Digit}_-]{0,64})(\\.[\\p{Alpha}][\\p{Alpha}\\p{Digit}_-]{0,64}){0,8}",
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

    if (!VALID_GROUP_NAME.matcher(value).matches()) {
      throw new EIValidityException(
        String.format(
          "Group name '%s' must match %s", value, VALID_GROUP_NAME));
    }
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
