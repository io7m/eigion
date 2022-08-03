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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A command to retrieve a list of group invites.
 *
 * @param since                Only return invites newer than this time
 * @param withUserBeingInvited Only return invites with this user as the target
 *                             of the invitation
 * @param withUserInviting     Only return invites with this user doing the
 *                             inviting
 * @param withStatus           Only return invites with this status
 * @param withGroup            Only return invites in this group
 */

@JsonDeserialize
@JsonSerialize
public record EISA1CommandGroupInvites(
  @JsonProperty(value = "Since", required = true)
  OffsetDateTime since,
  @JsonProperty(value = "WithUserInviting")
  @JsonInclude(JsonInclude.Include.NON_ABSENT)
  Optional<UUID> withUserInviting,
  @JsonProperty(value = "WithUserBeingInvited")
  @JsonInclude(JsonInclude.Include.NON_ABSENT)
  Optional<UUID> withUserBeingInvited,
  @JsonProperty(value = "WithGroup")
  @JsonInclude(JsonInclude.Include.NON_ABSENT)
  Optional<String> withGroup,
  @JsonProperty(value = "WithStatus")
  @JsonInclude(JsonInclude.Include.NON_ABSENT)
  Optional<EISA1GroupInviteStatus> withStatus)
  implements EISA1CommandType
{
  /**
   * A command to retrieve a list of group invites.
   *
   * @param since                Only return invites newer than this time
   * @param withUserBeingInvited Only return invites with this user as the
   *                             target of the invitation
   * @param withUserInviting     Only return invites with this user doing the
   *                             inviting
   * @param withStatus           Only return invites with this status
   * @param withGroup            Only return invites in this group
   */

  public EISA1CommandGroupInvites
  {
    Objects.requireNonNull(since, "since");
    Objects.requireNonNull(withGroup, "withGroup");
    Objects.requireNonNull(withUserInviting, "withUserInviting");
    Objects.requireNonNull(withUserBeingInvited, "withUserBeingInvited");
    Objects.requireNonNull(withStatus, "withStatus");
  }
}
