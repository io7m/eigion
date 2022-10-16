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

package com.io7m.eigion.server.database.api;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static java.time.ZoneOffset.UTC;

/**
 * The base type of query interfaces.
 */

public sealed interface EIDatabaseQueriesType
  permits
  EIDatabaseAuditQueriesType,
  EIDatabaseMaintenanceQueriesType
{
  /**
   * The earliest possible time considered by the server
   */

  OffsetDateTime EARLIEST =
    LocalDateTime.ofEpochSecond(0L, 0, UTC)
      .atOffset(UTC);

  /**
   * @return The earliest possible time considered by the server
   */

  static OffsetDateTime earliest()
  {
    return EARLIEST;
  }
}
