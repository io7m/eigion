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

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * A command to retrieve audit logs by a time range.
 *
 * @param fromInclusive The inclusive lower bound of the date/time range
 * @param toInclusive   The inclusive upper bound of the date/time range
 * @param messages      The messages to include/exclude
 * @param owners        The owners to include/exclude
 * @param types         The types to include/exclude
 */

@JsonDeserialize
@JsonSerialize
public record EISA1CommandAuditGet(
  @JsonProperty(value = "FromInclusive", required = true)
  OffsetDateTime fromInclusive,
  @JsonProperty(value = "ToInclusive", required = true)
  OffsetDateTime toInclusive,
  @JsonProperty(value = "Owners", required = true)
  EISA1SubsetMatch<String> owners,
  @JsonProperty(value = "Types", required = true)
  EISA1SubsetMatch<String> types,
  @JsonProperty(value = "Messages", required = true)
  EISA1SubsetMatch<String> messages)
  implements EISA1CommandType
{
  /**
   * A command to retrieve audit logs by a time range.
   *
   * @param fromInclusive The inclusive lower bound of the date/time range
   * @param toInclusive   The inclusive upper bound of the date/time range
   * @param messages      The messages to include/exclude
   * @param owners        The owners to include/exclude
   * @param types         The types to include/exclude
   */

  public EISA1CommandAuditGet
  {
    Objects.requireNonNull(fromInclusive, "fromInclusive");
    Objects.requireNonNull(toInclusive, "toInclusive");
    Objects.requireNonNull(owners, "owners");
    Objects.requireNonNull(types, "types");
    Objects.requireNonNull(messages, "messages");
  }
}
