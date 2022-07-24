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


package com.io7m.eigion.server.database.postgres.internal;

import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIGroupRole;
import com.io7m.eigion.model.EIPassword;
import com.io7m.eigion.model.EIPasswordAlgorithms;
import com.io7m.eigion.model.EIPasswordException;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.model.EIUserBan;
import com.io7m.eigion.model.EIUserDisplayName;
import com.io7m.eigion.model.EIUserEmail;
import com.io7m.eigion.model.EIUserSummary;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseRequiresAdmin;
import com.io7m.eigion.server.database.api.EIServerDatabaseUsersQueriesType;
import com.io7m.eigion.server.database.postgres.internal.tables.records.UserBansRecord;
import com.io7m.eigion.server.database.postgres.internal.tables.records.UsersRecord;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.io7m.eigion.server.database.postgres.internal.EIServerDatabaseExceptions.handleDatabaseException;
import static com.io7m.eigion.server.database.postgres.internal.Tables.GROUPS;
import static com.io7m.eigion.server.database.postgres.internal.Tables.GROUP_USERS;
import static com.io7m.eigion.server.database.postgres.internal.Tables.USER_BANS;
import static com.io7m.eigion.server.database.postgres.internal.Tables.USER_IDS;
import static com.io7m.eigion.server.database.postgres.internal.tables.Audit.AUDIT;
import static com.io7m.eigion.server.database.postgres.internal.tables.Users.USERS;

