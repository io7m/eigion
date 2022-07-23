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


package com.io7m.eigion.amberjack;

import com.io7m.eigion.amberjack.api.EIAClientException;
import com.io7m.eigion.amberjack.api.EIAClientFactoryType;
import com.io7m.eigion.amberjack.api.EIAClientType;
import com.io7m.eigion.amberjack.internal.EIAClient;
import com.io7m.eigion.amberjack.internal.EIAClientProtocolHandlerDisconnected;
import com.io7m.eigion.amberjack.internal.EIAStrings;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.CookieManager;
import java.net.http.HttpClient;
import java.util.Locale;

/**
 * The default factory of {@code amberjack} clients.
 */

public final class EIAClients implements EIAClientFactoryType
{
  /**
   * The default factory of {@code amberjack} clients.
   */

  public EIAClients()
  {

  }

  @Override
  public EIAClientType create(
    final Locale locale)
    throws EIAClientException, InterruptedException
  {
    final var cookieJar =
      new CookieManager();

    final EIAStrings strings;
    try {
      strings = new EIAStrings(locale);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }

    final var httpClient =
      HttpClient.newBuilder()
        .cookieHandler(cookieJar)
        .build();

    return new EIAClient(
      strings,
      httpClient,
      new EIAClientProtocolHandlerDisconnected(strings, httpClient)
    );
  }
}
