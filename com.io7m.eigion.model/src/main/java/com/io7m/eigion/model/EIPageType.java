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

import java.util.List;
import java.util.Optional;

/**
 * A set of items that have been collected into a single page of data. The next
 * page of data can be accessed using the value returned by the
 * {@link #lastKey()} method.
 *
 * @param <T> The type of data items
 * @param <K> The type of page order keys
 */

public interface EIPageType<T, K extends Comparable<K>>
{
  /**
   * @return A list of data items in the page
   */

  List<T> items();

  /**
   * A function that, given a data item, returns a page order key.
   *
   * @param x The item
   *
   * @return A page order key
   */

  K sortKeyOf(T x);

  /**
   * Obtain the last order key present on the page. This key can be used to
   * retrieve the next page of data from whatever API provided the current
   * page.
   *
   * @return The last key, if any
   */

  default Optional<K> lastKey()
  {
    final var items = this.items();
    if (items.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(this.sortKeyOf(items.get(items.size() - 1)));
  }
}
