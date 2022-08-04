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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.io7m.eigion.model.EIUserDisplayName;
import com.io7m.eigion.model.EIUserEmail;
import com.io7m.eigion.model.EIUserSummary;
import com.io7m.eigion.protocol.api.EIProtocolFromModel;
import com.io7m.eigion.protocol.api.EIProtocolToModel;

import java.util.Objects;
import java.util.UUID;

/**
 * A summary of the "identifier" parts of a user.
 *
 * @param id    The user ID
 * @param name  The user name
 * @param email The user email
 */

@JsonDeserialize
@JsonSerialize
public record EISP1UserSummary(
  @JsonProperty(value = "ID", required = true)
  UUID id,
  @JsonProperty(value = "Name", required = true)
  String name,
  @JsonProperty(value = "Email", required = true)
  String email)
{
  /**
   * A summary of the "identifier" parts of a user.
   *
   * @param id    The user ID
   * @param name  The user name
   * @param email The user email
   */

  public EISP1UserSummary
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(email, "email");
  }

  /**
   * A model summary as a v1 summary.
   *
   * @param u The summary
   *
   * @return A v1 summary
   */

  @EIProtocolFromModel
  public static EISP1UserSummary ofUserSummary(
    final EIUserSummary u)
  {
    return new EISP1UserSummary(
      u.id(),
      u.name().value(),
      u.email().value()
    );
  }

  /**
   * This summary as a model summary.
   *
   * @return A model summary
   */

  @EIProtocolToModel
  public EIUserSummary toUserSummary()
  {
    return new EIUserSummary(
      this.id,
      new EIUserDisplayName(this.name),
      new EIUserEmail(this.email)
    );
  }
}
