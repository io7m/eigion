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

import com.io7m.eigion.protocol.pike.cb.EIPCB1Messages;
import com.io7m.genevan.core.GenProtocolIdentifier;
import com.io7m.genevan.core.GenProtocolVersion;

import java.net.URI;
import java.net.http.HttpClient;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

/**
 * The factory of version 1 protocol handlers.
 */

public final class EIPClientProtocolHandlers1
  implements EIPClientProtocolHandlerFactoryType
{
  /**
   * The factory of version 1 protocol handlers.
   */

  public EIPClientProtocolHandlers1()
  {

  }

  @Override
  public EIPClientProtocolHandlerType createHandler(
    final HttpClient inHttpClient,
    final EIPStrings inStrings,
    final URI inBase)
  {
    return new EIPClientProtocolHandler1(inHttpClient, inStrings, inBase);
  }

  @Override
  public GenProtocolIdentifier supported()
  {
    return new GenProtocolIdentifier(
      EIPCB1Messages.protocolId().toString(),
      new GenProtocolVersion(ONE, ZERO)
    );
  }
}
