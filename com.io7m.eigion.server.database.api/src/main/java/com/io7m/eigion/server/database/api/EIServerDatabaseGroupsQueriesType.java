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
import com.io7m.eigion.model.EIGroupInvite;
import com.io7m.eigion.model.EIGroupInviteStatus;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIGroupRole;
import com.io7m.eigion.model.EIGroupRoles;
import com.io7m.eigion.model.EIToken;

import java.time.OffsetDateTime;
import java.util.List;
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

  void groupCreate(
    EIGroupName name,
    UUID userFounder)
    throws EIServerDatabaseException;

  /**
   * Start a group creation request.
   *
   * @param request The request
   *
   * @throws EIServerDatabaseException On errors
   */

  void groupCreationRequestStart(
    EIGroupCreationRequest request)
    throws EIServerDatabaseException;

  /**
   * @param userId The user ID
   *
   * @return A history of the group creation requests for the given user.
   *
   * @throws EIServerDatabaseException On errors
   */

  List<EIGroupCreationRequest> groupCreationRequestsForUser(
    UUID userId)
    throws EIServerDatabaseException;

  /**
   * A group creation request can become obsolete if a group is created before
   * the request is completed. This method returns all the group requests that
   * are obsolete for any reason.
   *
   * @return All the obsolete group creation requests
   *
   * @throws EIServerDatabaseException On errors
   */

  List<EIGroupCreationRequest> groupCreationRequestsObsolete()
    throws EIServerDatabaseException;

  /**
   * @return The group creation requests that refer to nonexistent groups, and
   * that have not yet completed
   *
   * @throws EIServerDatabaseException On errors
   */

  List<EIGroupCreationRequest> groupCreationRequestsActive()
    throws EIServerDatabaseException;

  /**
   * @param token The token
   *
   * @return The request associated with the given token, if any
   *
   * @throws EIServerDatabaseException On errors
   */

  Optional<EIGroupCreationRequest> groupCreationRequest(
    EIToken token)
    throws EIServerDatabaseException;

  /**
   * Finish a group creation request. This checks the supplied request and then,
   * on success, acts as {@link #groupCreate(EIGroupName, UUID)}.
   *
   * @param request The request
   *
   * @throws EIServerDatabaseException On errors
   */

  void groupCreationRequestComplete(
    EIGroupCreationRequest request)
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

  void groupMembershipSet(
    EIGroupName name,
    UUID userId,
    Set<EIGroupRole> roles)
    throws EIServerDatabaseException;

  /**
   * List the groups of which the given user is a member.
   *
   * @param userId The user
   *
   * @return The groups of which the given user is a member
   *
   * @throws EIServerDatabaseException On errors
   */

  List<EIGroupRoles> groupMembershipGet(
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

  /**
   * Invite the given user into the given group.
   *
   * @param group  The group
   * @param userId The user to be invited
   *
   * @return The new invite
   *
   * @throws EIServerDatabaseException On errors
   */

  @EIServerDatabaseRequiresUser
  EIGroupInvite groupInvite(
    EIGroupName group,
    UUID userId)
    throws EIServerDatabaseException;

  /**
   * Find the invite with the given token, if one exists.
   *
   * @param token The token
   *
   * @return The invite
   *
   * @throws EIServerDatabaseException On errors
   */

  Optional<EIGroupInvite> groupInviteGet(
    EIToken token)
    throws EIServerDatabaseException;

  /**
   * Get the list of invites created by the given user.
   *
   * @param since      The time lower bound
   * @param withStatus The required status value
   *
   * @return the list of invites
   *
   * @throws EIServerDatabaseException On errors
   */

  @EIServerDatabaseRequiresUser
  List<EIGroupInvite> groupInvitesCreatedByUser(
    OffsetDateTime since,
    Optional<EIGroupInviteStatus> withStatus)
    throws EIServerDatabaseException;

  /**
   * Get the list of invites received by the given user.
   *
   * @param since      The time lower bound
   * @param withStatus The required status value
   *
   * @return the list of invites
   *
   * @throws EIServerDatabaseException On errors
   */

  @EIServerDatabaseRequiresUser
  List<EIGroupInvite> groupInvitesReceivedByUser(
    OffsetDateTime since,
    Optional<EIGroupInviteStatus> withStatus)
    throws EIServerDatabaseException;

  /**
   * Get the list of invites matching the given parameters.
   *
   * @param since                The time lower bound
   * @param withStatus           The required status value
   * @param withGroupName        The group name in the invite
   * @param withUserBeingInvited The user being invited
   * @param withUserInviter      The user doing the inviting
   *
   * @return the list of invites
   *
   * @throws EIServerDatabaseException On errors
   */

  List<EIGroupInvite> groupInvites(
    OffsetDateTime since,
    Optional<EIGroupName> withGroupName,
    Optional<UUID> withUserInviter,
    Optional<UUID> withUserBeingInvited,
    Optional<EIGroupInviteStatus> withStatus)
    throws EIServerDatabaseException;

  /**
   * Set the status of the given invite.
   *
   * @param token  The invite token
   * @param status The status
   *
   * @throws EIServerDatabaseException On errors
   */

  @EIServerDatabaseRequiresUser
  void groupInviteSetStatus(
    EIToken token,
    EIGroupInviteStatus status)
    throws EIServerDatabaseException;

}
