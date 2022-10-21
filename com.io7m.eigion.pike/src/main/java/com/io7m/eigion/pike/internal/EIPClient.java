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

import com.io7m.eigion.model.EIGroupCreationChallenge;
import com.io7m.eigion.model.EIGroupCreationRequest;
import com.io7m.eigion.model.EIGroupMembership;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.pike.api.EIPClientException;
import com.io7m.eigion.pike.api.EIPClientPagedType;
import com.io7m.eigion.pike.api.EIPClientType;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Locale;
import java.util.Objects;

/**
 * The default client implementation.
 */

public final class EIPClient implements EIPClientType
{
  private final EIPStrings strings;
  private final HttpClient httpClient;
  private final Locale locale;
  private volatile EIPClientProtocolHandlerType handler;

  /**
   * The default client implementation.
   *
   * @param inLocale     The locale
   * @param inStrings    The string resources
   * @param inHttpClient The HTTP client
   * @param inHandler    The versioned handler
   */

  public EIPClient(
    final Locale inLocale,
    final EIPStrings inStrings,
    final HttpClient inHttpClient,
    final EIPClientProtocolHandlerType inHandler)
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
    throws EIPClientException, InterruptedException
  {
    final var newHandler =
      EIPProtocolNegotiation.negotiateProtocolHandler(
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
      "[EIPClient 0x%s",
      Integer.toUnsignedString(this.hashCode())
    );
  }

  @Override
  public EIGroupCreationChallenge groupCreateBegin(
    final EIGroupName groupName)
    throws EIPClientException, InterruptedException
  {
    return this.handler.groupCreateBegin(groupName);
  }

  @Override
  public void groupCreateReady(
    final EIToken token)
    throws EIPClientException, InterruptedException
  {
    this.handler.groupCreateReady(token);
  }

  @Override
  public void groupCreateCancel(
    final EIToken token)
    throws EIPClientException, InterruptedException
  {
    this.handler.groupCreateCancel(token);
  }

  @Override
  public EIPClientPagedType<EIGroupMembership> groups()
    throws EIPClientException, InterruptedException
  {
    return this.handler.groups();
  }

  @Override
  public EIPClientPagedType<EIGroupCreationRequest> groupCreateRequests()
    throws EIPClientException, InterruptedException
  {
    return this.handler.groupCreateRequests();
  }
}
