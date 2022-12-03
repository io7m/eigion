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


package com.io7m.eigion.server.service.sessions;

import com.io7m.eigion.error_codes.EIErrorCode;
import com.io7m.eigion.error_codes.EIException;

/**
 * The type of session exceptions.
 */

public final class EISessionException
  extends EIException
{
  /**
   * Create an exception.
   *
   * @param message     The message
   * @param inErrorCode The error code
   */

  public EISessionException(
    final String message,
    final EIErrorCode inErrorCode)
  {
    super(message, inErrorCode);
  }

  /**
   * Create an exception.
   *
   * @param message     The message
   * @param cause       The cause
   * @param inErrorCode The error code
   */

  public EISessionException(
    final String message,
    final Throwable cause,
    final EIErrorCode inErrorCode)
  {
    super(message, cause, inErrorCode);
  }

  /**
   * Create an exception.
   *
   * @param cause       The cause
   * @param inErrorCode The error code
   */

  public EISessionException(
    final Throwable cause,
    final EIErrorCode inErrorCode)
  {
    super(cause, inErrorCode);
  }
}
