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


package com.io7m.eigion.error_codes;

import java.util.Objects;

/**
 * The base type of exceptions.
 */

public class EIException extends Exception
{
  /**
   * @return The error code
   */

  public final EIErrorCode errorCode()
  {
    return this.errorCode;
  }

  private final EIErrorCode errorCode;

  /**
   * Create an exception.
   *
   * @param message     The message
   * @param inErrorCode The error code
   */

  public EIException(
    final String message,
    final EIErrorCode inErrorCode)
  {
    super(Objects.requireNonNull(message, "message"));
    this.errorCode =
      Objects.requireNonNull(inErrorCode, "inErrorCode");
  }

  /**
   * Create an exception.
   *
   * @param message     The message
   * @param cause       The cause
   * @param inErrorCode The error code
   */

  public EIException(
    final String message,
    final Throwable cause,
    final EIErrorCode inErrorCode)
  {
    super(
      Objects.requireNonNull(message, "message"),
      Objects.requireNonNull(cause, "cause")
    );

    this.errorCode =
      Objects.requireNonNull(inErrorCode, "inErrorCode");
  }

  /**
   * Create an exception.
   *
   * @param cause       The cause
   * @param inErrorCode The error code
   */

  public EIException(
    final Throwable cause,
    final EIErrorCode inErrorCode)
  {
    super(Objects.requireNonNull(cause, "cause"));
    this.errorCode =
      Objects.requireNonNull(inErrorCode, "inErrorCode");
  }
}
