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


package com.io7m.eigion.pike.api;

import com.io7m.eigion.model.EIGroupCreationRequest;
import com.io7m.eigion.model.EIGroupInvite;
import com.io7m.eigion.model.EIGroupInviteStatus;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIGroupRole;
import com.io7m.eigion.model.EIGroupRoles;
import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.model.EIUserDisplayName;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Commands related to groups.
 */

public interface EIPClientGroupsType
{
  /**
   * Start the creation of a group.
   *
   * @param name The name
   *
   * @return The request details
   *
   * @throws EIPClientException   On errors
   * @throws InterruptedException On interruption
   */

  EIPGroupCreationChallenge groupCreationBegin(
    EIGroupName name)
    throws EIPClientException, InterruptedException;

  /**
   * List group creation requests.
   *
   * @return The request details
   *
   * @throws EIPClientException   On errors
   * @throws InterruptedException On interruption
   */

  List<EIGroupCreationRequest> groupCreationRequests()
    throws EIPClientException, InterruptedException;

  /**
   * Cancel a group creation request.
   *
   * @param token The creation token
   *
   * @throws EIPClientException   On errors
   * @throws InterruptedException On interruption
   */

  void groupCreationCancel(EIToken token)
    throws EIPClientException, InterruptedException;

  /**
   * Mark a group creation request as ready.
   *
   * @param token The creation token
   *
   * @throws EIPClientException   On errors
   * @throws InterruptedException On interruption
   */

  void groupCreationReady(EIToken token)
    throws EIPClientException, InterruptedException;

  /**
   * @return The groups in which the current user is a member
   *
   * @throws EIPClientException   On errors
   * @throws InterruptedException On interruption
   */

  List<EIGroupRoles> groups()
    throws EIPClientException, InterruptedException;

  /**
   * Invite a user to the given group.
   *
   * @param group The group
   * @param user  The user
   *
   * @throws EIPClientException   On errors
   * @throws InterruptedException On interruption
   */

  void groupInvite(
    EIGroupName group,
    UUID user)
    throws EIPClientException, InterruptedException;

  /**
   * Invite a user to the given group.
   *
   * @param group The group
   * @param user  The user
   *
   * @throws EIPClientException   On errors
   * @throws InterruptedException On interruption
   */

  void groupInviteByName(
    EIGroupName group,
    EIUserDisplayName user)
    throws EIPClientException, InterruptedException;

  /**
   * @param since      Only list invites newer than this date
   * @param withStatus Only list invites with this status
   *
   * @return The list of invites the current user has sent
   *
   * @throws EIPClientException   On errors
   * @throws InterruptedException On interruption
   */

  List<EIGroupInvite> groupInvitesSent(
    OffsetDateTime since,
    Optional<EIGroupInviteStatus> withStatus)
    throws EIPClientException, InterruptedException;

  /**
   * @param since      Only list invites newer than this date
   * @param withStatus Only list invites with this status
   *
   * @return The list of invites the current user has received
   *
   * @throws EIPClientException   On errors
   * @throws InterruptedException On interruption
   */

  List<EIGroupInvite> groupInvitesReceived(
    OffsetDateTime since,
    Optional<EIGroupInviteStatus> withStatus)
    throws EIPClientException, InterruptedException;

  /**
   * Cancel the given invite.
   *
   * @param token The invite token
   *
   * @throws EIPClientException   On errors
   * @throws InterruptedException On interruption
   */

  void groupInviteCancel(EIToken token)
    throws EIPClientException, InterruptedException;

  /**
   * Respond to the given invite.
   *
   * @param token  The invite token
   * @param accept {@code true} if the invite is to be accepted
   *
   * @throws EIPClientException   On errors
   * @throws InterruptedException On interruption
   */

  void groupInviteRespond(
    EIToken token,
    boolean accept)
    throws EIPClientException, InterruptedException;

  /**
   * Leave a group.
   *
   * @param group The group name
   *
   * @throws EIPClientException   On errors
   * @throws InterruptedException On interruption
   */

  void groupLeave(EIGroupName group)
    throws EIPClientException, InterruptedException;

  /**
   * Grant a role to a user within the given group.
   *
   * @param group         The group name
   * @param userReceiving The user that will receive the role
   * @param role          The role
   *
   * @throws EIPClientException   On errors
   * @throws InterruptedException On interruption
   */

  void groupGrant(
    EIGroupName group,
    UUID userReceiving,
    EIGroupRole role)
    throws EIPClientException, InterruptedException;
}
