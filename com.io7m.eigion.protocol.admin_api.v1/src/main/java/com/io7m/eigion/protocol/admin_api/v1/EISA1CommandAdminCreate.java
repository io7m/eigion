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

import java.util.Objects;
import java.util.Set;

/**
 * A command to create an admin.
 *
 * @param name        The admin name
 * @param email       The admin email
 * @param password    The admin password
 * @param permissions The permissions the created admin will have
 */

@JsonDeserialize
@JsonSerialize
public record EISA1CommandAdminCreate(
  @JsonProperty(value = "Name", required = true)
  String name,
  @JsonProperty(value = "Email", required = true)
  String email,
  @JsonProperty(value = "Password", required = true)
  EISA1Password password,
  @JsonProperty(value = "Permissions", required = true)
  Set<EISA1AdminPermission> permissions)
  implements EISA1CommandType
{
  /**
   * A command to create a user.
   *
   * @param name        The user name
   * @param email       The user email
   * @param password    The user password
   * @param permissions The permissions the created admin will have
   */

  public EISA1CommandAdminCreate
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(permissions, "permissions");
  }
}