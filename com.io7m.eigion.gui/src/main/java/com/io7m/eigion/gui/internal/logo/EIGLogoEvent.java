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


package com.io7m.eigion.gui.internal.logo;

import com.io7m.eigion.gui.internal.EIGEventType;

/**
 * Events relating to the logo screen.
 */

public enum EIGLogoEvent implements EIGEventType
{
  /**
   * The logo screen wants to open.
   */

  LOGO_SCREEN_WANT_OPEN,

  /**
   * The logo screen wants to close.
   */

  LOGO_SCREEN_WANT_CLOSE,

  /**
   * The logo screen wants to close immediately
   */

  LOGO_SCREEN_WANT_CLOSE_IMMEDIATE
}
