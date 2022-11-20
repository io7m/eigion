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

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Parameters to search for group creation requests.
 *
 * @param owner The owner
 * @param limit The limit on the number of returned values
 */

public record EIGroupCreationRequestSearchParameters(
  Optional<UUID> owner,
  long limit)
{
  /**
   * Parameters to search for group creation requests.
   *
   * @param owner The name query
   * @param limit The limit on the number of returned values
   */

  public EIGroupCreationRequestSearchParameters
  {
    Objects.requireNonNull(owner, "owner");
  }

  /**
   * @return The limit on the number of returned values
   */

  @Override
  public long limit()
  {
    return Math.min(1000L, Math.max(1L, this.limit));
  }
}
