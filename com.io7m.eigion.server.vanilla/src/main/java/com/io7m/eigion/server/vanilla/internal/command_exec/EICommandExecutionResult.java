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

package com.io7m.eigion.server.vanilla.internal.command_exec;

import com.io7m.eigion.protocol.api.EIProtocolMessageType;

import java.util.Objects;

/**
 * The result of executing a command.
 *
 * @param httpStatus The HTTP status code
 * @param response   The response
 * @param <R>        The type of response values
 */

public record EICommandExecutionResult<R extends EIProtocolMessageType>(
  int httpStatus,
  R response)
{
  /**
   * The result of executing a public command.
   *
   * @param httpStatus The HTTP status code
   * @param response   The response
   */

  public EICommandExecutionResult
  {
    Objects.requireNonNull(response, "response");
  }

  /**
   * @return {@code true} if the HTTP status implies failure
   */

  public boolean isFailure()
  {
    return this.httpStatus >= 400;
  }

  /**
   * Soften the type of the result.
   *
   * @param result The result
   * @param <A>    The type of the output
   * @param <B>    The type of the input
   *
   * @return The same result with a wider type bound
   */

  public static <A extends EIProtocolMessageType, B extends A> EICommandExecutionResult<A> soften(
    final EICommandExecutionResult<B> result)
  {
    return new EICommandExecutionResult<>(result.httpStatus, result.response);
  }
}
