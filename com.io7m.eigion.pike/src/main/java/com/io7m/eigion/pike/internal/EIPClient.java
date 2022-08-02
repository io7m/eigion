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
import com.io7m.eigion.model.EIGroupInvite;
import com.io7m.eigion.model.EIGroupInviteStatus;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIGroupRoles;
import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.model.EIUserDisplayName;
import com.io7m.eigion.pike.api.EIPClientException;
import com.io7m.eigion.pike.api.EIPClientType;
import com.io7m.eigion.pike.api.EIPGroupCreationChallenge;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The default client implementation.
 */

public final class EIPClient implements EIPClientType
{
  private final EIPStrings strings;
  private final HttpClient httpClient;
  private volatile EIPClientProtocolHandlerType handler;

  /**
   * The default client implementation.
   *
   * @param inStrings    The string resources
   * @param inHttpClient The HTTP client
   * @param inHandler    The versioned handler
   */

  public EIPClient(
    final EIPStrings inStrings,
    final HttpClient inHttpClient,
    final EIPClientProtocolHandlerType inHandler)
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
    throws EIPClientException, InterruptedException
  {
    final var newHandler =
      EIPProtocolNegotiation.negotiateProtocolHandler(
        this.httpClient,
        this.strings,
        user,
        password,
        base
      );

    this.handler = newHandler.login(user, password, base);
  }

  @Override
  public EIPGroupCreationChallenge groupCreationBegin(
    final EIGroupName name)
    throws EIPClientException, InterruptedException
  {
    return this.handler.groupCreationBegin(name);
  }

  @Override
  public List<EIGroupCreationRequest> groupCreationRequests()
    throws EIPClientException, InterruptedException
  {
    return this.handler.groupCreationRequests();
  }

  @Override
  public void groupCreationCancel(
    final EIToken token)
    throws EIPClientException, InterruptedException
  {
    this.handler.groupCreationCancel(token);
  }

  @Override
  public void groupCreationReady(
    final EIToken token)
    throws EIPClientException, InterruptedException
  {
    this.handler.groupCreationReady(token);
  }

  @Override
  public List<EIGroupRoles> groups()
    throws EIPClientException, InterruptedException
  {
    return this.handler.groups();
  }

  @Override
  public void groupInvite(
    final EIGroupName group,
    final UUID user)
    throws EIPClientException, InterruptedException
  {
    this.handler.groupInvite(group, user);
  }

  @Override
  public void groupInviteByName(
    final EIGroupName group,
    final EIUserDisplayName user)
    throws EIPClientException, InterruptedException
  {
    this.handler.groupInviteByName(group, user);
  }

  @Override
  public List<EIGroupInvite> groupInvitesSent(
    final OffsetDateTime since,
    final Optional<EIGroupInviteStatus> withStatus)
    throws EIPClientException, InterruptedException
  {
    return this.handler.groupInvitesSent(since, withStatus);
  }

  @Override
  public List<EIGroupInvite> groupInvitesReceived(
    final OffsetDateTime since,
    final Optional<EIGroupInviteStatus> withStatus)
    throws EIPClientException, InterruptedException
  {
    return this.handler.groupInvitesReceived(since, withStatus);
  }

  @Override
  public void groupInviteCancel(
    final EIToken token)
    throws EIPClientException, InterruptedException
  {
    this.handler.groupInviteCancel(token);
  }

  @Override
  public void groupInviteRespond(
    final EIToken token,
    final boolean accept)
    throws EIPClientException, InterruptedException
  {
    this.handler.groupInviteRespond(token, accept);
  }
}
