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

import com.io7m.eigion.model.EIAdmin;
import com.io7m.eigion.model.EIAdminPermission;
import com.io7m.eigion.model.EIAdminSummary;
import com.io7m.eigion.model.EIPassword;
import com.io7m.eigion.model.EIPasswordAlgorithms;
import com.io7m.eigion.model.EIPasswordException;
import com.io7m.eigion.model.EIUserDisplayName;
import com.io7m.eigion.model.EIUserEmail;
import com.io7m.eigion.server.database.api.EIServerDatabaseAdminsQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.postgres.internal.tables.records.AdminsRecord;
import org.jooq.exception.DataAccessException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.ADMIN_DUPLICATE_EMAIL;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.ADMIN_DUPLICATE_ID;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.ADMIN_DUPLICATE_NAME;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.ADMIN_NONEXISTENT;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.ADMIN_NOT_INITIAL;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.PASSWORD_ERROR;
import static com.io7m.eigion.server.database.postgres.internal.EIServerDatabaseExceptions.handleDatabaseException;
import static com.io7m.eigion.server.database.postgres.internal.Tables.USER_IDS;
import static com.io7m.eigion.server.database.postgres.internal.tables.Admins.ADMINS;
import static com.io7m.eigion.server.database.postgres.internal.tables.Audit.AUDIT;

