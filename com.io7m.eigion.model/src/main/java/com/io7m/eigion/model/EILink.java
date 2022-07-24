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

import java.net.URI;
import java.util.Objects;

/**
 * A link to a resource.
 *
 * @param relation The link relation
 * @param location The link location
 */

public record EILink(
  String relation,
  URI location)
{
  /**
   * A link to a resource.
   *
   * @param relation The link relation
   * @param location The link location
   */

  public EILink
  {
    Objects.requireNonNull(relation, "relation");
    Objects.requireNonNull(location, "location");

    if (relation.length() > 128) {
      throw new EIValidityException(
        "Link relations must be <= 128 characters");
    }
  }
}
