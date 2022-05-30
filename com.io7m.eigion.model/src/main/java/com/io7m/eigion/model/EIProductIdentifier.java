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

import java.text.ParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.UNICODE_CHARACTER_CLASS;

/**
 * A product identifier.
 *
 * @param name  The artifact name
 * @param group The group name
 */

public record EIProductIdentifier(
  String group,
  String name)
  implements Comparable<EIProductIdentifier>
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
   * The pattern that defines a valid artifact name.
   */

  public static final Pattern VALID_ARTIFACT_NAME =
    Pattern.compile(
      "([\\p{Alpha}][\\p{Alpha}\\p{Digit}_-]{0,64})(\\.[\\p{Alpha}][\\p{Alpha}\\p{Digit}_-]{0,64}){0,8}",
      UNICODE_CHARACTER_CLASS
    );

  /**
   * A product identifier.
   *
   * @param name  The artifact name
   * @param group The group name
   */

  public EIProductIdentifier
  {
    Objects.requireNonNull(group, "groupName");
    Objects.requireNonNull(name, "artifactName");

    if (!VALID_GROUP_NAME.matcher(group).matches()) {
      throw new IllegalArgumentException(
        String.format(
          "Group name '%s' must match %s", group, VALID_GROUP_NAME));
    }
    if (!VALID_ARTIFACT_NAME.matcher(name).matches()) {
      throw new IllegalArgumentException(
        String.format(
          "Artifact name '%s' must match %s", group, VALID_ARTIFACT_NAME));
    }
  }

  /**
   * Parse an identifier.
   *
   * @param text The identifier text
   *
   * @return A parsed identifier
   *
   * @throws ParseException On parse errors
   * @see #show()
   */

  public static EIProductIdentifier parse(
    final String text)
    throws ParseException
  {
    final var segments = List.of(text.split(":"));
    if (segments.size() != 2) {
      throw new ParseException(text, 0);
    }
    return new EIProductIdentifier(segments.get(0), segments.get(1));
  }

  /**
   * @return The identifier as a string
   */

  public String show()
  {
    return String.format("%s:%s", this.group, this.name);
  }

  @Override
  public int compareTo(
    final EIProductIdentifier other)
  {
    return Comparator.comparing(EIProductIdentifier::group)
      .thenComparing(EIProductIdentifier::name)
      .compare(this, other);
  }
}