final class EIServerDatabaseUsersQueries
  extends EIBaseQueries
  implements EIServerDatabaseUsersQueriesType
{
  EIServerDatabaseUsersQueries(
    final EIServerDatabaseTransaction inTransaction)
  {
    super(inTransaction);
  }

  private static EIUser userRecordToUser(
    final UsersRecord userRecord,
    final Optional<UserBansRecord> banOpt,
    final Map<EIGroupName, Set<EIGroupRole>> memberships)
    throws EIPasswordException
  {
    return new EIUser(
      userRecord.getId(),
      new EIUserDisplayName(userRecord.getName()),
      new EIUserEmail(userRecord.getEmail()),
      userRecord.getCreated(),
      userRecord.getLastLoginTime(),
      new EIPassword(
        EIPasswordAlgorithms.parse(userRecord.getPasswordAlgo()),
        userRecord.getPasswordHash().toUpperCase(Locale.ROOT),
        userRecord.getPasswordSalt().toUpperCase(Locale.ROOT)
      ),
      banOpt.map(userBansRecord -> {
        return new EIUserBan(
          Optional.ofNullable(userBansRecord.getExpires()),
          userBansRecord.getReason()
        );
      }),
      memberships
    );
  }

  private static Optional<EIUser> userMap(
    final DSLContext context,
    final Optional<UsersRecord> recordOpt,
    final Map<EIGroupName, Set<EIGroupRole>> memberships)
    throws EIPasswordException
  {
    if (recordOpt.isPresent()) {
      final var userRecord =
        recordOpt.get();
      final var banOpt =
        context.fetchOptional(
          USER_BANS,
          USER_BANS.USER_ID.eq(userRecord.getId()));
      return Optional.of(userRecordToUser(userRecord, banOpt, memberships));
    }

    return Optional.empty();
  }

  private static EIServerDatabaseException handlePasswordException(
    final EIPasswordException exception)
  {
    return new EIServerDatabaseException(
      exception.getMessage(),
      exception,
      "password-error"
    );
  }

  private static Map<EIGroupName, Set<EIGroupRole>> userGetGroupMemberships(
    final DSLContext context,
    final UUID id)
  {
    final var groupMemberships =
      context.select()
        .from(GROUP_USERS)
        .join(GROUPS)
        .on(GROUPS.ID.eq(GROUP_USERS.GROUP_ID))
        .where(GROUP_USERS.USER_ID.eq(id))
        .fetch();

    final var memberships = new HashMap<EIGroupName, Set<EIGroupRole>>();
    for (final var membership : groupMemberships) {
      final var name = new EIGroupName(membership.get(GROUPS.NAME));
      final var roles =
        Arrays.stream(membership.get(GROUP_USERS.ROLES).split(","))
          .filter(s -> !s.isBlank())
          .map(EIGroupRole::valueOf)
          .collect(Collectors.toUnmodifiableSet());
      memberships.put(name, roles);
    }
    return Map.copyOf(memberships);
  }

  @Override
  @EIServerDatabaseRequiresAdmin
  public EIUser userCreate(
    final UUID id,
    final String userName,
    final String email,
    final OffsetDateTime created,
    final EIPassword password)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(userName, "userName");
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(created, "created");
    Objects.requireNonNull(password, "password");

    final var transaction = this.transaction();
    final var context = transaction.createContext();
    final var admin = transaction.adminId();

    try {
      {
        final var existing =
          context.fetchOptional(USER_IDS, USER_IDS.ID.eq(id));
        if (existing.isPresent()) {
          throw new EIServerDatabaseException(
            "User ID already exists",
            "user-duplicate-id"
          );
        }
      }

      {
        final var existing =
          context.fetchOptional(USERS, USERS.NAME.eq(userName));
        if (existing.isPresent()) {
          throw new EIServerDatabaseException(
            "User name already exists",
            "user-duplicate-name"
          );
        }
      }

      {
        final var existing =
          context.fetchOptional(USERS, USERS.EMAIL.eq(email));
        if (existing.isPresent()) {
          throw new EIServerDatabaseException(
            "Email already exists",
            "user-duplicate-email"
          );
        }
      }

      final var idCreate =
        context.insertInto(USER_IDS)
          .set(USER_IDS.ID, id);

      idCreate.execute();

      final var userCreate =
        context.insertInto(USERS)
          .set(USERS.ID, id)
          .set(USERS.NAME, userName)
          .set(USERS.EMAIL, email)
          .set(USERS.CREATED, created)
          .set(USERS.LAST_LOGIN_TIME, created)
          .set(USERS.PASSWORD_ALGO, password.algorithm().identifier())
          .set(USERS.PASSWORD_HASH, password.hash())
          .set(USERS.PASSWORD_SALT, password.salt());

      userCreate.execute();

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, this.currentTime())
          .set(AUDIT.TYPE, "USER_CREATED")
          .set(AUDIT.USER_ID, admin)
          .set(AUDIT.MESSAGE, id.toString());

      audit.execute();
      return this.userGet(id).orElseThrow();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(transaction, e);
    }
  }

  @Override
  public Optional<EIUser> userGet(
    final UUID id)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");

    final var context = this.transaction().createContext();
    try {
      final var record =
        context.fetchOptional(USERS, USERS.ID.eq(id));
      final var memberships =
        record.map(r -> userGetGroupMemberships(context, r.getId()))
          .orElse(Map.of());

      return userMap(context, record, memberships);
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    } catch (final EIPasswordException e) {
      throw handlePasswordException(e);
    }
  }

  @Override
  public Optional<EIUser> userGetForName(
    final String name)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(name, "name");

    final var context = this.transaction().createContext();
    try {
      final var record =
        context.fetchOptional(USERS, USERS.NAME.eq(name));
      final var memberships =
        record.map(r -> userGetGroupMemberships(context, r.getId()))
          .orElse(Map.of());

      return userMap(context, record, memberships);
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    } catch (final EIPasswordException e) {
      throw handlePasswordException(e);
    }
  }

  @Override
  public Optional<EIUser> userGetForEmail(
    final String email)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(email, "email");

    final var context = this.transaction().createContext();
    try {
      final var record =
        context.fetchOptional(USERS, USERS.EMAIL.eq(email));
      final var memberships =
        record.map(r -> userGetGroupMemberships(context, r.getId()))
          .orElse(Map.of());

      return userMap(context, record, memberships);
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    } catch (final EIPasswordException e) {
      throw handlePasswordException(e);
    }
  }

  @Override
  @EIServerDatabaseRequiresAdmin
  public void userBan(
    final UUID id,
    final Optional<OffsetDateTime> expires,
    final String reason)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(expires, "expires");
    Objects.requireNonNull(reason, "reason");

    final var transaction = this.transaction();
    final var context = transaction.createContext();
    final var owner = transaction.adminId();

    try {
      final var existingBanOpt =
        context.fetchOptional(USER_BANS, USER_BANS.USER_ID.eq(id));

      if (existingBanOpt.isPresent()) {
        final var existingBan = existingBanOpt.get();
        existingBan.setExpires(expires.orElse(null));
        existingBan.setReason(reason);
        existingBan.update();
      } else {
        context.insertInto(USER_BANS)
          .set(USER_BANS.USER_ID, id)
          .set(USER_BANS.EXPIRES, expires.orElse(null))
          .set(USER_BANS.REASON, reason)
          .execute();
      }

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, this.currentTime())
          .set(AUDIT.TYPE, "USER_BANNED")
          .set(AUDIT.USER_ID, owner)
          .set(AUDIT.MESSAGE, id + ": " + reason);

      audit.execute();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    }
  }

  @Override
  @EIServerDatabaseRequiresAdmin
  public void userUnban(final UUID id)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");

    final var transaction = this.transaction();
    final var context = transaction.createContext();
    final var owner = transaction.adminId();

    try {
      context.deleteFrom(USER_BANS)
        .where(USER_BANS.USER_ID.eq(id))
        .execute();

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, this.currentTime())
          .set(AUDIT.TYPE, "USER_UNBANNED")
          .set(AUDIT.USER_ID, owner)
          .set(AUDIT.MESSAGE, id.toString());

      audit.execute();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    }
  }

  @Override
  public void userLogin(
    final UUID id,
    final String host)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(host, "host");

    final var context =
      this.transaction().createContext();

    try {
      final var time = this.currentTime();

      final var existingOpt =
        context.fetchOptional(USERS, USERS.ID.eq(id));
      if (!existingOpt.isPresent()) {
        throw new EIServerDatabaseException(
          "User does not exist",
          "user-nonexistent"
        );
      }

      final var existing = existingOpt.get();
      existing.setLastLoginTime(time);
      existing.store();

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, time)
          .set(AUDIT.TYPE, "USER_LOGGED_IN")
          .set(AUDIT.USER_ID, id)
          .set(AUDIT.MESSAGE, host);

      audit.execute();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    }
  }

  @Override
  public List<EIUserSummary> userSearch(
    final String query)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(query, "query");

    final var context =
      this.transaction().createContext();

    try {
      final var wildcardQuery =
        "%%%s%%".formatted(query);

      final var records =
        context.selectFrom(USERS)
          .where(USERS.NAME.likeIgnoreCase(wildcardQuery))
          .or(USERS.EMAIL.likeIgnoreCase(wildcardQuery))
          .or(USERS.ID.likeIgnoreCase(wildcardQuery))
          .orderBy(USERS.NAME)
          .fetch();

      final var summaries = new ArrayList<EIUserSummary>(records.size());
      for (final var record : records) {
        summaries.add(
          new EIUserSummary(
            record.get(USERS.ID),
            new EIUserDisplayName(record.get(USERS.NAME)),
            new EIUserEmail(record.get(USERS.EMAIL))
          )
        );
      }
      return List.copyOf(summaries);
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    }
  }
}
