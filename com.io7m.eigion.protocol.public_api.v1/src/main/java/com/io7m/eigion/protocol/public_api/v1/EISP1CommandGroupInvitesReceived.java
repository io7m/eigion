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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * A request to list invites received.
 *
 * @param since      Only list invites newer than this date
 * @param withStatus Only list invites with this status
 */

@JsonDeserialize
@JsonSerialize
public record EISP1CommandGroupInvitesReceived(
  @JsonProperty(value = "Since", required = true)
  OffsetDateTime since,
  @JsonProperty(value = "WithStatus")
  Optional<EISP1GroupInviteStatus> withStatus)
  implements EISP1CommandType
{
  /**
   * A request to list invites received.
   *
   * @param since      Only list invites newer than this date
   * @param withStatus Only list invites with this status
   */

  @JsonCreator
  public EISP1CommandGroupInvitesReceived
  {
    Objects.requireNonNull(since, "since");
    Objects.requireNonNull(withStatus, "withStatus");
  }
}
