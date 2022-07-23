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

import com.io7m.eigion.amberjack.api.EIAClientException;
import com.io7m.eigion.amberjack.api.EIAClientType;
import com.io7m.eigion.model.EIAuditEvent;
import com.io7m.eigion.model.EIService;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.model.EIUserSummary;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The default client implementation.
 */

public final class EIAClient implements EIAClientType
{
  private final EIAStrings strings;
  private final HttpClient httpClient;
  private volatile EIAClientProtocolHandlerType handler;

  /**
   * The default client implementation.
   *
   * @param inStrings    The string resources
   * @param inHttpClient The HTTP client
   * @param inHandler    The versioned handler
   */

  public EIAClient(
    final EIAStrings inStrings,
    final HttpClient inHttpClient,
    final EIAClientProtocolHandlerType inHandler)
  {
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
  public void login(
    final String user,
    final String password,
    final URI base)
    throws EIAClientException, InterruptedException
  {
    final var newHandler =
      EIAProtocolNegotiation.negotiateProtocolHandler(
        this.httpClient,
        this.strings,
        user,
        password,
        base
      );

    this.handler = newHandler.login(user, password, base);
  }

  @Override
  public List<EIService> services()
    throws EIAClientException, InterruptedException
  {
    return this.handler.services();
  }

  @Override
  public Optional<EIUser> userById(
    final String id)
    throws EIAClientException, InterruptedException
  {
    return this.handler.userById(id);
  }

  @Override
  public Optional<EIUser> userByName(
    final String name)
    throws EIAClientException, InterruptedException
  {
    return this.handler.userByName(name);
  }

  @Override
  public Optional<EIUser> userByEmail(
    final String email)
    throws EIAClientException, InterruptedException
  {
    return this.handler.userByEmail(email);
  }

  @Override
  public List<EIUserSummary> userSearch(
    final String query)
    throws EIAClientException, InterruptedException
  {
    return this.handler.userSearch(query);
  }

  @Override
  public List<EIAuditEvent> auditGet(
    final OffsetDateTime dateLower,
    final OffsetDateTime dateUpper)
    throws EIAClientException, InterruptedException
  {
    return this.handler.auditGet(dateLower, dateUpper);
  }

  @Override
  public EIUser userCreate(
    final String name,
    final String email,
    final String password)
    throws EIAClientException, InterruptedException
  {
    return this.handler.userCreate(name, email, password);
  }
}
