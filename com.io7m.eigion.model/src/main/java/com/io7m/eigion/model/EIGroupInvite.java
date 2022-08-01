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

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * An invite to join a group.
 *
 * @param userInviting         The user doing the inviting
 * @param userInvitingName     The user doing the inviting
 * @param userBeingInvited     The user being invited
 * @param userBeingInvitedName The user being invited
 * @param group                The group
 * @param token                The token
 * @param status               The status
 * @param timeStarted          The time the invite was created
 * @param timeCompleted        The time the invite was completed, if available
 */

public record EIGroupInvite(
  UUID userInviting,
  EIUserDisplayName userInvitingName,
  UUID userBeingInvited,
  EIUserDisplayName userBeingInvitedName,
  EIGroupName group,
  EIToken token,
  EIGroupInviteStatus status,
  OffsetDateTime timeStarted,
  Optional<OffsetDateTime> timeCompleted)
{
  /**
   * An invite to join a group.
   *
   * @param userInviting         The user doing the inviting
   * @param userInvitingName     The user doing the inviting
   * @param userBeingInvited     The user being invited
   * @param userBeingInvitedName The user being invited
   * @param group                The group
   * @param token                The token
   * @param status               The status
   * @param timeStarted          The time the invite was created
   * @param timeCompleted        The time the invite was completed, if
   *                             available
   */

  public EIGroupInvite
  {
    Objects.requireNonNull(userInviting, "userInviting");
    Objects.requireNonNull(userInvitingName, "userInvitingName");
    Objects.requireNonNull(userBeingInvited, "userBeingInvited");
    Objects.requireNonNull(userBeingInvitedName, "userBeingInvitedName");
    Objects.requireNonNull(group, "group");
    Objects.requireNonNull(token, "token");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(timeStarted, "timeStarted");
    Objects.requireNonNull(timeCompleted, "timeCompleted");
  }
}
