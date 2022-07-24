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

package com.io7m.eigion.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Information for a single administrator.
 *
 * @param id            The admin's ID
 * @param name          The admin's name
 * @param email         The admin's email
 * @param password      The admin's password
 * @param created       The date the admin was created
 * @param lastLoginTime The date the admin last logged in
 */

public record EIAdmin(
  UUID id,
  EIUserDisplayName name,
  EIUserEmail email,
  OffsetDateTime created,
  OffsetDateTime lastLoginTime,
  EIPassword password)
{
  /**
   * Information for a single administrator.
   *
   * @param id            The admin's ID
   * @param name          The admin's name
   * @param email         The admin's email
   * @param password      The admin's password
   * @param created       The date the admin was created
   * @param lastLoginTime The date the admin last logged in
   */

  public EIAdmin
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(created, "created");
    Objects.requireNonNull(lastLoginTime, "lastLoginTime");
    Objects.requireNonNull(password, "password");
  }
}