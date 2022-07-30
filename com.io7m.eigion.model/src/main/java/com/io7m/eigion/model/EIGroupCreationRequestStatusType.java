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

/**
 * The status of a group creation request.
 */

public sealed interface EIGroupCreationRequestStatusType
{
  /**
   * The name of the in-progress state.
   */

  String NAME_IN_PROGRESS = "in-progress";

  /**
   * The name of the succeeded state.
   */

  String NAME_SUCCEEDED = "succeeded";

  /**
   * The name of the failed state.
   */

  String NAME_FAILED = "failed";

  /**
   * @return The name of the status
   */

  String name();

  /**
   * @return The time the request started
   */

  OffsetDateTime timeStarted();

  /**
   * @return The time the request completed
   */

  Optional<OffsetDateTime> timeCompleted();


  /**
   * The request is in progress.
   *
   * @param timeStarted The time the request started
   */

  record InProgress(
    OffsetDateTime timeStarted)
    implements EIGroupCreationRequestStatusType
  {
    /**
     * The request is in progress.
     */

    public InProgress
    {
      Objects.requireNonNull(timeStarted, "timeStarted");
    }

    @Override
    public String name()
    {
      return NAME_IN_PROGRESS;
    }

    @Override
    public Optional<OffsetDateTime> timeCompleted()
    {
      return Optional.empty();
    }
  }


  /**
   * The request succeeded.
   *
   * @param timeStarted        The time the request started
   * @param timeCompletedValue The time the request completed
   */

  record Succeeded(
    OffsetDateTime timeStarted,
    OffsetDateTime timeCompletedValue)
    implements EIGroupCreationRequestStatusType
  {
    /**
     * The request succeeded.
     */

    public Succeeded
    {
      Objects.requireNonNull(timeStarted, "timeStarted");
      Objects.requireNonNull(timeCompletedValue, "timeCompletedValue");
    }

    @Override
    public String name()
    {
      return NAME_SUCCEEDED;
    }

    @Override
    public Optional<OffsetDateTime> timeCompleted()
    {
      return Optional.of(this.timeCompletedValue);
    }
  }

  /**
   * The request failed.
   *
   * @param timeStarted        The time the request started
   * @param timeCompletedValue The time the request completed
   * @param message            The failure message
   */

  record Failed(
    OffsetDateTime timeStarted,
    OffsetDateTime timeCompletedValue,
    String message)
    implements EIGroupCreationRequestStatusType
  {
    /**
     * The request succeeded.
     */

    public Failed
    {
      Objects.requireNonNull(timeStarted, "timeStarted");
      Objects.requireNonNull(timeCompletedValue, "timeCompletedValue");
      Objects.requireNonNull(message, "message");
    }

    @Override
    public String name()
    {
      return NAME_FAILED;
    }

    @Override
    public Optional<OffsetDateTime> timeCompleted()
    {
      return Optional.of(this.timeCompletedValue);
    }
  }
}
