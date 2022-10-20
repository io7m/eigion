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

package com.io7m.eigion.pike.internal;

import com.io7m.eigion.pike.api.EIPClientException;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.Locale;
import java.util.Objects;

/**
 * The "disconnected" protocol handler.
 */

public final class EIPClientProtocolHandlerDisconnected
  implements EIPClientProtocolHandlerType
{
  private final HttpClient httpClient;
  private final Locale locale;
  private final EIPStrings strings;

  /**
   * The "disconnected" protocol handler.
   *
   * @param inLocale     The locale
   * @param inStrings    The string resources
   * @param inHttpClient The HTTP client
   */

  public EIPClientProtocolHandlerDisconnected(
    final Locale inLocale,
    final EIPStrings inStrings,
    final HttpClient inHttpClient)
  {
    this.locale =
      Objects.requireNonNull(inLocale, "locale");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.httpClient =
      Objects.requireNonNull(inHttpClient, "httpClient");
  }

  @Override
  public EIPNewHandler login(
    final String admin,
    final String password,
    final URI base)
    throws EIPClientException, InterruptedException
  {
    final var handler =
      EIPProtocolNegotiation.negotiateProtocolHandler(
        this.locale,
        this.httpClient,
        this.strings,
        base
      );

    return handler.login(admin, password, base);
  }
}
