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

package com.io7m.eigion.server.api;

import java.util.Objects;
import java.util.Set;

/**
 * Configuration for the parts of the server that serve over HTTP.
 *
 * @param amberjackService The amberjack API service
 * @param pikeService      The pike API service
 */

public record EIServerHTTPConfiguration(
  EIServerHTTPServiceConfiguration amberjackService,
  EIServerHTTPServiceConfiguration pikeService)
{
  /**
   * Configuration for the parts of the server that serve over HTTP.
   *
   * @param amberjackService The amberjack API service
   * @param pikeService      The pike API service
   */

  public EIServerHTTPConfiguration
  {
    Objects.requireNonNull(amberjackService, "adminAPIService");
    Objects.requireNonNull(pikeService, "userAPIService");

    try {
      Set.of(
        Integer.valueOf(amberjackService.listenPort()),
        Integer.valueOf(pikeService.listenPort())
      );
    } catch (final IllegalArgumentException e) {
      throw new IllegalArgumentException(
        "All HTTP services must be configured to listen on different ports: %s"
          .formatted(e.getMessage()),
        e
      );
    }
  }
}