final class EIServerDatabaseAdminsQueries
  extends EIBaseQueries
  implements EIServerDatabaseAdminsQueriesType
{
  EIServerDatabaseAdminsQueries(
    final EIServerDatabaseTransaction inTransaction)
  {
    super(inTransaction);
  }

  private static EIAdmin adminRecordToAdmin(
    final AdminsRecord adminRecord)
    throws EIPasswordException
  {
    return new EIAdmin(
      adminRecord.getId(),
      new EIUserDisplayName(adminRecord.getName()),
      new EIUserEmail(adminRecord.getEmail()),
      adminRecord.getCreated(),
      adminRecord.getLastLoginTime(),
      new EIPassword(
        EIPasswordAlgorithms.parse(adminRecord.getPasswordAlgo()),
        adminRecord.getPasswordHash().toUpperCase(Locale.ROOT),
        adminRecord.getPasswordSalt().toUpperCase(Locale.ROOT)
      ),
      permissionsDeserializeRecord(adminRecord)
    );
  }

  private static Set<EIAdminPermission> permissionsDeserializeRecord(
    final AdminsRecord adminRecord)
  {
    return permissionsDeserialize(adminRecord.getPermissions());
  }

  private static Set<EIAdminPermission> permissionsDeserialize(
    final String str)
  {
    return Arrays.stream(str.split(","))
      .filter(s -> !s.isBlank())
      .map(EIAdminPermission::valueOf)
      .collect(Collectors.toUnmodifiableSet());
  }

  private static String permissionsSerialize(
    final Set<EIAdminPermission> permissions)
  {
    return permissions.stream()
      .map(Enum::toString)
      .sorted()
      .collect(Collectors.joining(","));
  }

  private static Optional<EIAdmin> adminMap(
    final Optional<AdminsRecord> recordOpt)
    throws EIPasswordException
  {
    if (recordOpt.isPresent()) {
      return Optional.of(adminRecordToAdmin(recordOpt.get()));
    }
    return Optional.empty();
  }

  private static EIServerDatabaseException handlePasswordException(
    final EIPasswordException exception)
  {
    return new EIServerDatabaseException(
      exception.getMessage(),
      exception,
      PASSWORD_ERROR
    );
  }

  @Override
  public EIAdmin adminCreateInitial(
    final UUID id,
    final String adminName,
    final String email,
    final OffsetDateTime created,
    final EIPassword password)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(adminName, "adminName");
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(created, "created");
    Objects.requireNonNull(password, "password");

    final var context =
      this.transaction().createContext();

    try {
      final var existing =
        context.selectFrom(ADMINS)
          .limit(Integer.valueOf(1))
          .fetch();

      if (existing.isNotEmpty()) {
        throw new EIServerDatabaseException(
          "Admin already exists",
          ADMIN_NOT_INITIAL
        );
      }

      final var idCreate =
        context.insertInto(USER_IDS)
          .set(USER_IDS.ID, id);

      idCreate.execute();

      final var permissionString =
        permissionsSerialize(EnumSet.allOf(EIAdminPermission.class));

      final var adminCreate =
        context.insertInto(ADMINS)
          .set(ADMINS.ID, id)
          .set(ADMINS.NAME, adminName)
          .set(ADMINS.EMAIL, email)
          .set(ADMINS.CREATED, created)
          .set(ADMINS.LAST_LOGIN_TIME, created)
          .set(ADMINS.PASSWORD_ALGO, password.algorithm().identifier())
          .set(ADMINS.PASSWORD_HASH, password.hash())
          .set(ADMINS.PASSWORD_SALT, password.salt())
          .set(ADMINS.PERMISSIONS, permissionString);

      adminCreate.execute();

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, this.currentTime())
          .set(AUDIT.TYPE, "ADMIN_CREATED")
          .set(AUDIT.USER_ID, id)
          .set(AUDIT.MESSAGE, id.toString());

      audit.execute();
      return this.adminGet(id).orElseThrow();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    }
  }

  @Override
  public EIAdmin adminCreate(
    final UUID id,
    final String adminName,
    final String email,
    final OffsetDateTime created,
    final EIPassword password,
    final Set<EIAdminPermission> permissions)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(adminName, "adminName");
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(created, "created");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(permissions, "permissions");

    final var context =
      this.transaction().createContext();

    try {
      {
        final var existing =
          context.fetchOptional(USER_IDS, USER_IDS.ID.eq(id));
        if (existing.isPresent()) {
          throw new EIServerDatabaseException(
            "Admin ID already exists",
            ADMIN_DUPLICATE_ID
          );
        }
      }

      {
        final var existing =
          context.fetchOptional(ADMINS, ADMINS.NAME.eq(adminName));
        if (existing.isPresent()) {
          throw new EIServerDatabaseException(
            "Admin name already exists",
            ADMIN_DUPLICATE_NAME
          );
        }
      }

      {
        final var existing =
          context.fetchOptional(ADMINS, ADMINS.EMAIL.eq(email));
        if (existing.isPresent()) {
          throw new EIServerDatabaseException(
            "Email already exists",
            ADMIN_DUPLICATE_EMAIL
          );
        }
      }

      final var idCreate =
        context.insertInto(USER_IDS)
          .set(USER_IDS.ID, id);

      idCreate.execute();

      final var permissionString =
        permissionsSerialize(permissions);

      final var adminCreate =
        context.insertInto(ADMINS)
          .set(ADMINS.ID, id)
          .set(ADMINS.NAME, adminName)
          .set(ADMINS.EMAIL, email)
          .set(ADMINS.CREATED, created)
          .set(ADMINS.LAST_LOGIN_TIME, created)
          .set(ADMINS.PASSWORD_ALGO, password.algorithm().identifier())
          .set(ADMINS.PASSWORD_HASH, password.hash())
          .set(ADMINS.PASSWORD_SALT, password.salt())
          .set(ADMINS.PERMISSIONS, permissionString);

      adminCreate.execute();

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, this.currentTime())
          .set(AUDIT.TYPE, "ADMIN_CREATED")
          .set(AUDIT.USER_ID, id)
          .set(AUDIT.MESSAGE, id.toString());

      audit.execute();
      return this.adminGet(id).orElseThrow();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    }
  }

  @Override
  public Optional<EIAdmin> adminGet(
    final UUID id)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");

    final var context = this.transaction().createContext();
    try {
      return adminMap(context.fetchOptional(ADMINS, ADMINS.ID.eq(id)));
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    } catch (final EIPasswordException e) {
      throw handlePasswordException(e);
    }
  }

  @Override
  public Optional<EIAdmin> adminGetForName(
    final String name)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(name, "name");

    final var context = this.transaction().createContext();
    try {
      return adminMap(
        context.fetchOptional(ADMINS, ADMINS.NAME.eq(name)));
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    } catch (final EIPasswordException e) {
      throw handlePasswordException(e);
    }
  }

  @Override
  public Optional<EIAdmin> adminGetForEmail(
    final String email)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(email, "email");

    final var context = this.transaction().createContext();
    try {
      return adminMap(
        context.fetchOptional(ADMINS, ADMINS.EMAIL.eq(email)));
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    } catch (final EIPasswordException e) {
      throw handlePasswordException(e);
    }
  }

  @Override
  public void adminLogin(
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
        context.fetchOptional(ADMINS, ADMINS.ID.eq(id));
      if (!existingOpt.isPresent()) {
        throw new EIServerDatabaseException(
          "Admin does not exist",
          ADMIN_NONEXISTENT
        );
      }

      final var existing = existingOpt.get();
      existing.setLastLoginTime(time);
      existing.store();

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, time)
          .set(AUDIT.TYPE, "ADMIN_LOGGED_IN")
          .set(AUDIT.USER_ID, id)
          .set(AUDIT.MESSAGE, host);

      audit.execute();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    }
  }

  @Override
  public List<EIAdminSummary> adminSearch(
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
        context.selectFrom(ADMINS)
          .where(ADMINS.NAME.likeIgnoreCase(wildcardQuery))
          .or(ADMINS.EMAIL.likeIgnoreCase(wildcardQuery))
          .or(ADMINS.ID.likeIgnoreCase(wildcardQuery))
          .orderBy(ADMINS.NAME)
          .fetch();

      final var summaries = new ArrayList<EIAdminSummary>(records.size());
      for (final var record : records) {
        summaries.add(
          new EIAdminSummary(
            record.get(ADMINS.ID),
            new EIUserDisplayName(record.get(ADMINS.NAME)),
            new EIUserEmail(record.get(ADMINS.EMAIL))
          )
        );
      }
      return List.copyOf(summaries);
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    }
  }
}
