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

import com.io7m.eigion.model.EIPermission;
import com.io7m.eigion.model.EIPermissionSet;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.model.EIUserLogin;
import com.io7m.eigion.server.database.api.EISDatabaseException;
import com.io7m.eigion.server.database.api.EISDatabaseUsersQueriesType;
import org.jooq.exception.DataAccessException;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.USER_NONEXISTENT;
import static com.io7m.eigion.server.database.postgres.internal.EISDatabaseExceptions.DEFAULT_HANDLER;
import static com.io7m.eigion.server.database.postgres.internal.EISDatabaseExceptions.handleDatabaseException;
import static com.io7m.eigion.server.database.postgres.internal.Tables.AUDIT;
import static com.io7m.eigion.server.database.postgres.internal.Tables.USERS;

final class EISDatabaseUsersQueries
  extends EISBaseQueries
  implements EISDatabaseUsersQueriesType
{
  static final Supplier<EISDatabaseException> USER_DOES_NOT_EXIST = () -> {
    return new EISDatabaseException(
      "User does not exist",
      USER_NONEXISTENT
    );
  };

  EISDatabaseUsersQueries(
    final EISDatabaseTransaction inTransaction)
  {
    super(inTransaction);
  }

  @Override
  public void userPut(
    final EIUser user)
    throws EISDatabaseException
  {
    Objects.requireNonNull(user, "user");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseUsersQueries.userPut");

    try {
      var userRec = context.fetchOne(USERS, USERS.ID.eq(user.id()));
      if (userRec == null) {
        userRec = context.newRecord(USERS);
        userRec.set(USERS.ID, user.id());
      }
      userRec.set(USERS.PERMISSIONS, user.permissions().asIntegers());
      userRec.store();
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, DEFAULT_HANDLER);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public Optional<EIUser> userGet(
    final UUID id)
    throws EISDatabaseException
  {
    Objects.requireNonNull(id, "id");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseUsersQueries.userGet");

    try {
      final var userRec = context.fetchOne(USERS, USERS.ID.eq(id));
      if (userRec == null) {
        return Optional.empty();
      }

      final var permissions =
        EIPermissionSet.of(
          Stream.of(userRec.getPermissions())
            .map(EIPermission::ofInteger)
            .toList()
        );

      return Optional.of(new EIUser(id, permissions));
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, DEFAULT_HANDLER);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public EIUser userGetRequire(
    final UUID id)
    throws EISDatabaseException
  {
    return this.userGet(id).orElseThrow(USER_DOES_NOT_EXIST);
  }

  @Override
  public void userLogin(
    final EIUserLogin login)
    throws EISDatabaseException
  {
    Objects.requireNonNull(login, "login");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseUsersQueries.userLogin");

    try {
      context.insertInto(AUDIT)
        .set(AUDIT.USER_ID, login.userId())
        .set(AUDIT.TYPE, "USER_LOGGED_IN")
        .set(AUDIT.MESSAGE, "%s|%s".formatted(login.host(), login.userAgent()))
        .set(AUDIT.TIME, login.time())
        .execute();

    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, DEFAULT_HANDLER);
    } finally {
      querySpan.end();
    }
  }
}
