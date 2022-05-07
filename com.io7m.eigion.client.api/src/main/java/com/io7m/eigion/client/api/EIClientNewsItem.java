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

package com.io7m.eigion.client.api;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A news item returned by a client.
 *
 * @param date   The news item date
 * @param format The news item format
 * @param text   The news item text
 * @param title  The news item title
 */

public record EIClientNewsItem(
  LocalDateTime date,
  String format,
  String title,
  String text)
{
  /**
   * A news item returned by a client.
   *
   * @param date   The news item date
   * @param format The news item format
   * @param text   The news item text
   * @param title  The news item title
   */

  public EIClientNewsItem
  {
    Objects.requireNonNull(date, "date");
    Objects.requireNonNull(format, "format");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(text, "text");
  }
}
