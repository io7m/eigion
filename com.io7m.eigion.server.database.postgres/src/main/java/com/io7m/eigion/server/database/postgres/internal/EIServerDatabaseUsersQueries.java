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

import com.io7m.eigion.server.api.EIPassword;
import com.io7m.eigion.server.api.EIUser;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseUsersQueriesType;
import com.io7m.eigion.server.database.postgres.internal.tables.records.UsersRecord;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.eigion.server.database.postgres.internal.tables.Audit.AUDIT;
import static com.io7m.eigion.server.database.postgres.internal.tables.Users.USERS;

record EIServerDatabaseUsersQueries(
  EIServerDatabaseTransaction transaction)
  implements EIServerDatabaseUsersQueriesType
{
  private static EIUser userRecordToUser(
    final UsersRecord rec)
  {
    return new EIUser(
      rec.getId(),
      rec.getName(),
      rec.getEmail(),
      rec.getCreated(),
      rec.getLastLogin(),
      new EIPassword(
        rec.getPasswordAlgo(),
        rec.getPasswordHash().toUpperCase(Locale.ROOT),
        rec.getPasswordSalt().toUpperCase(Locale.ROOT)
      ),
      rec.getLocked().booleanValue()
    );
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
        .set(USERS.PASSWORD_SALT, password.salt())
        .set(USERS.LOCKED, Boolean.FALSE);

    insert.execute();

    final var audit =
      context.insertInto(AUDIT)
        .set(AUDIT.TIME, created)
        .set(AUDIT.TYPE, "USER_CREATED")
        .set(AUDIT.MESSAGE, id.toString());

    audit.execute();
    return this.userGet(id).orElseThrow();
  }

  @Override
  public Optional<EIUser> userGet(
    final UUID id)
  {
    Objects.requireNonNull(id, "id");

    final var context = this.transaction.createContext();
    return context.fetchOptional(USERS, USERS.ID.eq(id))
      .map(EIServerDatabaseUsersQueries::userRecordToUser);
  }

  @Override
  public Optional<EIUser> userGetForName(
    final String name)
  {
    Objects.requireNonNull(name, "name");

    final var context = this.transaction.createContext();
    return context.fetchOptional(USERS, USERS.NAME.eq(name))
      .map(EIServerDatabaseUsersQueries::userRecordToUser);
  }

  @Override
  public Optional<EIUser> userGetForEmail(
    final String email)
  {
    Objects.requireNonNull(email, "email");

    final var context = this.transaction.createContext();
    return context.fetchOptional(USERS, USERS.EMAIL.eq(email))
      .map(EIServerDatabaseUsersQueries::userRecordToUser);
  }
}
