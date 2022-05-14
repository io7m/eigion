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


package com.io7m.eigion.gui.icons.vanilla;

import com.io7m.eigion.gui.icons.EIIconSemantic;
import com.io7m.eigion.gui.icons.EIIconSetType;
import javafx.scene.image.Image;

/**
 * The vanilla icon set.
 */

public final class EIIconsVanilla implements EIIconSetType
{
  /**
   * The vanilla icon set.
   */

  public EIIconsVanilla()
  {

  }

  @Override
  public Image icon(
    final EIIconSemantic semantic)
  {
    return load(
      switch (semantic) {
        case NEWS_24 -> "news24";
        case ERROR_16 -> "error16";
        case ERROR_24 -> "error24";
        case SERVICE_24 -> "service";
        case TASK_16 -> "task16";
      }
    );
  }

  private static Image load(
    final String name)
  {
    return new Image(
      EIIconsVanilla.class.getResource(
        new StringBuilder(64)
          .append("/com/io7m/eigion/icons/vanilla/")
          .append(name)
          .append(".png")
          .toString()
      ).toString(),
      true
    );
  }
}
