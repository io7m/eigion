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

import java.util.EnumSet;
import java.util.Set;

/**
 * The role of a user within a group.
 */

public enum EIGroupRole
{
  /**
   * The user may invite users to the group.
   */

  USER_INVITE {
    @Override
    public Set<EIGroupRole> impliedRoles()
    {
      return Set.of(USER_INVITE);
    }
  },

  /**
   * The user may dismiss users from the group.
   */

  USER_DISMISS {
    @Override
    public Set<EIGroupRole> impliedRoles()
    {
      return Set.of(USER_DISMISS);
    }
  },

  /**
   * The user is the founder of the group.
   */

  FOUNDER {
    @Override
    public Set<EIGroupRole> impliedRoles()
    {
      return EnumSet.allOf(EIGroupRole.class);
    }
  };

  /**
   * @return The roles implied by this role
   */

  public abstract Set<EIGroupRole> impliedRoles();

  /**
   * @param r The checked role
   *
   * @return {@code true} if this role implies {@code r}
   */

  public boolean implies(
    final EIGroupRole r)
  {
    return this.impliedRoles().contains(r);
  }

  /**
   * Determine if any of the set of given roles implies {@code role}.
   *
   * @param roles The roles
   * @param role  The role
   *
   * @return {@code true} if any of the roles imply {@code role}
   */

  public static boolean roleSetImplies(
    final Set<EIGroupRole> roles,
    final EIGroupRole role)
  {
    return roles.stream().anyMatch(rs -> rs.implies(role));
  }
}
