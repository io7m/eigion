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

import com.io7m.eigion.server.api.EIPassword;
import com.io7m.eigion.server.api.EIUser;

import java.time.OffsetDateTime;
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
   */

  Optional<EIUser> userGet(UUID id);

  /**
   * @param name The user name
   *
   * @return A user with the given name
   */

  Optional<EIUser> userGetForName(String name);

  /**
   * @param email The user email
   *
   * @return A user with the given email
   */

  Optional<EIUser> userGetForEmail(String email);
}