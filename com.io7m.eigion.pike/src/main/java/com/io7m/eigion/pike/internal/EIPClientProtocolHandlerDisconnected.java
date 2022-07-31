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

import com.io7m.eigion.model.EIGroupCreationRequest;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.pike.api.EIPClientException;
import com.io7m.eigion.pike.api.EIPGroupCreationChallenge;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Objects;

/**
 * The "disconnected" protocol handler.
 */

public final class EIPClientProtocolHandlerDisconnected
  implements EIPClientProtocolHandlerType
{
  private final HttpClient httpClient;
  private final EIPStrings strings;

  /**
   * The "disconnected" protocol handler.
   *
   * @param inStrings    The string resources
   * @param inHttpClient The HTTP client
   */

  public EIPClientProtocolHandlerDisconnected(
    final EIPStrings inStrings,
    final HttpClient inHttpClient)
  {
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.httpClient =
      Objects.requireNonNull(inHttpClient, "httpClient");
  }

  @Override
  public EIPClientProtocolHandlerType login(
    final String user,
    final String password,
    final URI base)
    throws EIPClientException, InterruptedException
  {
    return EIPProtocolNegotiation.negotiateProtocolHandler(
      this.httpClient,
      this.strings,
      user,
      password,
      base
    );
  }

  @Override
  public EIPGroupCreationChallenge groupCreationBegin(
    final EIGroupName name)
    throws EIPClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public List<EIGroupCreationRequest> groupCreationRequests()
    throws EIPClientException, InterruptedException
  {
    throw this.notLoggedIn();
  }


  private EIPClientException notLoggedIn()
  {
    return new EIPClientException(
      this.strings.format("notLoggedIn")
    );
  }

}