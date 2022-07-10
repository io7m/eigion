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

package com.io7m.eigion.hash;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The hash of an object.
 *
 * @param hash      The ASCII-encoded hash
 * @param algorithm The JCE hash algorithm name (such as "SHA-256")
 */

public record EIHash(
  String algorithm,
  String hash)
  implements Serializable
{
  private static final Pattern VALID_HASH =
    Pattern.compile("[A-F0-9]{1,256}");

  /**
   * The hash of an object.
   *
   * @param hash      The ASCII-encoded hash
   * @param algorithm The JCE hash algorithm name (such as "SHA-256")
   */

  public EIHash
  {
    Objects.requireNonNull(algorithm, "algorithm");
    Objects.requireNonNull(hash, "hash");

    if (!VALID_HASH.matcher(hash).matches()) {
      throw new IllegalArgumentException(
        String.format("Hash '%s' must match %s", hash, VALID_HASH));
    }
  }

  @Override
  public String toString()
  {
    return "%s:%s".formatted(this.algorithm, this.hash);
  }
}
