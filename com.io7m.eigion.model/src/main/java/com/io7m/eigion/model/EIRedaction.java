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

/**
 * The reason an object has been redacted.
 *
 * @param reason The reason
 */

public record EIRedaction(String reason)
{
  /**
   * The reason an object has been redacted.
   *
   * @param reason The reason
   */

  public EIRedaction
  {
    Objects.requireNonNull(reason, "reason");

    if (reason.length() >= 256) {
      throw new IllegalArgumentException(
        "Redaction reasons must be < 256 characters");
    }
  }

  /**
   * Create a redaction.
   *
   * @param reason The reason
   *
   * @return The redaction
   */

  public static EIRedaction redaction(
    final String reason)
  {
    return new EIRedaction(reason);
  }

  /**
   * Create a redaction.
   *
   * @param reason The reason
   *
   * @return The redaction
   */

  public static Optional<EIRedaction> redactionOpt(
    final String reason)
  {
    return Optional.of(redaction(reason));
  }
}
