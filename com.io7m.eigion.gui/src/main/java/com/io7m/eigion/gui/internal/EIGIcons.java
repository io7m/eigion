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


package com.io7m.eigion.gui.internal;

import com.io7m.eigion.gui.EIGIconsConfiguration;
import com.io7m.eigion.services.api.EIServiceType;
import javafx.scene.image.Image;

/**
 * The application icon service.
 */

public final class EIGIcons implements EIServiceType
{
  private final Image error16;
  private final Image error24;
  private final Image news24;
  private final Image task16;

  /**
   * The application icon service.
   *
   * @param iconsConfiguration The icon configuration
   */

  public EIGIcons(
    final EIGIconsConfiguration iconsConfiguration)
  {
    this.error16 =
      new Image(iconsConfiguration.error16().toString(), true);
    this.error24 =
      new Image(iconsConfiguration.error24().toString(), true);
    this.news24 =
      new Image(iconsConfiguration.news24().toString(), true);
    this.task16 =
      new Image(iconsConfiguration.task16().toString(), true);
  }

  /**
   * @return An icon
   */

  public Image error16()
  {
    return this.error16;
  }

  /**
   * @return An icon
   */

  public Image error24()
  {
    return this.error24;
  }

  /**
   * @return An icon
   */

  public Image news24()
  {
    return this.news24;
  }

  /**
   * @return An icon
   */

  public Image task16()
  {
    return this.task16;
  }

  @Override
  public String toString()
  {
    return String.format(
      "[EIGIcons 0x%08x]",
      Integer.valueOf(this.hashCode())
    );
  }
}
