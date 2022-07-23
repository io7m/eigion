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

package com.io7m.eigion.server.api;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A shared secret used to access admin functionality.
 *
 * @param value The secret
 */

public record EIServerAdminSharedSecret(String value)
{
  private static final Pattern VALID_SECRET =
    Pattern.compile("[A-F0-9]{64}");

  /**
   * A shared secret used to access admin functionality.
   *
   * @param value The secret
   */

  public EIServerAdminSharedSecret
  {
    Objects.requireNonNull(value, "secret");

    if (!VALID_SECRET.matcher(value).matches()) {
      throw new IllegalArgumentException(
        String.format("Secret '%s' must match %s", value, VALID_SECRET));
    }
  }
}
