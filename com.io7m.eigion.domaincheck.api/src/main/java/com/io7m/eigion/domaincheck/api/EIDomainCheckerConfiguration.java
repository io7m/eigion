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

package com.io7m.eigion.domaincheck.api;

import java.net.http.HttpClient;
import java.time.Clock;
import java.util.Objects;

/**
 * The domain checker configuration.
 *
 * @param clock      A clock used for time-based operations
 * @param httpClient The HTTP client used for requests
 */

public record EIDomainCheckerConfiguration(
  Clock clock,
  HttpClient httpClient)
{
  /**
   * The domain checker configuration.
   *
   * @param clock      A clock used for time-based operations
   * @param httpClient The HTTP client used for requests
   */

  public EIDomainCheckerConfiguration
  {
    Objects.requireNonNull(clock, "clock");
    Objects.requireNonNull(httpClient, "client");
  }
}
