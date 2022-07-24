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
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseGroupsQueriesType;
import com.io7m.eigion.server.database.postgres.internal.tables.records.GroupUsersRecord;
import com.io7m.jaffirm.core.Postconditions;
import org.jooq.exception.DataAccessException;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.io7m.eigion.server.database.postgres.internal.EIServerDatabaseExceptions.handleDatabaseException;
import static com.io7m.eigion.server.database.postgres.internal.Tables.GROUPS;
import static com.io7m.eigion.server.database.postgres.internal.Tables.GROUP_USERS;
import static com.io7m.eigion.server.database.postgres.internal.Tables.USERS;
import static com.io7m.eigion.server.database.postgres.internal.tables.Audit.AUDIT;

final class EIServerDatabaseGroupQueries
  extends EIBaseQueries
  implements EIServerDatabaseGroupsQueriesType
{
  EIServerDatabaseGroupQueries(
    final EIServerDatabaseTransaction inTransaction)
  {
    super(inTransaction);
  }

  @Override
  public long groupIdentifierLast()
    throws EIServerDatabaseException
  {
    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();

    try {
      final var record =
        context.selectFrom(GROUPS)
          .orderBy(GROUPS.ID.desc())
          .limit(Long.valueOf(1L))
          .fetchOne(GROUPS.ID);

      if (record == null) {
        return 0L;
      }

      return record.longValue();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(transaction, e);
    }
  }

  @Override
  public void groupCreate(
    final EIGroupName name,
    final UUID userFounder)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(userFounder, "userFounder");

    final var transaction = this.transaction();
    final var admin = transaction.adminId();
    final var context = transaction.createContext();

    try {
      final var existingGroup =
        context.fetchOptional(GROUPS, GROUPS.NAME.eq(name.value()));

      if (existingGroup.isPresent()) {
        throw new EIServerDatabaseException(
          "Group already exists",
          "group-duplicate"
        );
      }

      context.fetchOptional(USERS, USERS.ID.eq(userFounder))
        .orElseThrow(() -> {
          return new EIServerDatabaseException(
            "User does not exist",
            "user-nonexistent"
          );
        });

      final var timeNow =
        this.currentTime();

      final var inserted =
        context.insertInto(GROUPS)
          .set(GROUPS.NAME, name.value())
          .set(GROUPS.CREATED, timeNow)
          .set(GROUPS.CREATOR_ADMIN, admin)
          .set(GROUPS.CREATOR_USER, userFounder)
          .execute();

      Postconditions.checkPostconditionV(
        inserted == 1,
        "Expected to insert 1 record (inserted %d)",
        Integer.valueOf(inserted)
      );

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, timeNow)
          .set(AUDIT.TYPE, "GROUP_CREATED")
          .set(AUDIT.USER_ID, admin)
          .set(AUDIT.MESSAGE, name.value());

      insertAuditRecord(audit);
    } catch (final DataAccessException e) {
      throw handleDatabaseException(transaction, e);
    }
  }

  @Override
  public void groupMembershipSet(
    final EIGroupName name,
    final UUID userId,
    final Set<EIGroupRole> roles)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(roles, "roles");

    final var transaction = this.transaction();
    final var admin = transaction.adminId();
    final var context = transaction.createContext();

    try {
      final var existingUser =
        context.fetchOptional(USERS, USERS.ID.eq(userId));

      if (existingUser.isEmpty()) {
        throw new EIServerDatabaseException(
          "User does not exist",
          "user-nonexistent"
        );
      }

      final var existingGroup =
        context.fetchOptional(GROUPS, GROUPS.NAME.eq(name.value()));

      if (existingGroup.isEmpty()) {
        throw new EIServerDatabaseException(
          "Group does not exist",
          "group-nonexistent"
        );
      }

      final var groupId =
        existingGroup.get().getId();

      final var timeNow =
        this.currentTime();

      final var groupRecordOpt =
        context.fetchOptional(
          GROUP_USERS,
          GROUP_USERS.GROUP_ID.eq(groupId).and(GROUP_USERS.USER_ID.eq(userId)));

      final var roleString =
        roles.stream()
          .map(Enum::toString)
          .sorted()
          .collect(Collectors.joining(","));

      final GroupUsersRecord groupRecord;
      if (groupRecordOpt.isPresent()) {
        groupRecord = groupRecordOpt.get();
      } else {
        groupRecord = context.newRecord(GROUP_USERS);
        groupRecord.set(GROUP_USERS.GROUP_ID, groupId);
        groupRecord.set(GROUP_USERS.USER_ID, userId);

        final var audit =
          context.insertInto(AUDIT)
            .set(AUDIT.TIME, timeNow)
            .set(AUDIT.TYPE, "GROUP_USERS_ADDED")
            .set(AUDIT.USER_ID, admin)
            .set(AUDIT.MESSAGE, "%s|%s".formatted(name, userId));

        insertAuditRecord(audit);
      }

      groupRecord.set(GROUP_USERS.ROLES, roleString);
      groupRecord.store();

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, timeNow)
          .set(AUDIT.TYPE, "GROUP_USERS_ROLES_CHANGED")
          .set(AUDIT.USER_ID, admin)
          .set(AUDIT.MESSAGE, "%s|%s|%s".formatted(name, userId, roleString));

      insertAuditRecord(audit);
    } catch (final DataAccessException e) {
      throw handleDatabaseException(transaction, e);
    }
  }

  @Override
  public Optional<Set<EIGroupRole>> groupMembershipGet(
    final EIGroupName name,
    final UUID userId)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(userId, "userId");

    final var transaction = this.transaction();
    final var context = transaction.createContext();

    try {
      final var existingUser =
        context.fetchOptional(USERS, USERS.ID.eq(userId));

      if (existingUser.isEmpty()) {
        throw new EIServerDatabaseException(
          "User does not exist",
          "user-nonexistent"
        );
      }

      final var existingGroup =
        context.fetchOptional(GROUPS, GROUPS.NAME.eq(name.value()));

      if (existingGroup.isEmpty()) {
        throw new EIServerDatabaseException(
          "Group does not exist",
          "group-nonexistent"
        );
      }

      final var groupId =
        existingGroup.get().getId();

      final var groupRecordOpt =
        context.fetchOptional(
          GROUP_USERS,
          GROUP_USERS.GROUP_ID.eq(groupId).and(GROUP_USERS.USER_ID.eq(userId)));

      return groupRecordOpt.map(groupUsersRecord -> {
        return Arrays.stream(groupUsersRecord.get(GROUP_USERS.ROLES)
          .split(","))
          .sorted()
          .filter(s -> !s.isBlank())
          .map(EIGroupRole::valueOf)
          .collect(Collectors.toUnmodifiableSet());
      });
    } catch (final DataAccessException e) {
      throw handleDatabaseException(transaction, e);
    }
  }

  @Override
  public void groupMembershipRemove(
    final EIGroupName name,
    final UUID userId)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(userId, "userId");

    final var transaction = this.transaction();
    final var admin = transaction.adminId();
    final var context = transaction.createContext();

    try {
      final var existingUser =
        context.fetchOptional(USERS, USERS.ID.eq(userId));

      if (existingUser.isEmpty()) {
        throw new EIServerDatabaseException(
          "User does not exist",
          "user-nonexistent"
        );
      }

      final var existingGroup =
        context.fetchOptional(GROUPS, GROUPS.NAME.eq(name.value()));

      if (existingGroup.isEmpty()) {
        throw new EIServerDatabaseException(
          "Group does not exist",
          "group-nonexistent"
        );
      }

      final var groupId =
        existingGroup.get().getId();

      final var timeNow =
        this.currentTime();

      final var groupRecordOpt =
        context.fetchOptional(
          GROUP_USERS,
          GROUP_USERS.GROUP_ID.eq(groupId).and(GROUP_USERS.USER_ID.eq(userId)));

      final GroupUsersRecord groupRecord;
      if (groupRecordOpt.isPresent()) {
        groupRecord = groupRecordOpt.get();
        groupRecord.delete();

        final var audit =
          context.insertInto(AUDIT)
            .set(AUDIT.TIME, timeNow)
            .set(AUDIT.TYPE, "GROUP_USERS_REMOVED")
            .set(AUDIT.USER_ID, admin)
            .set(AUDIT.MESSAGE, "%s|%s".formatted(name, userId));

        insertAuditRecord(audit);
      }
    } catch (final DataAccessException e) {
      throw handleDatabaseException(transaction, e);
    }
  }

  @Override
  public boolean groupExists(
    final EIGroupName name)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(name, "name");

    final var context =
      this.transaction().createContext();

    try {
      return context.fetchOptional(GROUPS, GROUPS.NAME.eq(name.value()))
        .isPresent();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    }
  }
}
