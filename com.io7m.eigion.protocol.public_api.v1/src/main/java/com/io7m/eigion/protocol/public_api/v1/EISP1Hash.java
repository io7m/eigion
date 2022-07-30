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

package com.io7m.eigion.protocol.public_api.v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.io7m.eigion.hash.EIHash;
import com.io7m.eigion.protocol.api.EIProtocolFromModel;
import com.io7m.eigion.protocol.api.EIProtocolToModel;

import java.util.Objects;

// CHECKSTYLE:OFF

@JsonDeserialize
@JsonSerialize
public record EISP1Hash(
  @JsonProperty(value = "Algorithm", required = true)
  String algorithm,
  @JsonProperty(value = "Value", required = true)
  String value)
{
  @JsonCreator
  public EISP1Hash
  {
    Objects.requireNonNull(algorithm, "algorithm");
    Objects.requireNonNull(value, "value");
  }

  /**
   * @return This hash as a model hash
   */

  @EIProtocolToModel
  public EIHash toHash()
  {
    return new EIHash(this.algorithm, this.value);
  }

  /**
   * @param hash The model hash
   *
   * @return A v1 hash from the given model hash
   */

  @EIProtocolFromModel
  public static EISP1Hash ofHash(
    final EIHash hash)
  {
    return new EISP1Hash(hash.algorithm(), hash.hash());
  }
}
