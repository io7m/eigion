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

package com.io7m.eigion.product.parser.internal.v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.io7m.anethum.common.ParseSeverity;
import com.io7m.anethum.common.ParseStatus;
import com.io7m.eigion.product.api.EIProductHash;
import com.io7m.jlexing.core.LexicalPositions;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/*
 * These are effectively JSON DTOs and therefore are exempt from the usual style checks.
 */

// CHECKSTYLE:OFF

@JsonSerialize
@JsonDeserialize
public final class EIv1ProductHash
  implements EIv1FromV1Type<EIProductHash>
{
  @JsonProperty(value = "algorithm", required = true)
  public final String algorithm;
  @JsonProperty(value = "hash", required = true)
  public final String hash;

  @JsonCreator
  public EIv1ProductHash(
    @JsonProperty(value = "algorithm", required = true) final String inAlgorithm,
    @JsonProperty(value = "hash", required = true) final String inHash)
  {
    this.algorithm =
      Objects.requireNonNull(inAlgorithm, "algorithm");
    this.hash =
      Objects.requireNonNull(inHash, "hash");
  }

  @Override
  public Optional<EIProductHash> toProduct(
    final URI source,
    final Consumer<ParseStatus> errorConsumer)
  {
    try {
      return Optional.of(new EIProductHash(
        this.algorithm,
        this.hash
      ));
    } catch (final IllegalArgumentException e) {
      errorConsumer.accept(
        ParseStatus.builder()
          .setSeverity(ParseSeverity.PARSE_ERROR)
          .setErrorCode("invalid-hash")
          .setLexical(LexicalPositions.zeroWithFile(source))
          .setMessage(e.getMessage())
          .build()
      );
      return Optional.empty();
    }
  }
}
