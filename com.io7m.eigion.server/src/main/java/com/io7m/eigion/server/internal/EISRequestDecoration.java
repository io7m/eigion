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


package com.io7m.eigion.server.internal;

import com.io7m.eigion.services.api.EIServiceDirectoryType;
import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * A request listener that appends a probably-unique ID to every request.
 */

public final class EISRequestDecoration implements HttpChannel.Listener
{
  private final EISClock clock;

  /**
   * A request listener that appends a probably-unique ID to every request.
   *
   * @param services A service directory
   */

  public EISRequestDecoration(
    final EIServiceDirectoryType services)
  {
    Objects.requireNonNull(services, "services");

    this.clock = services.requireService(EISClock.class);
  }

  /**
   * @return The attribute name used to hold a unique ID
   */

  public static String uniqueRequestIDKey()
  {
    return "com.io7m.eigion.server.requestId";
  }

  /**
   * @return The attribute name used to hold a start time
   */

  public static String uniqueRequestTimeKey()
  {
    return "com.io7m.eigion.server.requestTimeStart";
  }

  /**
   * Get the unique ID for the given request.
   *
   * @param request The request
   *
   * @return The unique ID
   */

  public static UUID requestIdFor(
    final HttpServletRequest request)
  {
    final var id = (UUID) request.getAttribute(uniqueRequestIDKey());
    if (id == null) {
      throw new IllegalStateException("Missing request ID");
    }
    return id;
  }

  /**
   * Get the unique ID for the given request.
   *
   * @param request The request
   *
   * @return The unique ID
   */

  public static OffsetDateTime requestStartTimeFor(
    final HttpServletRequest request)
  {
    final var time = (OffsetDateTime) request.getAttribute(uniqueRequestTimeKey());
    if (time == null) {
      throw new IllegalStateException("Missing request start time");
    }
    return time;
  }

  @Override
  public void onRequestBegin(
    final Request request)
  {
    request.setAttribute(uniqueRequestIDKey(), UUID.randomUUID());
    request.setAttribute(uniqueRequestTimeKey(), this.clock.nowPrecise());
  }
}
