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


package com.io7m.eigion.amberjack.api;

import com.io7m.eigion.model.EIAuditEvent;
import com.io7m.eigion.model.EISubsetMatch;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Commands related to audit events.
 */

public interface EIAClientAuditType
{
  /**
   * Retrieve audit events within the given time range.
   *
   * @param dateLower The lower date bound
   * @param dateUpper The upper date bound
   * @param message   The subset of messages to include
   * @param type      The subset of types to include
   * @param owner     The subset of owners to include
   *
   * @return The audit events
   *
   * @throws EIAClientException   On errors
   * @throws InterruptedException On interruption
   */

  List<EIAuditEvent> auditGet(
    OffsetDateTime dateLower,
    OffsetDateTime dateUpper,
    EISubsetMatch<String> owner,
    EISubsetMatch<String> type,
    EISubsetMatch<String> message)
    throws EIAClientException, InterruptedException;
}
