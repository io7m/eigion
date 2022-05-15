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

package com.io7m.eigion.server.api;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Information for a single user.
 *
 * @param id        The user's ID
 * @param name      The user's name
 * @param email     The user's email
 * @param password  The user's password
 * @param created   The date the user was created
 * @param lastLogin The date the user last logged in
 * @param ban       The user's ban, if one exists
 */

public record EIUser(
  UUID id,
  String name,
  String email,
  OffsetDateTime created,
  OffsetDateTime lastLogin,
  EIPassword password,
  Optional<EIUserBan> ban)
{
  /**
   * Information for a single user.
   *
   * @param id        The user's ID
   * @param name      The user's name
   * @param email     The user's email
   * @param password  The user's password
   * @param created   The date the user was created
   * @param lastLogin The date the user last logged in
   * @param ban       The user's ban, if one exists
   */

  public EIUser
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(created, "created");
    Objects.requireNonNull(lastLogin, "lastLogin");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(ban, "ban");
  }
}
