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

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;

/**
 * The factory of version 1 protocol handlers.
 */

public final class EIAClientProtocolHandlers1
  implements EIAClientProtocolHandlerFactoryType
{
  /**
   * The factory of version 1 protocol handlers.
   */

  public EIAClientProtocolHandlers1()
  {

  }

  @Override
  public String id()
  {
    return "com.io7m.eigion.admin";
  }

  @Override
  public BigInteger versionMajor()
  {
    return BigInteger.ONE;
  }

  @Override
  public EIAClientProtocolHandlerType createHandler(
    final HttpClient inHttpClient,
    final EIAStrings inStrings,
    final URI inBase)
  {
    return new EIAClientProtocolHandler1(inHttpClient, inStrings, inBase);
  }
}
