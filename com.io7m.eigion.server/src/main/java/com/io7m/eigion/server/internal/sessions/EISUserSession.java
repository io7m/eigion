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

package com.io7m.eigion.server.internal.sessions;

import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.server.database.api.EISDatabaseAuditEventsSearchType;
import com.io7m.idstore.user_client.api.IdUClientType;
import com.io7m.jaffirm.core.Preconditions;
import jakarta.servlet.http.HttpSession;

import java.util.Objects;
import java.util.Optional;

/**
 * A controller for a single user session.
 */

public final class EISUserSession
  implements AutoCloseable
{
  private final HttpSession httpSession;
  private final IdUClientType idClient;
  private final EIUser user;
  private Optional<EISDatabaseAuditEventsSearchType> auditSearch;

  /**
   * A controller for a single user session.
   *
   * @param inUser        The user
   * @param inHttpSession The HTTP session
   * @param inIdClient    The ID client
   */

  public EISUserSession(
    final EIUser inUser,
    final HttpSession inHttpSession,
    final IdUClientType inIdClient)
  {
    this.user =
      Objects.requireNonNull(inUser, "inUser");
    this.httpSession =
      Objects.requireNonNull(inHttpSession, "inHttpSession");
    this.idClient =
      Objects.requireNonNull(inIdClient, "inIdClient");
    this.auditSearch =
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
   * Set the current audit search.
   * @param search The search
   */

  public void setAuditSearch(
    final EISDatabaseAuditEventsSearchType search)
  {
    this.auditSearch = Optional.of(search);
  }
}
