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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.io7m.eigion.model.EIPassword;
import com.io7m.eigion.model.EIPasswordException;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.model.EIUserBan;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Information for a single user.
 *
 * @param id            The user's ID
 * @param name          The user's name
 * @param email         The user's email
 * @param password      The user's password
 * @param created       The date the user was created
 * @param lastLoginTime The date the user last logged in
 * @param ban           The user's ban, if one exists
 */

@JsonDeserialize
@JsonSerialize
public record EISA1User(
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
  @JsonInclude(JsonInclude.Include.NON_ABSENT)
  @JsonProperty(value = "Ban", required = false)
  Optional<EISA1UserBan> ban)
{
  /**
   * Information for a single user.
   *
   * @param id            The user's ID
   * @param name          The user's name
   * @param email         The user's email
   * @param password      The user's password
   * @param created       The date the user was created
   * @param lastLoginTime The date the user last logged in
   * @param ban           The user's ban, if one exists
   */

  public EISA1User
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(created, "created");
    Objects.requireNonNull(lastLoginTime, "lastLoginTime");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(ban, "ban");
  }

  /**
   * Create a v1 user from the given model user.
   *
   * @param user The model user
   *
   * @return A v1 user
   *
   * @see #toUser()
   */

  public static EISA1User ofUser(
    final EIUser user)
  {
    Objects.requireNonNull(user, "user");
    return new EISA1User(
      user.id(),
      user.name(),
      user.email(),
      user.created(),
      user.lastLoginTime(),
      mapPassword(user.password()),
      user.ban().map(EISA1User::mapBan)
    );
  }

  private static EISA1UserBan mapBan(
    final EIUserBan b)
  {
    return new EISA1UserBan(
      b.expires(),
      b.reason()
    );
  }

  private static EISA1Password mapPassword(
    final EIPassword password)
  {
    return new EISA1Password(
      password.algorithm().identifier(),
      password.hash(),
      password.salt()
    );
  }

  /**
   * Conver this to a model user.
   *
   * @return This as a model user
   *
   * @throws EIPasswordException On password errors
   * @see #ofUser(EIUser)
   */

  public EIUser toUser()
    throws EIPasswordException
  {
    return new EIUser(
      this.id,
      this.name,
      this.email,
      this.created,
      this.lastLoginTime,
      this.password.toPassword(),
      this.ban.map(EISA1UserBan::toBan)
    );
  }
}
