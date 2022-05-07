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

package com.io7m.eigion.gui;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * The icon resource configuration.
 *
 * @param error16 A 16x16 image used to indicate errors
 * @param error24 A 24x24 image used to indicate errors
 * @param news24  A 24x24 image used for news items
 * @param task16  A 16x16 image used to indicate task list items
 */

public record EIGIconsConfiguration(
  URI error16,
  URI error24,
  URI news24,
  URI task16)
{
  /**
   * The icon resource configuration.
   *
   * @param error16 A 16x16 image used to indicate errors
   * @param error24 A 24x24 image used to indicate errors
   * @param news24  A 24x24 image used for news items
   * @param task16  A 16x16 image used to indicate task list items
   */

  public EIGIconsConfiguration
  {
    Objects.requireNonNull(error16, "error16");
    Objects.requireNonNull(error24, "error24");
    Objects.requireNonNull(news24, "news24");
    Objects.requireNonNull(task16, "task16");
  }

  /**
   * @return The default icon resources
   */

  public static EIGIconsConfiguration defaults()
  {
    return new EIGIconsConfiguration(
      resourceOf("error16.png"),
      resourceOf("error24.png"),
      resourceOf("news24.png"),
      resourceOf("task16.png")
    );
  }

  private static URI resourceOf(
    final String name)
  {
    try {
      return EIGIconsConfiguration.class.getResource(
        "/com/io7m/eigion/gui/internal/" + name).toURI();
    } catch (final URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }
}
