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
import com.io7m.eigion.model.EIAdmin;
import com.io7m.eigion.model.EIAdminPermission;
import com.io7m.eigion.model.EIAdminSummary;
import com.io7m.eigion.model.EIAuditEvent;
import com.io7m.eigion.model.EIGroupInvite;
import com.io7m.eigion.model.EIGroupInviteStatus;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIService;
import com.io7m.eigion.model.EISubsetMatch;
import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.model.EIUserSummary;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The "disconnected" protocol handler.
 */

public final class EIAClientProtocolHandlerDisconnected
  implements EIAClientProtocolHandlerType
{
  private final HttpClient httpClient;
  private final EIAStrings strings;

  /**
   * The "disconnected" protocol handler.
   *
   * @param inStrings    The string resources
   * @param inHttpClient The HTTP client
   */

  public EIAClientProtocolHandlerDisconnected(
    final EIAStrings inStrings,
    final HttpClient inHttpClient)
  {
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.httpClient =
      Objects.requireNonNull(inHttpClient, "httpClient");
  }

  @Override
  public EIAClientProtocolHandlerType login(
    final String user,
    final String password,
    final URI base)
    throws EIAClientException, InterruptedException
  {
    return EIAProtocolNegotiation.negotiateProtocolHandler(
      this.httpClient,
      this.strings,
      user,
      password,
      base
    );
  }

  @Override
  public List<EIService> services()
    throws EIAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public Optional<EIUser> userById(
    final String id)
    throws EIAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public Optional<EIUser> userByName(
    final String name)
    throws EIAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public Optional<EIUser> userByEmail(
    final String email)
    throws EIAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public List<EIUserSummary> userSearch(
    final String query)
    throws EIAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public List<EIAuditEvent> auditGet(
    final OffsetDateTime dateLower,
    final OffsetDateTime dateUpper,
    final EISubsetMatch<String> owner,
    final EISubsetMatch<String> type,
    final EISubsetMatch<String> message)
    throws EIAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public EIUser userCreate(
    final String name,
    final String email,
    final String password)
    throws EIAClientException
  {
    throw this.notLoggedIn();
  }

  private EIAClientException notLoggedIn()
  {
    return new EIAClientException(
      this.strings.format("notLoggedIn")
    );
  }

  @Override
  public Optional<EIAdmin> adminById(
    final String id)
    throws EIAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public Optional<EIAdmin> adminByName(
    final String name)
    throws EIAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public Optional<EIAdmin> adminByEmail(
    final String email)
    throws EIAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public List<EIAdminSummary> adminSearch(
    final String query)
    throws EIAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public EIAdmin adminCreate(
    final String name,
    final String email,
    final String password,
    final Set<EIAdminPermission> permissions)
    throws EIAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public List<EIGroupInvite> groupInvites(
    final OffsetDateTime since,
    final Optional<EIGroupName> withGroupName,
    final Optional<UUID> withUserInviter,
    final Optional<UUID> withUserBeingInvited,
    final Optional<EIGroupInviteStatus> withStatus)
    throws EIAClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public void groupInviteSetStatus(
    final EIToken token,
    final EIGroupInviteStatus status)
    throws EIAClientException
  {
    throw this.notLoggedIn();
  }
}
