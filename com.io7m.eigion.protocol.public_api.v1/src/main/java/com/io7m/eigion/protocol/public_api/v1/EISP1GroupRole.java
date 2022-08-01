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

import com.io7m.eigion.model.EIGroupRole;
import com.io7m.eigion.protocol.api.EIProtocolFromModel;
import com.io7m.eigion.protocol.api.EIProtocolToModel;

/**
 * The role of a user within a group.
 */

public enum EISP1GroupRole
{
  /**
   * The user may invite users to the group.
   */

  USER_INVITE,

  /**
   * The user may dismiss users from the group.
   */

  USER_DISMISS,

  /**
   * The user is the founder of the group.
   */

  FOUNDER;

  /**
   * @return This role as a model role
   */

  @EIProtocolToModel
  public EIGroupRole toRole()
  {
    return switch (this) {
      case USER_INVITE -> EIGroupRole.USER_INVITE;
      case USER_DISMISS -> EIGroupRole.USER_DISMISS;
      case FOUNDER -> EIGroupRole.FOUNDER;
    };
  }

  /**
   * @param role The model role
   *
   * @return The v1 role
   */

  @EIProtocolFromModel
  public static EISP1GroupRole ofRole(
    final EIGroupRole role)
  {
    return switch (role) {
      case USER_INVITE -> USER_INVITE;
      case USER_DISMISS -> USER_DISMISS;
      case FOUNDER -> FOUNDER;
    };
  }
}
