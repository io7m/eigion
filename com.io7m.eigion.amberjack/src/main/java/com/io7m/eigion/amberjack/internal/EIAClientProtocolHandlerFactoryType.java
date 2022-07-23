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
 * A factory of versioned protocol handlers.
 */

public interface EIAClientProtocolHandlerFactoryType
{
  /**
   * @return The supported protocol ID
   */

  String id();

  /**
   * @return The supported protocol major version
   */

  BigInteger versionMajor();

  /**
   * Create a new handler.
   *
   * @param inHttpClient The underlying HTTP client
   * @param inStrings    The string resources
   * @param inBase       The server base URI
   *
   * @return A new handler
   */

  EIAClientProtocolHandlerType createHandler(
    HttpClient inHttpClient,
    EIAStrings inStrings,
    URI inBase);
}
