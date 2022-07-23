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

import com.io7m.eigion.model.EIPageType;
import com.io7m.eigion.model.EIProductIdentifier;
import com.io7m.eigion.model.EIProductSummary;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A product list was successfully retrieved.
 *
 * @param requestId The server-assigned request ID
 * @param items     The products
 */

public record EISP1ResponseProductList(
  UUID requestId,
  List<EIProductSummary> items)
  implements EISP1ResponseType,
  EIPageType<EIProductSummary, EIProductIdentifier>
{
  /**
   * A product list was successfully retrieved.
   *
   * @param requestId The server-assigned request ID
   * @param items     The products
   */

  public EISP1ResponseProductList
  {
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(items, "items");
  }

  @Override
  public EIProductIdentifier sortKeyOf(
    final EIProductSummary x)
  {
    return x.id();
  }
}
