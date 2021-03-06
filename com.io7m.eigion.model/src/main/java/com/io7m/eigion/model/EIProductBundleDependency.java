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

import com.io7m.eigion.hash.EIHash;

import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * A dependency on a bundle.
 *
 * @param identifier The identifier of the target
 * @param version    The version of the target
 * @param hash       The hash of the target
 * @param links      A list of links describing where this bundle might be
 *                   found
 */

public record EIProductBundleDependency(
  EIProductIdentifier identifier,
  EIProductVersion version,
  EIHash hash,
  List<URI> links)
{
  /**
   * A dependency on a bundle.
   *
   * @param identifier The identifier of the target
   * @param version    The version of the target
   * @param hash       The hash of the target
   * @param links      A list of links describing where this bundle might be
   *                   found
   */

  public EIProductBundleDependency
  {
    Objects.requireNonNull(identifier, "identifier");
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(hash, "hash");
    Objects.requireNonNull(links, "links");
  }
}
