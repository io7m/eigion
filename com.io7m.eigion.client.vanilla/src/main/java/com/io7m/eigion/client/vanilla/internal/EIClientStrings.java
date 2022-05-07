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


package com.io7m.eigion.client.vanilla.internal;

import com.io7m.jxtrand.vanilla.JXTAbstractStrings;

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The client string resources.
 */

public final class EIClientStrings extends JXTAbstractStrings
{
  private static final Pattern WHITESPACE =
    Pattern.compile("\\s+");

  /**
   * The client string resources.
   *
   * @param locale The application locale
   *
   * @throws IOException On I/O errors
   */

  public EIClientStrings(
    final Locale locale)
    throws IOException
  {
    super(
      locale,
      EIClientStrings.class,
      "/com/io7m/eigion/client/vanilla/internal",
      "Messages"
    );
  }

  @Override
  public String format(
    final String id,
    final Object... args)
  {
    return WHITESPACE.matcher(super.format(id, args))
      .replaceAll(" ")
      .strip();
  }

  @Override
  public String toString()
  {
    return String.format(
      "[EIClientStrings 0x%08x]",
      Integer.valueOf(this.hashCode())
    );
  }
}
