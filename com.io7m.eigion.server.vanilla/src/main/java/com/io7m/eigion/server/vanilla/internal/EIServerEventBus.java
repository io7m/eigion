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

import com.io7m.eigion.server.api.EIServerEventType;
import com.io7m.eigion.services.api.EIServiceType;

import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

/**
 * A service that exposes an event bus.
 */

public final class EIServerEventBus
  implements EIServiceType, AutoCloseable
{
  private final SubmissionPublisher<EIServerEventType> events;

  /**
   * A service that exposes an event bus.
   */

  public EIServerEventBus()
  {
    this.events = new SubmissionPublisher<>();
  }

  /**
   * Publish an event to the bus.
   *
   * @param event The event
   */

  public void publish(
    final EIServerEventType event)
  {
    this.events.submit(Objects.requireNonNull(event, "event"));
  }

  @Override
  public String description()
  {
    return "Server event bus.";
  }

  @Override
  public void close()
  {
    this.events.close();
  }

  /**
   * @return The event publisher
   */

  public Flow.Publisher<EIServerEventType> publisher()
  {
    return this.events;
  }

  @Override
  public String toString()
  {
    return "[EIServerEventBus 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode()));
  }
}
