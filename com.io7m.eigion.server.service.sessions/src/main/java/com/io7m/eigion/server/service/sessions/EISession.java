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

package com.io7m.eigion.server.service.sessions;

import com.io7m.eigion.model.EIGroupCreationRequest;
import com.io7m.eigion.model.EIGroupMembership;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.server.database.api.EISDatabaseAuditEventsSearchType;
import com.io7m.eigion.server.database.api.EISDatabaseGroupsPagedQueryType;
import com.io7m.eigion.server.database.api.EISDatabaseGroupsQueriesType;
import com.io7m.eigion.server.database.api.EISDatabasePagedQueryType;
import com.io7m.idstore.user_client.api.IdUClientType;
import com.io7m.jaffirm.core.Preconditions;

import java.util.Objects;
import java.util.Optional;

/**
 * A controller for a single user session.
 */

public final class EISession implements EISessionType
{
  private final EISessionSecretIdentifier identifier;
  private final IdUClientType idClient;
  private final EIUser user;
  private Optional<EISDatabaseAuditEventsSearchType> auditSearch;
  private Optional<EISDatabasePagedQueryType<EISDatabaseGroupsQueriesType, EIGroupName>> groupSearchByName;
  private Optional<EISDatabasePagedQueryType<EISDatabaseGroupsQueriesType, EIGroupMembership>> groupRolesOwnSearch;
  private Optional<EISDatabaseGroupsPagedQueryType<EIGroupCreationRequest>> groupCreationRequestsSearch;

  /**
   * A controller for a single user session.
   *
   * @param inUser       The user
   * @param inIdentifier The session identifier
   * @param inIdClient   The ID client
   */

  public EISession(
    final EIUser inUser,
    final EISessionSecretIdentifier inIdentifier,
    final IdUClientType inIdClient)
  {
    this.user =
      Objects.requireNonNull(inUser, "inUser");
    this.identifier =
      Objects.requireNonNull(inIdentifier, "identifier");
    this.idClient =
      Objects.requireNonNull(inIdClient, "inIdClient");
    this.auditSearch =
      Optional.empty();
    this.groupSearchByName =
      Optional.empty();
    this.groupRolesOwnSearch =
      Optional.empty();
    this.groupCreationRequestsSearch =
      Optional.empty();
  }

  @Override
  public void close()
    throws Exception
  {
    this.idClient.close();
  }

  /**
   * @return The user
   */

  public EIUser user()
  {
    return this.user;
  }

  /**
   * Update the user.
   *
   * @param inUser The user
   */

  public void setUser(
    final EIUser inUser)
  {
    Preconditions.checkPreconditionV(
      Objects.equals(this.user.id(), inUser.id()),
      "Session user ID must match."
    );
  }

  /**
   * @return The current audit search
   */

  public Optional<EISDatabaseAuditEventsSearchType> auditSearch()
  {
    return this.auditSearch;
  }

  /**
   * @return The current group by name search
   */

  public Optional<EISDatabasePagedQueryType<EISDatabaseGroupsQueriesType, EIGroupName>> groupSearchByName()
  {
    return this.groupSearchByName;
  }

  /**
   * Set the current audit search.
   *
   * @param search The search
   */

  public void setAuditSearch(
    final EISDatabaseAuditEventsSearchType search)
  {
    this.auditSearch = Optional.of(search);
  }

  /**
   * Set the current group by name search.
   *
   * @param search The search
   */

  public void setGroupSearchByName(
    final EISDatabasePagedQueryType<EISDatabaseGroupsQueriesType, EIGroupName> search)
  {
    this.groupSearchByName = Optional.of(search);
  }

  /**
   * Set the current group roles search.
   *
   * @param search The search
   */

  public void setGroupRolesOwnSearch(
    final EISDatabasePagedQueryType<EISDatabaseGroupsQueriesType, EIGroupMembership> search)
  {
    this.groupRolesOwnSearch = Optional.of(search);
  }

  /**
   * @return The current group roles search.
   */

  public Optional<EISDatabasePagedQueryType<EISDatabaseGroupsQueriesType, EIGroupMembership>> groupRolesOwnSearch()
  {
    return this.groupRolesOwnSearch;
  }

  /**
   * Set the current group creation requests search.
   *
   * @param search The search
   */

  public void setGroupCreationRequestsSearch(
    final EISDatabaseGroupsPagedQueryType<EIGroupCreationRequest> search)
  {
    this.groupCreationRequestsSearch = Optional.of(search);
  }

  /**
   * @return The current group creation requests search.
   */

  public Optional<EISDatabaseGroupsPagedQueryType<EIGroupCreationRequest>> groupCreationRequestsSearch()
  {
    return this.groupCreationRequestsSearch;
  }

  @Override
  public EISessionSecretIdentifier id()
  {
    return this.identifier;
  }
}
