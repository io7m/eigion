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

package com.io7m.eigion.protocol.public_api.v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.io7m.eigion.model.EIGroupCreationRequest;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.protocol.api.EIProtocolFromModel;
import com.io7m.eigion.protocol.api.EIProtocolToModel;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.Failed;
import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.InProgress;
import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.NAME_FAILED;
import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.NAME_IN_PROGRESS;
import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.NAME_SUCCEEDED;
import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.Succeeded;

/**
 * A request to create a group.
 *
 * @param groupName     The group name
 * @param userId        The request user ID
 * @param token         The request token
 * @param status        The request status
 * @param message       The request message
 * @param timeStarted   The time the request started
 * @param timeCompleted The time the request completed (if applicable)
 */

@JsonDeserialize
@JsonSerialize
public record EISP1GroupCreationRequest(
  String groupName,
  UUID userId,
  String token,
  String status,
  OffsetDateTime timeStarted,
  Optional<OffsetDateTime> timeCompleted,
  String message)
{
  /**
   * A request to create a group.
   *
   * @param groupName     The group name
   * @param userId        The request user ID
   * @param token         The request token
   * @param status        The request status
   * @param message       The request message
   * @param timeStarted   The time the request started
   * @param timeCompleted The time the request completed (if applicable)
   */

  @JsonCreator
  public EISP1GroupCreationRequest
  {
    Objects.requireNonNull(groupName, "groupName");
    Objects.requireNonNull(token, "token");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(message, "message");
    Objects.requireNonNull(timeStarted, "timeStarted");
    Objects.requireNonNull(timeCompleted, "timeCompleted");
  }

  /**
   * @return This request as a model request
   */

  @EIProtocolToModel
  public EIGroupCreationRequest toRequest()
  {
    return new EIGroupCreationRequest(
      new EIGroupName(this.groupName),
      this.userId,
      new EIToken(this.token),
      switch (this.status) {
        case NAME_IN_PROGRESS -> new InProgress(this.timeStarted);
        case NAME_FAILED -> new Failed(
          this.timeStarted,
          this.timeCompleted.orElse(this.timeStarted),
          this.message
        );
        case NAME_SUCCEEDED -> new Succeeded(
          this.timeStarted,
          this.timeCompleted.orElse(this.timeStarted));
        default -> throw new IllegalStateException();
      });
  }

  /**
   * @param request The model request
   *
   * @return The model request as a v1 request
   */

  @EIProtocolFromModel
  public static EISP1GroupCreationRequest ofRequest(
    final EIGroupCreationRequest request)
  {
    final var status = request.status();
    if (status instanceof InProgress inProgress) {
      return new EISP1GroupCreationRequest(
        request.groupName().value(),
        request.userFounder(),
        request.token().value(),
        NAME_IN_PROGRESS,
        inProgress.timeStarted(),
        inProgress.timeCompleted(),
        ""
      );
    }
    if (status instanceof Succeeded succeeded) {
      return new EISP1GroupCreationRequest(
        request.groupName().value(),
        request.userFounder(),
        request.token().value(),
        NAME_SUCCEEDED,
        succeeded.timeStarted(),
        succeeded.timeCompleted(),
        ""
      );
    }
    if (status instanceof Failed failed) {
      return new EISP1GroupCreationRequest(
        request.groupName().value(),
        request.userFounder(),
        request.token().value(),
        NAME_SUCCEEDED,
        failed.timeStarted(),
        failed.timeCompleted(),
        ellipsize(failed.message())
      );
    }

    throw new IllegalStateException();
  }

  private static String ellipsize(
    final String message)
  {
    if (message.length() > 128) {
      return "%s...".formatted(message.substring(127));
    }
    return message;
  }
}
