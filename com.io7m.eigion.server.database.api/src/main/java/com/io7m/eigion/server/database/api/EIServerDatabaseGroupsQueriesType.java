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

package com.io7m.eigion.server.database.api;

import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIGroupRole;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The database queries involving groups.
 */

public non-sealed interface EIServerDatabaseGroupsQueriesType
  extends EIServerDatabaseQueriesType
{
  /**
   * @return The highest existing identifier
   *
   * @throws EIServerDatabaseException On errors
   */

  long groupIdentifierLast()
    throws EIServerDatabaseException;

  /**
   * Create a group, failing if one already exists with the given name.
   *
   * @param name        The group
   * @param userFounder The founding user
   *
   * @throws EIServerDatabaseException On errors
   */

  @EIServerDatabaseRequiresAdmin
  void groupCreate(
    EIGroupName name,
    UUID userFounder)
    throws EIServerDatabaseException;

  /**
   * Set the given user as a member of the given group, with the given roles.
   *
   * @param name   The group
   * @param userId The user
   * @param roles  The roles
   *
   * @throws EIServerDatabaseException On errors
   */

  @EIServerDatabaseRequiresAdmin
  void groupMembershipSet(
    EIGroupName name,
    UUID userId,
    Set<EIGroupRole> roles)
    throws EIServerDatabaseException;

  /**
   * Get the membership of the given user within the given group. Returns
   * nothing if the user is not a member of the group.
   *
   * @param name   The group
   * @param userId The user
   *
   * @return A non-empty value if the user is a member
   *
   * @throws EIServerDatabaseException On errors
   */

  Optional<Set<EIGroupRole>> groupMembershipGet(
    EIGroupName name,
    UUID userId)
    throws EIServerDatabaseException;

  /**
   * Remove the given user as a member of the given group.
   *
   * @param name   The group
   * @param userId The user
   *
   * @throws EIServerDatabaseException On errors
   */

  @EIServerDatabaseRequiresAdmin
  void groupMembershipRemove(
    EIGroupName name,
    UUID userId)
    throws EIServerDatabaseException;

  /**
   * @param name The group
   *
   * @return {@code true} if a group exists with the given name
   *
   * @throws EIServerDatabaseException On errors
   */

  boolean groupExists(EIGroupName name)
    throws EIServerDatabaseException;
}
