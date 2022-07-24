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

import com.io7m.eigion.model.EIGroupRole;

/**
 * The role of a user within a group.
 */

public enum EISA1GroupRole
{
  /**
   * The user is the founder of the group.
   */

  FOUNDER;

  /**
   * @return This v1 group role as a model group role
   */

  public EIGroupRole toGroupRole()
  {
    return switch (this) {
      case FOUNDER -> EIGroupRole.FOUNDER;
    };
  }

  /**
   * Convert a model role to a v1 role.
   *
   * @param role The model role
   *
   * @return The model role as a v1 role
   */

  public static EISA1GroupRole ofGroupRole(
    final EIGroupRole role)
  {
    return switch (role) {
      case FOUNDER -> FOUNDER;
    };
  }
}