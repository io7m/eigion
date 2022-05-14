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

import com.io7m.anethum.common.ParseStatus;

import java.net.URI;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * An interface supported by objects that can be converted to product elements.
 *
 * @param <T> The type of target classes
 */

public interface EIv1FromV1Type<T>
{
  /**
   * Convert to a product.
   *
   * @param source        The source URI
   * @param errorConsumer An error consumer
   *
   * @return The converted value, if no errors occur
   */

  Optional<T> toProduct(
    URI source,
    Consumer<ParseStatus> errorConsumer);
}
