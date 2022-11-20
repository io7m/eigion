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

import com.io7m.eigion.model.EIGroupCreationRequest;
import com.io7m.eigion.model.EIGroupCreationRequestSearchParameters;
import com.io7m.eigion.model.EIGroupMembership;
import com.io7m.eigion.model.EIGroupMembershipWithUser;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIGroupPrefix;
import com.io7m.eigion.model.EIGroupRole;
import com.io7m.eigion.model.EIGroupSearchByNameParameters;
import com.io7m.eigion.model.EIToken;

import java.util.List;
import java.util.Optional;
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
   * Create a new personal group.
   *
   * @param userId The founder of the group
   * @param prefix The group prefix
   *
   * @return The new group name
   *
   * @throws EISDatabaseException On errors
   */

  EIGroupName groupCreatePersonal(
    UUID userId,
    EIGroupPrefix prefix)
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

  EISDatabaseGroupsPagedQueryType<EIGroupMembershipWithUser> groupRoles(
    EIGroupName name,
    long limit)
    throws EISDatabaseException;

  /**
   * Search groups by name.
   *
   * @param parameters The parameters
   *
   * @return A paged search query
   *
   * @throws EISDatabaseException On errors
   */

  EISDatabaseGroupsPagedQueryType<EIGroupName> groupSearchByName(
    EIGroupSearchByNameParameters parameters)
    throws EISDatabaseException;

  /**
   * Start a group creation request.
   *
   * @param request The request
   *
   * @throws EISDatabaseException On errors
   */

  void groupCreationRequestStart(
    EIGroupCreationRequest request)
    throws EISDatabaseException;

  /**
   * @param userId The user ID
   *
   * @return A history of the group creation requests for the given user.
   *
   * @throws EISDatabaseException On errors
   * @deprecated Use
   * {@link
   * #groupCreationRequestsSearch(EIGroupCreationRequestSearchParameters)}
   */

  @Deprecated
  List<EIGroupCreationRequest> groupCreationRequestsForUser(
    UUID userId)
    throws EISDatabaseException;

  /**
   * @param parameters The parameters
   *
   * @return A list of matching group creation requests
   *
   * @throws EISDatabaseException On errors
   */

  EISDatabaseGroupsPagedQueryType<EIGroupCreationRequest> groupCreationRequestsSearch(
    EIGroupCreationRequestSearchParameters parameters)
    throws EISDatabaseException;

  /**
   * A group creation request can become obsolete if a group is created before
   * the request is completed. This method returns all the group requests that
   * are obsolete for any reason.
   *
   * @return All the obsolete group creation requests
   *
   * @throws EISDatabaseException On errors
   */

  List<EIGroupCreationRequest> groupCreationRequestsObsolete()
    throws EISDatabaseException;

  /**
   * @return The group creation requests that refer to nonexistent groups, and
   * that have not yet completed
   *
   * @throws EISDatabaseException On errors
   */

  List<EIGroupCreationRequest> groupCreationRequestsActive()
    throws EISDatabaseException;

  /**
   * @param token The token
   *
   * @return The request associated with the given token, if any
   *
   * @throws EISDatabaseException On errors
   */

  Optional<EIGroupCreationRequest> groupCreationRequest(
    EIToken token)
    throws EISDatabaseException;

  /**
   * Finish a group creation request. This checks the supplied request and then,
   * on success, acts as {@link #groupCreate(UUID, EIGroupName)}.
   *
   * @param request The request
   *
   * @throws EISDatabaseException On errors
   */

  void groupCreationRequestComplete(
    EIGroupCreationRequest request)
    throws EISDatabaseException;

  /**
   * List the groups in which the given user is a member.
   *
   * @param userId The user ID
   *
   * @return The groups
   *
   * @throws EISDatabaseException On errors
   */

  EISDatabaseGroupsPagedQueryType<EIGroupMembership>
  groupUserRoles(UUID userId)
    throws EISDatabaseException;
}
