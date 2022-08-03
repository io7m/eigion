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

package com.io7m.eigion.protocol.admin_api.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.io7m.eigion.model.EIGroupInvite;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.model.EIUserDisplayName;
import com.io7m.eigion.protocol.api.EIProtocolFromModel;
import com.io7m.eigion.protocol.api.EIProtocolToModel;

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
 * @param timeStarted          The time the invite was created
 * @param status               The status
 * @param timeCompleted        The time the invite was completed
 */

public record EISA1GroupInvite(
  @JsonProperty(value = "UserInviting", required = true)
  UUID userInviting,
  @JsonProperty(value = "UserInvitingName", required = true)
  String userInvitingName,
  @JsonProperty(value = "UserBeingInvited", required = true)
  UUID userBeingInvited,
  @JsonProperty(value = "UserBeingInvitedName", required = true)
  String userBeingInvitedName,
  @JsonProperty(value = "Group", required = true)
  String group,
  @JsonProperty(value = "Token", required = true)
  String token,
  @JsonProperty(value = "Status", required = true)
  EISA1GroupInviteStatus status,
  @JsonProperty(value = "TimeStarted", required = true)
  OffsetDateTime timeStarted,
  @JsonProperty(value = "TimeCompleted", required = false)
  @JsonInclude(JsonInclude.Include.NON_ABSENT)
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
   * @param timeStarted          The time the invite was created
   * @param status               The status
   * @param timeCompleted        The time the invite was completed
   */

  public EISA1GroupInvite
  {
    Objects.requireNonNull(userInviting, "userInviting");
    Objects.requireNonNull(userInvitingName, "userInvitingName");
    Objects.requireNonNull(userBeingInvited, "userBeingInvited");
    Objects.requireNonNull(userBeingInvitedName, "userBeingInvitedName");
    Objects.requireNonNull(group, "group");
    Objects.requireNonNull(token, "token");
    Objects.requireNonNull(timeStarted, "time");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(timeCompleted, "timeCompleted");
  }

  /**
   * @return This invite as a model invite
   */

  @EIProtocolToModel
  public EIGroupInvite toInvite()
  {
    return new EIGroupInvite(
      this.userInviting,
      new EIUserDisplayName(this.userInvitingName),
      this.userBeingInvited,
      new EIUserDisplayName(this.userBeingInvitedName),
      new EIGroupName(this.group),
      new EIToken(this.token),
      this.status.toStatus(),
      this.timeStarted,
      this.timeCompleted
    );
  }

  /**
   * @param i The model invite
   *
   * @return The model invite as a v1 invite
   */

  @EIProtocolFromModel
  public static EISA1GroupInvite ofInvite(
    final EIGroupInvite i)
  {
    return new EISA1GroupInvite(
      i.userInviting(),
      i.userInvitingName().value(),
      i.userBeingInvited(),
      i.userBeingInvitedName().value(),
      i.group().value(),
      i.token().value(),
      EISA1GroupInviteStatus.ofStatus(i.status()),
      i.timeStarted(),
      i.timeCompleted()
    );
  }
}
