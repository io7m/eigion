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

package com.io7m.eigion.server.database.api;

import com.io7m.eigion.model.EIPassword;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.model.EIUserSummary;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The database queries involving users.
 */

public non-sealed interface EIServerDatabaseUsersQueriesType
  extends EIServerDatabaseQueriesType
{
  /**
   * Create a user.
   *
   * @param userName The user name
   * @param email    The user email
   * @param password The hashed password
   *
   * @return The created user
   *
   * @throws EIServerDatabaseException On errors
   */

  @EIServerDatabaseRequiresAdmin
  default EIUser userCreate(
    final String userName,
    final String email,
    final EIPassword password)
    throws EIServerDatabaseException
  {
    return this.userCreate(
      UUID.randomUUID(),
      userName,
      email,
      OffsetDateTime.now(),
      password
    );
  }

  /**
   * Create a user.
   *
   * @param id       The user ID
   * @param created  The creation time
   * @param userName The user name
   * @param email    The user email
   * @param password The hashed password
   *
   * @return The created user
   *
   * @throws EIServerDatabaseException On errors
   */

  @EIServerDatabaseRequiresAdmin
  EIUser userCreate(
    UUID id,
    String userName,
    String email,
    OffsetDateTime created,
    EIPassword password)
    throws EIServerDatabaseException;

  /**
   * @param id The user ID
   *
   * @return A user with the given ID
   *
   * @throws EIServerDatabaseException On errors
   */

  Optional<EIUser> userGet(UUID id)
    throws EIServerDatabaseException;

  /**
   * @param name The user name
   *
   * @return A user with the given name
   *
   * @throws EIServerDatabaseException On errors
   */

  Optional<EIUser> userGetForName(String name)
    throws EIServerDatabaseException;

  /**
   * @param email The user email
   *
   * @return A user with the given email
   *
   * @throws EIServerDatabaseException On errors
   */

  Optional<EIUser> userGetForEmail(String email)
    throws EIServerDatabaseException;

  /**
   * Ban the given user.
   *
   * @param id      The user ID
   * @param expires The expiration date
   * @param reason  The ban reason
   *
   * @throws EIServerDatabaseException On errors
   */

  @EIServerDatabaseRequiresAdmin
  void userBan(
    UUID id,
    Optional<OffsetDateTime> expires,
    String reason)
    throws EIServerDatabaseException;

  /**
   * Unban the given user.
   *
   * @param id The user ID
   *
   * @throws EIServerDatabaseException On errors
   */

  @EIServerDatabaseRequiresAdmin
  void userUnban(
    UUID id)
    throws EIServerDatabaseException;

  /**
   * Record the fact that the given user has logged in.
   *
   * @param id The user ID
   * @param host The host from which the user logged in
   *
   * @throws EIServerDatabaseException On errors
   */

  void userLogin(
    UUID id,
    String host)
    throws EIServerDatabaseException;

  /**
   * Search for users that have an ID, email, or name that contains the
   * given string, case-insensitive.
   *
   * @param query The user query
   *
   * @return A list of matching users
   *
   * @throws EIServerDatabaseException On errors
   */

  List<EIUserSummary> userSearch(String query)
    throws EIServerDatabaseException;
}
