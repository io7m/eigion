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
 * An immutable set of permissions.
 */

public final class EIPermissionSet
{
  private static final EIPermissionSet EMPTY =
    new EIPermissionSet(EnumSet.noneOf(EIPermission.class));

  private final EnumSet<EIPermission> permissions;

  private EIPermissionSet(
    final EnumSet<EIPermission> inPermissions)
  {
    this.permissions =
      Objects.requireNonNull(inPermissions, "inPermissions");
  }

  /**
   * @return The empty set of permissions
   */

  public static EIPermissionSet empty()
  {
    return EMPTY;
  }

  /**
   * @param permissions The permissions
   *
   * @return A set with the given permissions
   */

  public static EIPermissionSet of(
    final EIPermission... permissions)
  {
    return of(List.of(permissions));
  }

  /**
   * @param permissions The permissions
   *
   * @return A set with the given permissions
   */

  public static EIPermissionSet of(
    final Collection<EIPermission> permissions)
  {
    if (permissions.isEmpty()) {
      return EMPTY;
    }
    return new EIPermissionSet(EnumSet.copyOf(permissions));
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
    final EIPermissionSet that = (EIPermissionSet) o;
    return this.permissions.equals(that.permissions);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(this.permissions);
  }

  /**
   * @return This set as a raw integer array
   */

  public Integer[] asIntegers()
  {
    var index = 0;
    final var xs = new Integer[this.permissions.size()];
    for (final var p : this.permissions) {
      xs[index] = Integer.valueOf(p.value());
      ++index;
    }
    return xs;
  }

  /**
   * @return The set of permissions
   */

  public Set<EIPermission> permissions()
  {
    return Set.copyOf(this.permissions);
  }

  /**
   * @param p The permission
   *
   * @return {@code true} if this set implies {@code p}
   */

  public boolean implies(
    final EIPermission p)
  {
    return this.permissions.contains(p);
  }
}
