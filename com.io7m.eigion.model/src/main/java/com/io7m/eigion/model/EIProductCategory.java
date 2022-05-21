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

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A product category.
 *
 * @param value     The category name
 * @param redaction The redaction, if any
 */

public record EIProductCategory(
  String value,
  Optional<EIRedaction> redaction)
  implements EIRedactableType
{
  private static final Pattern VALID_CATEGORY =
    Pattern.compile("\\p{Upper}[\\p{Alpha}\\p{Digit} ]{0,64}");

  /**
   * A product category.
   *
   * @param value     The category name
   * @param redaction The redaction, if any
   */

  public EIProductCategory
  {
    Objects.requireNonNull(value, "value");
    Objects.requireNonNull(redaction, "redaction");

    final var matcher = VALID_CATEGORY.matcher(value);
    if (!matcher.matches()) {
      throw new IllegalArgumentException(
        String.format("Category '%s' must match %s", value, VALID_CATEGORY));
    }
  }

  /**
   * Create a category.
   *
   * @param text The category name
   *
   * @return A category
   */

  public static EIProductCategory category(
    final String text)
  {
    return new EIProductCategory(text, Optional.empty());
  }
}
