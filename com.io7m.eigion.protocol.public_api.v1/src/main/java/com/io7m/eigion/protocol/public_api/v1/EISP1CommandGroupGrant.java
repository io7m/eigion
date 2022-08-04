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

import java.util.Objects;
import java.util.UUID;

/**
 * A request to grant a role to a user.
 *
 * @param userReceiving  The user
 * @param group The group
 * @param role  The role to grant
 */

@JsonDeserialize
@JsonSerialize
public record EISP1CommandGroupGrant(
  @JsonProperty(value = "User", required = true)
  UUID userReceiving,
  @JsonProperty(value = "Group", required = true)
  String group,
  @JsonProperty(value = "Role", required = true)
  EISP1GroupRole role)
  implements EISP1CommandType
{
  /**
   * A request to grant a role to a user.
   *
   * @param userReceiving  The user
   * @param group The group
   * @param role  The role to grant
   */

  @JsonCreator
  public EISP1CommandGroupGrant
  {
    Objects.requireNonNull(userReceiving, "user");
    Objects.requireNonNull(group, "group");
    Objects.requireNonNull(role, "role");
  }
}
