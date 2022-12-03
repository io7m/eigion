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


package com.io7m.eigion.server.service.limits;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.io.input.BoundedInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.Function;

/**
 * Methods to handle request size limits.
 */

public final class EIRequestLimits implements EIRequestLimitsType
{
  private final Function<Long, String> requestTooLargeMessage;

  /**
   * Methods to handle request size limits.
   *
   * @param inRequestTooLargeMessage A function that formats a message
   */

  public EIRequestLimits(
    final Function<Long, String> inRequestTooLargeMessage)
  {
    this.requestTooLargeMessage =
      Objects.requireNonNull(
        inRequestTooLargeMessage, "requestTooLargeMessage");
  }

  @Override
  public InputStream boundedMaximumInput(
    final HttpServletRequest request,
    final int maximum)
    throws IOException, EIRequestLimitExceeded
  {
    final int size;
    final var specifiedLength = request.getContentLength();
    if (specifiedLength == -1) {
      size = maximum;
    } else {
      if (Integer.compareUnsigned(specifiedLength, maximum) > 0) {
        throw new EIRequestLimitExceeded(
          this.requestTooLargeMessage.apply(
            Long.valueOf(Integer.toUnsignedLong(specifiedLength))
          ),
          Integer.toUnsignedLong(maximum),
          Integer.toUnsignedLong(specifiedLength)
        );
      }
      size = specifiedLength;
    }

    final var baseStream = request.getInputStream();
    return new BoundedInputStream(baseStream, Integer.toUnsignedLong(size));
  }

  @Override
  public String description()
  {
    return "Request limiting service.";
  }

  @Override
  public String toString()
  {
    return "[EIRequestLimits 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }
}
