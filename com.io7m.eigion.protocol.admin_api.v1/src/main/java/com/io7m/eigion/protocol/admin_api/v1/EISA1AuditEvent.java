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

package com.io7m.eigion.protocol.admin_api.v1;

import com.io7m.eigion.model.EIAuditEvent;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * An audit event.
 *
 * @param id      The unique event ID
 * @param owner   The event owner
 * @param time    The event time
 * @param message The event message
 * @param type    The event type
 */

public record EISA1AuditEvent(
  long id,
  UUID owner,
  OffsetDateTime time,
  String type,
  String message)
{
  /**
   * An audit event.
   *
   * @param id      The unique event ID
   * @param owner   The event owner
   * @param time    The event time
   * @param message The event message
   * @param type    The event type
   */

  public EISA1AuditEvent
  {
    Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(time, "time");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(message, "message");
  }

  /**
   * @return This event as a model event
   */

  public EIAuditEvent toAuditEvent()
  {
    return new EIAuditEvent(
      this.id,
      this.owner,
      this.time,
      this.type,
      this.message
    );
  }

  /**
   * @param event The input event
   *
   * @return A v1 event of this event
   */

  public static EISA1AuditEvent ofAuditEvent(
    final EIAuditEvent event)
  {
    return new EISA1AuditEvent(
      event.id(),
      event.owner(),
      event.time(),
      event.type(),
      event.message()
    );
  }
}
