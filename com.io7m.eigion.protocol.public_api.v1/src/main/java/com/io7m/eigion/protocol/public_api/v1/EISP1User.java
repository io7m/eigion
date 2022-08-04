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

package com.io7m.eigion.protocol.public_api.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIGroupRole;
import com.io7m.eigion.model.EIPasswordException;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.model.EIUserDisplayName;
import com.io7m.eigion.model.EIUserEmail;
import com.io7m.eigion.protocol.api.EIProtocolFromModel;
import com.io7m.eigion.protocol.api.EIProtocolToModel;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Information for a single user.
 *
 * @param id              The user's ID
 * @param name            The user's name
 * @param email           The user's email
 * @param password        The user's password
 * @param created         The date the user was created
 * @param lastLoginTime   The date the user last logged in
 * @param ban             The user's ban, if one exists
 * @param groupMembership The user's group membership
 */

@JsonDeserialize
@JsonSerialize
public record EISP1User(
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
  EISP1Password password,
  @JsonInclude(JsonInclude.Include.NON_ABSENT)
  @JsonProperty(value = "Ban", required = false)
  Optional<EISP1UserBan> ban,
  @JsonProperty(value = "GroupMembership", required = true)
  Map<String, Set<EISP1GroupRole>> groupMembership)
{
  /**
   * Information for a single user.
   *
   * @param id              The user's ID
   * @param name            The user's name
   * @param email           The user's email
   * @param password        The user's password
   * @param created         The date the user was created
   * @param lastLoginTime   The date the user last logged in
   * @param ban             The user's ban, if one exists
   * @param groupMembership The user's group membership
   */

  public EISP1User
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(created, "created");
    Objects.requireNonNull(lastLoginTime, "lastLoginTime");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(ban, "ban");
    Objects.requireNonNull(groupMembership, "groupMembership");
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

  @EIProtocolFromModel
  public static EISP1User ofUser(
    final EIUser user)
  {
    Objects.requireNonNull(user, "user");
    return new EISP1User(
      user.id(),
      user.name().value(),
      user.email().value(),
      user.created(),
      user.lastLoginTime(),
      EISP1Password.ofPassword(user.password()),
      user.ban().map(EISP1UserBan::ofBan),
      mapGroupMembership(user.groupMembership())
    );
  }

  private static Map<String, Set<EISP1GroupRole>> mapGroupMembership(
    final Map<EIGroupName, Set<EIGroupRole>> groupMembership)
  {
    return groupMembership.entrySet()
      .stream()
      .map(EISP1User::mapEntry)
      .collect(Collectors.toUnmodifiableMap(
        Map.Entry::getKey,
        Map.Entry::getValue));
  }

  private static Map.Entry<String, Set<EISP1GroupRole>> mapEntry(
    final Map.Entry<EIGroupName, Set<EIGroupRole>> e)
  {
    return Map.entry(e.getKey().value(), mapGroupRoles(e.getValue()));
  }

  private static Set<EISP1GroupRole> mapGroupRoles(
    final Set<EIGroupRole> roles)
  {
    return roles.stream()
      .map(EISP1GroupRole::ofRole)
      .collect(Collectors.toUnmodifiableSet());
  }

  private static Map<EIGroupName, Set<EIGroupRole>> mapGroupMembershipV1(
    final Map<String, Set<EISP1GroupRole>> groupMembership)
  {
    return groupMembership.entrySet()
      .stream()
      .map(EISP1User::mapEntryV1)
      .collect(Collectors.toUnmodifiableMap(
        Map.Entry::getKey,
        Map.Entry::getValue));
  }

  private static Map.Entry<EIGroupName, Set<EIGroupRole>> mapEntryV1(
    final Map.Entry<String, Set<EISP1GroupRole>> e)
  {
    return Map.entry(
      new EIGroupName(e.getKey()),
      mapGroupRolesV1(e.getValue())
    );
  }

  private static Set<EIGroupRole> mapGroupRolesV1(
    final Set<EISP1GroupRole> roles)
  {
    return roles.stream()
      .map(EISP1GroupRole::toRole)
      .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Convert this to a model user.
   *
   * @return This as a model user
   *
   * @throws EIPasswordException On password errors
   * @see #ofUser(EIUser)
   */

  @EIProtocolToModel
  public EIUser toUser()
    throws EIPasswordException
  {
    return new EIUser(
      this.id,
      new EIUserDisplayName(this.name),
      new EIUserEmail(this.email),
      this.created,
      this.lastLoginTime,
      this.password.toPassword(),
      this.ban.map(EISP1UserBan::toBan),
      mapGroupMembershipV1(this.groupMembership)
    );
  }
}
