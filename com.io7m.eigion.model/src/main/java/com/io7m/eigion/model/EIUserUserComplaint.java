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
import java.util.UUID;

/**
 * A complaint made about a {@code userTarget} by {@code userComplainer}.
 *
 * @param userComplainer The complaining user
 * @param userTarget     The target user
 * @param created        The creation time
 * @param reason         The complaint reason
 */

public record EIUserUserComplaint(
  UUID userComplainer,
  UUID userTarget,
  OffsetDateTime created,
  String reason)
{
  /**
   * A complaint made about a {@code userTarget} by {@code userComplainer}.
   *
   * @param userComplainer The complaining user
   * @param userTarget     The target user
   * @param created        The creation time
   * @param reason         The complaint reason
   */

  public EIUserUserComplaint
  {
    Objects.requireNonNull(userComplainer, "userComplainer");
    Objects.requireNonNull(userTarget, "userTarget");
    Objects.requireNonNull(created, "created");
    Objects.requireNonNull(reason, "reason");

    final var length = reason.length();
    if (length > 4096) {
      throw new EIValidityException(
        "Complaint reason length %d must be <= 4096"
          .formatted(Integer.valueOf(length))
      );
    }
  }
}