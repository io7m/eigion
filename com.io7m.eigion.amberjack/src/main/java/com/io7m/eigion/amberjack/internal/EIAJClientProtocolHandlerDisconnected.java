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
import com.io7m.eigion.amberjack.api.EIAJClientPagedType;
import com.io7m.eigion.model.EIAuditEvent;
import com.io7m.eigion.model.EIAuditSearchParameters;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIGroupSearchByNameParameters;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.Locale;
import java.util.Objects;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.NOT_LOGGED_IN;

/**
 * The "disconnected" protocol handler.
 */

public final class EIAJClientProtocolHandlerDisconnected
  implements EIAJClientProtocolHandlerType
{
  private final HttpClient httpClient;
  private final Locale locale;
  private final EIAJStrings strings;

  /**
   * The "disconnected" protocol handler.
   *
   * @param inLocale     The locale
   * @param inStrings    The string resources
   * @param inHttpClient The HTTP client
   */

  public EIAJClientProtocolHandlerDisconnected(
    final Locale inLocale,
    final EIAJStrings inStrings,
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
  public EIAJNewHandler login(
    final String admin,
    final String password,
    final URI base)
    throws EIAJClientException, InterruptedException
  {
    final var handler =
      EIAJProtocolNegotiation.negotiateProtocolHandler(
        this.locale,
        this.httpClient,
        this.strings,
        base
      );

    return handler.login(admin, password, base);
  }

  @Override
  public void groupCreate(
    final EIGroupName name)
    throws EIAJClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public EIAJClientPagedType<EIGroupName> groupSearchByName(
    final EIGroupSearchByNameParameters parameters)
    throws EIAJClientException
  {
    throw this.notLoggedIn();
  }

  private EIAJClientException notLoggedIn()
  {
    return new EIAJClientException(
      NOT_LOGGED_IN,
      this.strings.format("notLoggedIn")
    );
  }

  @Override
  public EIAJClientPagedType<EIAuditEvent> auditSearch(
    final EIAuditSearchParameters parameters)
    throws EIAJClientException
  {
    throw this.notLoggedIn();
  }
}
