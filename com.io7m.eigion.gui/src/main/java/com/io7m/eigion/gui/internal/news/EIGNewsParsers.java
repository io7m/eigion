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


package com.io7m.eigion.gui.internal.news;

import com.io7m.eigion.news.xml.EINXParser;
import com.io7m.eigion.services.api.EIServiceType;

/**
 * A news parser service.
 */

public final class EIGNewsParsers implements EIServiceType
{
  private final EINXParser newsParser;

  /**
   * A news parser service.
   */

  public EIGNewsParsers()
  {
    this.newsParser = new EINXParser();
  }

  /**
   * @return The XML news parser
   */

  public EINXParser newsParser()
  {
    return this.newsParser;
  }

  @Override
  public String toString()
  {
    return String.format(
      "[EIGNewsParsers 0x%08x]",
      Integer.valueOf(this.hashCode())
    );
  }

  @Override
  public String description()
  {
    return "News data parser service";
  }
}
