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
import com.io7m.anethum.common.ParseStatus;
import com.io7m.eigion.model.EIProductDependency;

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
public final class EIv1ProductDependency
  implements EIv1FromV1Type<EIProductDependency>
{
  @JsonProperty(value = "ID", required = true)
  public final EIv1ProductId id;
  @JsonProperty(value = "Version", required = true)
  public final String version;
  @JsonProperty(value = "Hash", required = true)
  public final EIv1ProductHash hash;

  @JsonCreator
  public EIv1ProductDependency(
    @JsonProperty(value = "ID", required = true) final EIv1ProductId inId,
    @JsonProperty(value = "Version", required = true) final String inVersion,
    @JsonProperty(value = "Hash", required = true) final EIv1ProductHash inHash)
  {
    this.id =
      Objects.requireNonNull(inId, "id");
    this.version =
      Objects.requireNonNull(inVersion, "version");
    this.hash =
      Objects.requireNonNull(inHash, "hash");
  }

  @Override
  public Optional<EIProductDependency> toProduct(
    final URI source,
    final Consumer<ParseStatus> errorConsumer)
  {
    final var pId =
      this.id.toProduct(source, errorConsumer);
    final var pHash =
      this.hash.toProduct(source, errorConsumer);
    final var pVersion =
      EIv1ProductVersion.toProduct(source, errorConsumer, this.version);

    if (pId.isPresent() && pHash.isPresent() && pVersion.isPresent()) {
      return Optional.of(
        new EIProductDependency(
          pId.get(),
          pVersion.get(),
          pHash.get()
        ));
    }
    return Optional.empty();
  }
}
