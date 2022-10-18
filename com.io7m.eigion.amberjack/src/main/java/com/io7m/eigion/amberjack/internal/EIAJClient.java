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

package com.io7m.eigion.amberjack.internal;

import com.io7m.eigion.amberjack.api.EIAJClientException;
import com.io7m.eigion.amberjack.api.EIAJClientType;
import com.io7m.eigion.model.EIUser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Locale;
import java.util.Objects;

/**
 * The default client implementation.
 */

public final class EIAJClient implements EIAJClientType
{
  private final EIAJStrings strings;
  private final HttpClient httpClient;
  private final Locale locale;
  private volatile EIAJClientProtocolHandlerType handler;

  /**
   * The default client implementation.
   *
   * @param inLocale     The locale
   * @param inStrings    The string resources
   * @param inHttpClient The HTTP client
   * @param inHandler    The versioned handler
   */

  public EIAJClient(
    final Locale inLocale,
    final EIAJStrings inStrings,
    final HttpClient inHttpClient,
    final EIAJClientProtocolHandlerType inHandler)
  {
    this.locale =
      Objects.requireNonNull(inLocale, "locale");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.httpClient =
      Objects.requireNonNull(inHttpClient, "httpClient");
    this.handler =
      Objects.requireNonNull(inHandler, "handler");
  }

  @Override
  public void close()
    throws IOException
  {

  }

  @Override
  public EIUser login(
    final String admin,
    final String password,
    final URI base)
    throws EIAJClientException, InterruptedException
  {
    final var newHandler =
      EIAJProtocolNegotiation.negotiateProtocolHandler(
        this.locale,
        this.httpClient,
        this.strings,
        base
      );

    final var result = newHandler.login(admin, password, base);
    this.handler = result.handler();
    return result.userLoggedIn();
  }

  @Override
  public String toString()
  {
    return String.format(
      "[EIAJClient 0x%s",
      Integer.toUnsignedString(this.hashCode())
    );
  }

}
