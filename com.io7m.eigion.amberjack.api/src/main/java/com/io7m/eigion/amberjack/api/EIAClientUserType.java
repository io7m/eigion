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

package com.io7m.eigion.amberjack.api;

import com.io7m.eigion.model.EIPassword;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.model.EIUserDisplayName;
import com.io7m.eigion.model.EIUserEmail;
import com.io7m.eigion.model.EIUserSummary;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Commands related to users.
 */

public interface EIAClientUserType
{
  /**
   * Retrieve the user with the given ID.
   *
   * @param id The user
   *
   * @return A user, if one exists
   *
   * @throws EIAClientException   On errors
   * @throws InterruptedException On interruption
   */

  Optional<EIUser> userById(String id)
    throws EIAClientException, InterruptedException;

  /**
   * Retrieve the user with the given name.
   *
   * @param name The user
   *
   * @return A user, if one exists
   *
   * @throws EIAClientException   On errors
   * @throws InterruptedException On interruption
   */

  Optional<EIUser> userByName(String name)
    throws EIAClientException, InterruptedException;

  /**
   * Retrieve the user with the given email.
   *
   * @param email The user email
   *
   * @return A user, if one exists
   *
   * @throws EIAClientException   On errors
   * @throws InterruptedException On interruption
   */

  Optional<EIUser> userByEmail(String email)
    throws EIAClientException, InterruptedException;

  /**
   * Search users.
   *
   * @param query The search query
   *
   * @return The matching users
   *
   * @throws EIAClientException   On errors
   * @throws InterruptedException On interruption
   */

  List<EIUserSummary> userSearch(String query)
    throws EIAClientException, InterruptedException;

  /**
   * Create a new user.
   *
   * @param name     The user name
   * @param email    The user email
   * @param password The password
   *
   * @return The created user
   *
   * @throws EIAClientException   On errors
   * @throws InterruptedException On interruption
   */

  EIUser userCreate(
    String name,
    String email,
    String password)
    throws EIAClientException, InterruptedException;

  /**
   * Update a user.
   *
   * @param id           The user ID
   * @param withName     The new user name
   * @param withEmail    The new user email
   * @param withPassword The new password
   *
   * @return The updated user
   *
   * @throws EIAClientException   On errors
   * @throws InterruptedException On interruption
   */

  EIUser userUpdate(
    UUID id,
    Optional<EIUserDisplayName> withName,
    Optional<EIUserEmail> withEmail,
    Optional<EIPassword> withPassword)
    throws EIAClientException, InterruptedException;

  /**
   * Ban a user.
   *
   * @param id      The user ID
   * @param expires The expiration date, if any
   * @param reason  The reason
   *
   * @return The updated user
   *
   * @throws EIAClientException   On errors
   * @throws InterruptedException On interruption
   */

  EIUser userBan(
    UUID id,
    Optional<OffsetDateTime> expires,
    String reason)
    throws EIAClientException, InterruptedException;

  /**
   * Unban a user.
   *
   * @param id The user ID
   *
   * @return The updated user
   *
   * @throws EIAClientException   On errors
   * @throws InterruptedException On interruption
   */

  EIUser userUnban(
    UUID id)
    throws EIAClientException, InterruptedException;
}
