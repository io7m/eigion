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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The role of a user within a group.
 */

public enum EIGroupRole
{
  /**
   * A base role that indicates that the user is a member of the group.
   */

  MEMBER {
    @Override
    public int index()
    {
      return 3;
    }

    @Override
    public Set<EIGroupRole> impliedRoles()
    {
      return Set.of(MEMBER);
    }
  },

  /**
   * The user may invite users to the group.
   */

  USER_INVITE {
    @Override
    public int index()
    {
      return 0;
    }

    @Override
    public Set<EIGroupRole> impliedRoles()
    {
      return Set.of(USER_INVITE, MEMBER);
    }
  },

  /**
   * The user may dismiss users from the group.
   */

  USER_DISMISS {
    @Override
    public int index()
    {
      return 1;
    }

    @Override
    public Set<EIGroupRole> impliedRoles()
    {
      return Set.of(USER_DISMISS, MEMBER);
    }
  },

  /**
   * The user is the founder of the group.
   */

  FOUNDER {
    @Override
    public int index()
    {
      return 2;
    }

    @Override
    public Set<EIGroupRole> impliedRoles()
    {
      return EnumSet.allOf(EIGroupRole.class);
    }
  };

  private static final Map<Integer, EIGroupRole> BY_INDEX =
    Stream.of(values())
      .collect(Collectors.toMap(
        v -> Integer.valueOf(v.index()),
        Function.identity())
      );

  /**
   * @param index The role index
   *
   * @return The group role of the given index
   *
   * @throws IllegalArgumentException On unrecognized indices
   */

  public static EIGroupRole ofIndex(
    final int index)
    throws IllegalArgumentException
  {
    return Optional.ofNullable(BY_INDEX.get(Integer.valueOf(index)))
      .orElseThrow(() -> {
        return new IllegalArgumentException(
          "Unrecognized group role index: " + index
        );
      });
  }

  /**
   * @return The integer index value
   */

  public abstract int index();

  /**
   * @return The roles implied by this role
   */

  public abstract Set<EIGroupRole> impliedRoles();

}
