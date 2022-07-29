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

package com.io7m.eigion.protocol.admin_api.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.io7m.eigion.model.EISubsetMatch;

import java.util.Objects;

/**
 * A specification of a subset. Include all items matching {@code include}, and
 * then, from that set, exclude all items matching {@code exclude}.
 *
 * @param include The items to include
 * @param exclude The items to exclude
 * @param <T>     The type of values
 */

@JsonDeserialize
@JsonSerialize
public record EISA1SubsetMatch<T>(
  @JsonProperty(value = "Include", required = true)
  T include,
  @JsonProperty(value = "Exclude", required = true)
  T exclude)
{
  /**
   * A specification of a subset. Include all items matching {@code include},
   * and then, from that set, exclude all items matching {@code exclude}.
   *
   * @param include The items to include
   * @param exclude The items to exclude
   */

  public EISA1SubsetMatch
  {
    Objects.requireNonNull(include, "include");
    Objects.requireNonNull(exclude, "exclude");
  }

  /**
   * @return This subset match as a model subset match
   */

  public EISubsetMatch<T> toSubsetMatch()
  {
    return new EISubsetMatch<>(
      this.include,
      this.exclude
    );
  }

  /**
   * @param match The model match
   * @param <T>   The type of match values
   *
   * @return The model match as a v1 match
   */

  public static <T> EISA1SubsetMatch<T> ofSubsetMatch(
    final EISubsetMatch<T> match)
  {
    return new EISA1SubsetMatch<>(
      match.include(),
      match.exclude()
    );
  }
}
