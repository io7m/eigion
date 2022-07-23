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

package com.io7m.eigion.server.vanilla.internal.admin_api;

import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseType;

import java.util.Objects;

/**
 * The result of executing an admin command.
 *
 * @param httpStatus The HTTP status code
 * @param response   The response
 */

public record EIACommandExecutionResult(
  int httpStatus,
  EISA1ResponseType response)
{
  /**
   * The result of executing an admin command.
   *
   * @param httpStatus The HTTP status code
   * @param response   The response
   */

  public EIACommandExecutionResult
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
}
