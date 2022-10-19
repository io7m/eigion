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

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * An immutable set of roles.
 */

public final class EIGroupRoleSet
{
  private static final EIGroupRoleSet EMPTY =
    new EIGroupRoleSet(EnumSet.noneOf(EIGroupRole.class));

  private final EnumSet<EIGroupRole> roles;

  private EIGroupRoleSet(
    final EnumSet<EIGroupRole> inRoles)
  {
    this.roles =
      Objects.requireNonNull(inRoles, "inRoles");
  }

  /**
   * @return The empty set of roles
   */

  public static EIGroupRoleSet empty()
  {
    return EMPTY;
  }

  /**
   * @param roles The roles
   *
   * @return A set with the given roles
   */

  public static EIGroupRoleSet of(
    final EIGroupRole... roles)
  {
    return of(List.of(roles));
  }

  /**
   * @param roles The roles
   *
   * @return A set with the given roles
   */

  public static EIGroupRoleSet of(
    final Collection<EIGroupRole> roles)
  {
    if (roles.isEmpty()) {
      return EMPTY;
    }
    return new EIGroupRoleSet(EnumSet.copyOf(roles));
  }

  @Override
  public boolean equals(final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || !this.getClass().equals(o.getClass())) {
      return false;
    }
    final EIGroupRoleSet that = (EIGroupRoleSet) o;
    return this.roles.equals(that.roles);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(this.roles);
  }

  /**
   * @return This set as a raw integer array
   */

  public Integer[] asIntegers()
  {
    var index = 0;
    final var xs = new Integer[this.roles.size()];
    for (final var p : this.roles) {
      xs[index] = Integer.valueOf(p.index());
      ++index;
    }
    return xs;
  }

  /**
   * @return The set of roles
   */

  public Set<EIGroupRole> roles()
  {
    return Set.copyOf(this.roles);
  }

  /**
   * @param p The permission
   *
   * @return {@code true} if this set implies {@code p}
   */

  public boolean implies(
    final EIGroupRole p)
  {
    return this.roles.contains(p);
  }

  @Override
  public String toString()
  {
    return this.roles.toString();
  }
}
