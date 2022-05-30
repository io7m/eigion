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


package com.io7m.eigion.server.vanilla.internal;

import com.io7m.eigion.server.api.EIServerRequestProcessed;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * A request log implementation that publishes events.
 */

public final class EIServerRequestLog implements RequestLog
{
  private final String name;
  private final EIServerEventBus events;
  private final EIServerClock clock;

  /**
   * A request log implementation that publishes events.
   *
   * @param services The service directory
   * @param inName   The API name
   */

  public EIServerRequestLog(
    final EIServiceDirectoryType services,
    final String inName)
  {
    Objects.requireNonNull(services, "services");

    this.events =
      services.requireService(EIServerEventBus.class);
    this.clock =
      services.requireService(EIServerClock.class);
    this.name =
      Objects.requireNonNull(inName, "name");
  }

  @Override
  public void log(
    final Request request,
    final Response response)
  {
    final var timeThen =
      EIServerRequestDecoration.requestStartTimeFor(request);
    final var timeNow =
      this.clock.nowPrecise();
    final var timeDuration =
      Duration.between(timeThen, timeNow);

    this.events.publish(
      new EIServerRequestProcessed(
        this.clock.nowPrecise(),
        this.name,
        EIServerRequestDecoration.requestIdFor(request),
        request.getRemoteAddr(),
        request.getRemotePort(),
        response.getStatus(),
        timeDuration,
        request.getRequestURI(),
        Optional.empty()
      )
    );
  }
}
