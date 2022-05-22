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

import com.io7m.anethum.common.ParseStatus;
import com.io7m.eigion.product.parser.api.EIProductReleaseParserType;
import com.io7m.eigion.product.parser.api.EIProductReleaseParsersType;
import com.io7m.eigion.product.parser.internal.EIProductReleaseParser;

import java.io.InputStream;
import java.net.URI;
import java.util.function.Consumer;

/**
 * The default factory of products parsers.
 */

public final class EIProductReleaseParsers
  implements EIProductReleaseParsersType
{
  /**
   * The default factory of products parsers.
   */

  public EIProductReleaseParsers()
  {

  }

  @Override
  public EIProductReleaseParserType createParserWithContext(
    final Void context,
    final URI source,
    final InputStream stream,
    final Consumer<ParseStatus> statusConsumer)
  {
    return new EIProductReleaseParser(source, stream, statusConsumer);
  }
}
