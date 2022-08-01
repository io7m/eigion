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

import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIGroupRoles;
import com.io7m.eigion.protocol.api.EIProtocolFromModel;
import com.io7m.eigion.protocol.api.EIProtocolToModel;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The roles a user holds within a group.
 *
 * @param group The group
 * @param roles The roles
 */

public record EISP1GroupRoles(
  String group,
  Set<EISP1GroupRole> roles)
{
  /**
   * The roles a user holds within a group.
   *
   * @param group The group
   * @param roles The roles
   */

  public EISP1GroupRoles
  {
    Objects.requireNonNull(group, "group");
    Objects.requireNonNull(roles, "roles");
  }

  /**
   * @return These roles as model roles
   */

  @EIProtocolToModel
  public EIGroupRoles toRoles()
  {
    return new EIGroupRoles(
      new EIGroupName(this.group),
      this.roles.stream()
        .map(EISP1GroupRole::toRole)
        .collect(Collectors.toUnmodifiableSet())
    );
  }

  /**
   * @param roles The model roles
   *
   * @return The v1 roles
   */

  @EIProtocolFromModel
  public static EISP1GroupRoles ofRoles(
    final EIGroupRoles roles)
  {
    return new EISP1GroupRoles(
      roles.group().value(),
      roles.roles()
        .stream()
        .map(EISP1GroupRole::ofRole)
        .collect(Collectors.toUnmodifiableSet())
    );
  }
}
