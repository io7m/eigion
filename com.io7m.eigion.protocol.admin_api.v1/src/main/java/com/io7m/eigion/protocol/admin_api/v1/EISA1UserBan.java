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
import com.io7m.eigion.model.EIUserBan;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * The reason a user is banned.
 *
 * @param expires The expiration date of the ban, if any
 * @param reason  The ban reason
 */

@JsonDeserialize
@JsonSerialize
public record EISA1UserBan(
  @JsonProperty(value = "Expires", required = true)
  Optional<OffsetDateTime> expires,
  @JsonProperty(value = "Reason", required = true)
  String reason)
{
  /**
   * The reason a user is banned.
   *
   * @param expires The expiration date of the ban, if any
   * @param reason  The ban reason
   */

  public EISA1UserBan
  {
    Objects.requireNonNull(expires, "expires");
    Objects.requireNonNull(reason, "reason");
  }

  /**
   * Convert this to a model ban.
   *
   * @return The model ban
   */

  public EIUserBan toBan()
  {
    return new EIUserBan(this.expires, this.reason);
  }

  /**
   * Convert a model ban to a V1 ban.
   *
   * @param ban The model ban
   *
   * @return A ban
   */

  public static EISA1UserBan ofBan(
    final EIUserBan ban)
  {
    return new EISA1UserBan(
      ban.expires(),
      ban.reason()
    );
  }
}
