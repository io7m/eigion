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


package com.io7m.eigion.product.parser;

import com.io7m.eigion.product.parser.api.EIProductReleaseSerializerType;
import com.io7m.eigion.product.parser.api.EIProductReleaseSerializersType;
import com.io7m.eigion.product.parser.api.EIProductsSerializerConfiguration;
import com.io7m.eigion.product.parser.internal.v1.EIv1ProductReleaseSerializer;

import java.io.OutputStream;
import java.net.URI;
import java.util.Objects;

/**
 * The default factory of products serializers.
 */

public final class EIProductReleaseSerializers
  implements EIProductReleaseSerializersType
{
  /**
   * The default factory of products serializers.
   */

  public EIProductReleaseSerializers()
  {

  }

  @Override
  public EIProductReleaseSerializerType createSerializerWithContext(
    final EIProductsSerializerConfiguration context,
    final URI target,
    final OutputStream stream)
  {
    Objects.requireNonNull(target, "target");
    Objects.requireNonNull(stream, "stream");

    final EIProductsSerializerConfiguration contextEx;
    if (context == null) {
      contextEx = new EIProductsSerializerConfiguration(1);
    } else {
      contextEx = context;
    }

    final var version = contextEx.formatVersion();
    return switch (version) {
      case 1 -> new EIv1ProductReleaseSerializer(target, stream);
      default -> throw new IllegalArgumentException(
        "Unrecognized format version: %d".formatted(version)
      );
    };
  }
}
