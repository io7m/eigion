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

package com.io7m.eigion.protocol.admin_api.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.io7m.eigion.model.EIAdmin;
import com.io7m.eigion.model.EIPasswordException;
import com.io7m.eigion.model.EIUserDisplayName;
import com.io7m.eigion.model.EIUserEmail;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Information for a single admin.
 *
 * @param id            The admin's ID
 * @param name          The admin's name
 * @param email         The admin's email
 * @param password      The admin's password
 * @param created       The date the admin was created
 * @param lastLoginTime The date the admin last logged in
 * @param permissions   The admin's permissions
 */

@JsonDeserialize
@JsonSerialize
public record EISA1Admin(
  @JsonProperty(value = "ID", required = true)
  UUID id,
  @JsonProperty(value = "Name", required = true)
  String name,
  @JsonProperty(value = "Email", required = true)
  String email,
  @JsonProperty(value = "Created", required = true)
  OffsetDateTime created,
  @JsonProperty(value = "LastLogin", required = true)
  OffsetDateTime lastLoginTime,
  @JsonProperty(value = "Password", required = true)
  EISA1Password password,
  @JsonProperty(value = "Permissions", required = true)
  Set<EISA1AdminPermission> permissions)
{
  /**
   * Information for a single admin.
   *
   * @param id            The admin's ID
   * @param name          The admin's name
   * @param email         The admin's email
   * @param password      The admin's password
   * @param created       The date the admin was created
   * @param lastLoginTime The date the admin last logged in
   * @param permissions   The admin's permissions
   */

  public EISA1Admin
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(created, "created");
    Objects.requireNonNull(lastLoginTime, "lastLoginTime");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(permissions, "permissions");
  }

  /**
   * Create a v1 admin from the given model admin.
   *
   * @param admin The model admin
   *
   * @return A v1 admin
   *
   * @see #toAdmin()
   */

  public static EISA1Admin ofAdmin(
    final EIAdmin admin)
  {
    Objects.requireNonNull(admin, "admin");
    return new EISA1Admin(
      admin.id(),
      admin.name().value(),
      admin.email().value(),
      admin.created(),
      admin.lastLoginTime(),
      EISA1Password.ofPassword(admin.password()),
      admin.permissions()
        .stream()
        .map(EISA1AdminPermission::ofAdmin)
        .collect(Collectors.toUnmodifiableSet())
    );
  }

  /**
   * Convert this to a model admin.
   *
   * @return This as a model admin
   *
   * @throws EIPasswordException On password errors
   * @see #ofAdmin(EIAdmin)
   */

  public EIAdmin toAdmin()
    throws EIPasswordException
  {
    return new EIAdmin(
      this.id,
      new EIUserDisplayName(this.name),
      new EIUserEmail(this.email),
      this.created,
      this.lastLoginTime,
      this.password.toPassword(),
      this.permissions.stream()
        .map(EISA1AdminPermission::toAdmin)
        .collect(Collectors.toUnmodifiableSet())
    );
  }
}
