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

import com.io7m.eigion.model.EIGroupMembership;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIGroupRole;

import java.util.Set;
import java.util.UUID;

/**
 * The database queries involving groups.
 */

public non-sealed interface EISDatabaseGroupsQueriesType
  extends EISDatabaseQueriesType
{
  /**
   * Create a new group.
   *
   * @param userId The founder of the group
   * @param name   The group name
   *
   * @throws EISDatabaseException On errors
   */

  void groupCreate(
    UUID userId,
    EIGroupName name)
    throws EISDatabaseException;

  /**
   * Specify that the given user has the given roles in the given group.
   *
   * @param name   The group
   * @param userId The user ID
   * @param roles  The roles
   *
   * @throws EISDatabaseException On errors
   */

  void groupUserUpdate(
    EIGroupName name,
    UUID userId,
    Set<EIGroupRole> roles)
    throws EISDatabaseException;

  /**
   * List the membership of a given group.
   *
   * @param name  The group name
   * @param limit The limit on page sizes
   *
   * @return A paged search query
   *
   * @throws EISDatabaseException On errors
   */

  EISDatabasePagedQueryType<EISDatabaseGroupsQueriesType, EIGroupMembership> groupRoles(
    EIGroupName name,
    long limit)
    throws EISDatabaseException;
}
