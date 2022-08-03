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


package com.io7m.eigion.amberjack.api;

import com.io7m.eigion.model.EIGroupInvite;
import com.io7m.eigion.model.EIGroupInviteStatus;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIToken;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Commands related to groups.
 */

public interface EIAClientGroupsType
{
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
   * @throws EIAClientException   On errors
   * @throws InterruptedException On interruption
   */

  List<EIGroupInvite> groupInvites(
    OffsetDateTime since,
    Optional<EIGroupName> withGroupName,
    Optional<UUID> withUserInviter,
    Optional<UUID> withUserBeingInvited,
    Optional<EIGroupInviteStatus> withStatus)
    throws EIAClientException, InterruptedException;

  /**
   * Set the status of a group invite.
   *
   * @param token  The invite token
   * @param status The invite status
   *
   * @throws EIAClientException   On errors
   * @throws InterruptedException On interruption
   */

  void groupInviteSetStatus(
    EIToken token,
    EIGroupInviteStatus status)
    throws EIAClientException, InterruptedException;

}
