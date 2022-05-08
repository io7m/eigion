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
import java.time.Duration;
import java.util.Objects;

/**
 * The logo screen configuration.
 *
 * @param logoURI      The URI of the logo resource
 * @param logoDuration The duration of the logo screen
 * @param logoHeight   The height of the logo image
 * @param logoWidth    The width of the logo image
 */

public record EIGLogoScreenConfiguration(
  URI logoURI,
  Duration logoDuration,
  double logoWidth,
  double logoHeight)
{
  /**
   * The logo screen configuration.
   */

  public EIGLogoScreenConfiguration
  {
    Objects.requireNonNull(logoURI, "logoConfiguration");
    Objects.requireNonNull(logoDuration, "logoDuration");
  }
}
