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

import com.io7m.eigion.model.EIPassword;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.model.EIUserBan;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseRequiresUser;
import com.io7m.eigion.server.database.api.EIServerDatabaseUsersQueriesType;
import com.io7m.eigion.server.database.postgres.internal.tables.records.UserBansRecord;
import com.io7m.eigion.server.database.postgres.internal.tables.records.UsersRecord;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.eigion.server.database.postgres.internal.EIServerDatabaseExceptions.handleDatabaseException;
import static com.io7m.eigion.server.database.postgres.internal.Tables.USER_BANS;
import static com.io7m.eigion.server.database.postgres.internal.tables.Audit.AUDIT;
import static com.io7m.eigion.server.database.postgres.internal.tables.Users.USERS;

record EIServerDatabaseUsersQueries(
  EIServerDatabaseTransaction transaction)
  implements EIServerDatabaseUsersQueriesType
{
  private static EIUser userRecordToUser(
    final UsersRecord userRecord,
    final Optional<UserBansRecord> banOpt)
  {
    return new EIUser(
      userRecord.getId(),
      userRecord.getName(),
      userRecord.getEmail(),
      userRecord.getCreated(),
      userRecord.getLastLogin(),
      new EIPassword(
        userRecord.getPasswordAlgo(),
        userRecord.getPasswordHash().toUpperCase(Locale.ROOT),
        userRecord.getPasswordSalt().toUpperCase(Locale.ROOT)
      ),
      banOpt.map(userBansRecord -> {
        return new EIUserBan(
          Optional.ofNullable(userBansRecord.getExpires()),
          userBansRecord.getReason()
        );
      })
    );
  }

  private static Optional<EIUser> userMap(
    final DSLContext context,
    final Optional<UsersRecord> recordOpt)
  {
    if (recordOpt.isPresent()) {
      final var userRecord =
        recordOpt.get();
      final var banOpt =
        context.fetchOptional(
          USER_BANS,
          USER_BANS.USER_ID.eq(userRecord.getId()));
      return Optional.of(userRecordToUser(userRecord, banOpt));
    }

    return Optional.empty();
  }

  @Override
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

    final var context =
      this.transaction.createContext();

    try {
      {
        final var existing =
          context.fetchOptional(USERS, USERS.ID.eq(id));
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

      final var insert =
        context.insertInto(USERS)
          .set(USERS.ID, id)
          .set(USERS.NAME, userName)
          .set(USERS.EMAIL, email)
          .set(USERS.CREATED, created)
          .set(USERS.LAST_LOGIN, created)
          .set(USERS.PASSWORD_ALGO, password.algorithm())
          .set(USERS.PASSWORD_HASH, password.hash())
          .set(USERS.PASSWORD_SALT, password.salt());

      insert.execute();

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, this.timeNow())
          .set(AUDIT.TYPE, "USER_CREATED")
          .set(AUDIT.USER_ID, id)
          .set(AUDIT.MESSAGE, id.toString());

      audit.execute();
      return this.userGet(id).orElseThrow();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction, e);
    }
  }

  private OffsetDateTime timeNow()
  {
    return OffsetDateTime.now(this.transaction.clock()).withNano(0);
  }

  @Override
  public Optional<EIUser> userGet(
    final UUID id)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");

    final var context = this.transaction.createContext();
    try {
      return userMap(context, context.fetchOptional(USERS, USERS.ID.eq(id)));
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction, e);
    }
  }

  @Override
  public Optional<EIUser> userGetForName(
    final String name)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(name, "name");

    final var context = this.transaction.createContext();
    try {
      return userMap(
        context,
        context.fetchOptional(USERS, USERS.NAME.eq(name)));
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction, e);
    }
  }

  @Override
  public Optional<EIUser> userGetForEmail(
    final String email)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(email, "email");

    final var context = this.transaction.createContext();
    try {
      return userMap(
        context,
        context.fetchOptional(USERS, USERS.EMAIL.eq(email)));
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction, e);
    }
  }

  @Override
  @EIServerDatabaseRequiresUser
  public void userBan(
    final UUID id,
    final Optional<OffsetDateTime> expires,
    final String reason)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(expires, "expires");
    Objects.requireNonNull(reason, "reason");

    final var owner =
      this.transaction.userId();
    final var context =
      this.transaction.createContext();

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
          .set(AUDIT.TIME, this.timeNow())
          .set(AUDIT.TYPE, "USER_BANNED")
          .set(AUDIT.USER_ID, owner)
          .set(AUDIT.MESSAGE, id + ": " + reason);

      audit.execute();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction, e);
    }
  }

  @Override
  @EIServerDatabaseRequiresUser
  public void userUnban(final UUID id)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");

    final var owner =
      this.transaction.userId();
    final var context =
      this.transaction.createContext();

    try {
      context.deleteFrom(USER_BANS)
        .where(USER_BANS.USER_ID.eq(id))
        .execute();

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, this.timeNow())
          .set(AUDIT.TYPE, "USER_UNBANNED")
          .set(AUDIT.USER_ID, owner)
          .set(AUDIT.MESSAGE, id.toString());

      audit.execute();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction, e);
    }
  }
}
