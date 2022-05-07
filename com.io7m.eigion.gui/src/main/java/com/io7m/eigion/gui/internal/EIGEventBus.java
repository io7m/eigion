/*
 * Copyright © 2021 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

package com.io7m.eigion.gui.internal;

import com.io7m.eigion.services.api.EIServiceType;

import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

/**
 * The main application event bus.
 */

public final class EIGEventBus implements EIServiceType
{
  private final SubmissionPublisher<EIGEventType> events;

  /**
   * The main application event bus.
   */

  public EIGEventBus()
  {
    this.events = new SubmissionPublisher<>();
  }

  /**
   * Subscribe to the event bus.
   *
   * @param subscriber A subscriber
   */

  public void subscribe(
    final Flow.Subscriber<? super EIGEventType> subscriber)
  {
    this.events.subscribe(subscriber);
  }

  /**
   * Submit a message to the bus.
   *
   * @param event The message
   */

  public void submit(
    final EIGEventType event)
  {
    this.events.submit(Objects.requireNonNull(event, "event"));
  }

  @Override
  public String toString()
  {
    return String.format(
      "[EIGMainEventBus 0x%08x]",
      Integer.valueOf(this.hashCode())
    );
  }
}
