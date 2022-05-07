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


package com.io7m.eigion.client.vanilla;

import com.io7m.eigion.client.api.EIClientConfiguration;
import com.io7m.eigion.client.api.EIClientFactoryType;
import com.io7m.eigion.client.api.EIClientType;
import com.io7m.eigion.client.vanilla.internal.EIClientStrings;
import com.io7m.eigion.client.vanilla.v1.EIV1Client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.util.Locale;

/**
 * The default client factory.
 */

public final class EIClients implements EIClientFactoryType
{
  private final EIClientStrings strings;

  /**
   * Create a client factory.
   *
   * @param locale The application locale
   */

  public EIClients(
    final Locale locale)
  {
    try {
      this.strings = new EIClientStrings(locale);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Create a client factory.
   */

  public EIClients()
  {
    this(Locale.getDefault());
  }

  @Override
  public EIClientType create(
    final EIClientConfiguration configuration)
  {
    return new EIV1Client(
      this.strings,
      configuration,
      HttpClient.newHttpClient()
    );
  }
}
