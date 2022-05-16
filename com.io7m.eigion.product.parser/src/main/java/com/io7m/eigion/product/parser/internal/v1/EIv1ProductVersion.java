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

import com.io7m.anethum.common.ParseSeverity;
import com.io7m.anethum.common.ParseStatus;
import com.io7m.eigion.model.EIProductVersion;
import com.io7m.jlexing.core.LexicalPositions;

import java.net.URI;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Functions over V1 product versions.
 */

public final class EIv1ProductVersion
{
  private EIv1ProductVersion()
  {

  }

  /**
   * Parse a version.
   *
   * @param source        The source URI
   * @param errorConsumer An error consumer
   * @param version       The version text
   *
   * @return A parsed version
   */

  public static Optional<EIProductVersion> toProduct(
    final URI source,
    final Consumer<ParseStatus> errorConsumer,
    final String version)
  {
    try {
      return Optional.of(EIProductVersion.parse(version));
    } catch (final IllegalArgumentException e) {
      errorConsumer.accept(
        ParseStatus.builder()
          .setSeverity(ParseSeverity.PARSE_ERROR)
          .setErrorCode("invalid-version")
          .setLexical(LexicalPositions.zeroWithFile(source))
          .setMessage(e.getMessage())
          .build()
      );
      return Optional.empty();
    }
  }
}
