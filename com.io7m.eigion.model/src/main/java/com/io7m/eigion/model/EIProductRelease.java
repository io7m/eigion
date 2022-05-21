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
import java.util.Objects;

/**
 * A release of a product.
 *
 * @param version             The version
 * @param bundleDependencies  The bundles upon which the release depends
 * @param productDependencies The products upon which the release depends
 * @param changes             The list of changes for the release
 */

public record EIProductRelease(
  EIProductVersion version,
  List<EIProductDependency> productDependencies,
  List<EIProductBundleDependency> bundleDependencies,
  List<EIChange> changes)
  implements Comparable<EIProductRelease>
{
  /**
   * A release of a product.
   *
   * @param version             The version
   * @param bundleDependencies  The bundles upon which the release depends
   * @param productDependencies The products upon which the release depends
   * @param changes             The list of changes for the release
   */

  public EIProductRelease
  {
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(productDependencies, "productDependencies");
    Objects.requireNonNull(bundleDependencies, "bundleDependencies");
    Objects.requireNonNull(changes, "changes");
  }

  @Override
  public int compareTo(
    final EIProductRelease other)
  {
    return this.version.compareTo(other.version);
  }
}
