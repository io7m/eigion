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

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A request was processed on the server.
 *
 * @param time              The timestamp
 * @param api               The API
 * @param requestId         The request ID
 * @param remoteHost        The remote host
 * @param remotePort        The remote port
 * @param status            The final HTTP status
 * @param requestDuration   The time it took to service the request
 * @param requestLine       The original request line
 * @param authenticatedUser The authenticated user, if any
 */

public record EIServerRequestProcessed(
  OffsetDateTime time,
  String api,
  UUID requestId,
  String remoteHost,
  int remotePort,
  int status,
  Duration requestDuration,
  String requestLine,
  Optional<UUID> authenticatedUser)
  implements EIServerEventType
{
  /**
   * A request was processed on the server.
   *
   * @param time              The timestamp
   * @param api               The API
   * @param requestId         The request ID
   * @param remoteHost        The remote host
   * @param remotePort        The remote port
   * @param status            The final HTTP status
   * @param requestDuration   The time it took to service the request
   * @param requestLine       The original request line
   * @param authenticatedUser The authenticated user, if any
   */

  public EIServerRequestProcessed
  {
    Objects.requireNonNull(time, "time");
    Objects.requireNonNull(api, "api");
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(remoteHost, "remoteHost");
    Objects.requireNonNull(requestDuration, "requestDuration");
    Objects.requireNonNull(requestLine, "requestLine");
    Objects.requireNonNull(authenticatedUser, "authenticatedUser");
  }
}
