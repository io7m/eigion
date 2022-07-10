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

package com.io7m.eigion.storage.api;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The specification of valid storage names.
 */

public final class EIStorageNames
{
  private static final Pattern NAME_SEGMENT =
    Pattern.compile("[a-z0-9][a-z0-9_\\-\\.]{1,253}");
  private static final Pattern SLASHES =
    Pattern.compile("/+");

  private EIStorageNames()
  {

  }

  /**
   * Check that a name is valid.
   *
   * @param name The name
   *
   * @return name
   *
   * @throws IllegalArgumentException If the name is not valid
   */

  public static String check(
    final String name)
    throws IllegalArgumentException
  {
    Objects.requireNonNull(name, "name");

    final var normalized =
      SLASHES.matcher(name).replaceAll("/");

    if (!normalized.startsWith("/")) {
      throw invalid(name);
    }

    if (normalized.length() >= 255) {
      throw invalid(normalized);
    }

    final var segments = normalized.split("/");
    for (int index = 0; index < segments.length; ++index) {
      if (validName(segments[index])) {
        throw invalid(normalized);
      }
    }
    return normalized;
  }

  private static boolean validName(
    final String segment)
  {
    return NAME_SEGMENT.matcher(segment).matches();
  }

  private static IllegalArgumentException invalid(
    final String name)
  {
    final var lineSeparator = System.lineSeparator();
    final var m = new StringBuilder(256);
    m.append("Name is invalid.");
    m.append(lineSeparator);
    m.append("  Received: ");
    m.append(name);
    m.append(lineSeparator);
    m.append("  Expected: '/' <name> ('/' <name>)* (Maximum length: 255)");
    m.append(lineSeparator);
    return new IllegalArgumentException(m.toString());
  }
}
