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

package com.io7m.eigion.product.api;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A product.
 *
 * @param id                  The product identifier
 * @param bundleDependencies  The bundles upon which the product depends
 * @param productDependencies The products upon which the product depends
 * @param categories          The categories to which the product belongs
 */

public record EIProduct(
  EIProductIdentifier id,
  List<EIProductDependency> productDependencies,
  List<EIProductDependency> bundleDependencies,
  Set<EIProductCategory> categories)
{
  /**
   * A product.
   *
   * @param id                  The product identifier
   * @param bundleDependencies  The bundles upon which the product depends
   * @param productDependencies The products upon which the product depends
   * @param categories          The categories to which the product belongs
   */

  public EIProduct
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(productDependencies, "productDependencies");
    Objects.requireNonNull(bundleDependencies, "bundleDependencies");
    Objects.requireNonNull(categories, "categories");
  }
}
