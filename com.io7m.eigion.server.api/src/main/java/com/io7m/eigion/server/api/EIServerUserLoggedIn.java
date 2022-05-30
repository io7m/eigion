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

package com.io7m.eigion.server.api;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * A user successfully logged in.
 *
 * @param time      The event timestamp
 * @param requestId The login request ID
 * @param userId    The user ID
 * @param userName  The user name
 * @param host      The user's remote host
 */

public record EIServerUserLoggedIn(
  OffsetDateTime time,
  UUID requestId,
  UUID userId,
  String userName,
  String host)
  implements EIServerEventType
{
  /**
   * A user successfully logged in.
   *
   * @param time      The event timestamp
   * @param requestId The login request ID
   * @param userId    The user ID
   * @param userName  The user name
   * @param host      The user's remote host
   */

  public EIServerUserLoggedIn
  {
    Objects.requireNonNull(time, "time");
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(userName, "userName");
    Objects.requireNonNull(host, "host");
  }
}
