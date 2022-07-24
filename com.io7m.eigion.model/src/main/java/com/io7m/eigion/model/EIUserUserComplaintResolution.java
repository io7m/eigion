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

/**
 * The resolution of a user complaint.
 *
 * @param resolved The resolution status
 * @param time     The resolution time
 * @param reason   The reason for the resolution
 */

public record EIUserUserComplaintResolution(
  EIComplaintResolution resolved,
  OffsetDateTime time,
  String reason)
{
  /**
   * The resolution of a user complaint.
   *
   * @param resolved The resolution status
   * @param time     The resolution time
   * @param reason   The reason for the resolution
   */

  public EIUserUserComplaintResolution
  {
    Objects.requireNonNull(resolved, "resolved");
    Objects.requireNonNull(time, "time");
    Objects.requireNonNull(reason, "reason");

    final var length = reason.length();
    if (length > 4096) {
      throw new EIValidityException(
        "Complaint resolution reason length %d must be <= 4096"
          .formatted(Integer.valueOf(length))
      );
    }
  }
}
