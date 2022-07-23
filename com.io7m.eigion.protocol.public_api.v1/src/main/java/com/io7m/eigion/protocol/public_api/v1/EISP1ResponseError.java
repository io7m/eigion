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

package com.io7m.eigion.protocol.public_api.v1;

import java.util.Objects;
import java.util.UUID;

/**
 * A command failed.
 *
 * @param requestId The server-assigned request ID
 * @param errorCode The error code
 * @param message   The error message
 */

public record EISP1ResponseError(
  UUID requestId,
  String errorCode,
  String message)
  implements EISP1ResponseType
{
  /**
   * A command failed.
   *
   * @param requestId The server-assigned request ID
   * @param errorCode The error code
   * @param message   The error message
   */

  public EISP1ResponseError
  {
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(errorCode, "errorCode");
    Objects.requireNonNull(message, "message");
  }
}
