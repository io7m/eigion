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

import com.io7m.eigion.model.EIGroupCreationRequest;
import com.io7m.eigion.model.EIGroupCreationRequestStatusType;
import com.io7m.eigion.model.EIGroupCreationRequestStatusType.Failed;
import com.io7m.eigion.model.EIGroupCreationRequestStatusType.Succeeded;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIGroupRole;
import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseGroupsQueriesType;
import com.io7m.eigion.server.database.postgres.internal.tables.records.GroupUsersRecord;
import com.io7m.eigion.server.database.postgres.internal.tables.records.GroupsCreationRequestsRecord;
import com.io7m.eigion.server.database.postgres.internal.tables.records.GroupsRecord;
import com.io7m.eigion.server.database.postgres.internal.tables.records.UsersRecord;
import com.io7m.jaffirm.core.Postconditions;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.io7m.eigion.server.database.postgres.internal.EIServerDatabaseExceptions.handleDatabaseException;
import static com.io7m.eigion.server.database.postgres.internal.Tables.GROUPS;
import static com.io7m.eigion.server.database.postgres.internal.Tables.GROUPS_CREATION_REQUESTS;
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

  private static EIGroupCreationRequest mapCreationRequestRecord(
    final GroupsCreationRequestsRecord rec)
  {
    final Optional<EIGroupCreationRequestStatusType> status;
    final var completed = rec.getCompleted();
    if (completed != null) {
      final var failed = rec.getFailed();
      final var started = rec.getCreated();
      if (failed != null) {
        status = Optional.of(new Failed(started, completed, failed));
      } else {
        status = Optional.of(new Succeeded(started, completed));
      }
    } else {
      status = Optional.empty();
    }

    return new EIGroupCreationRequest(
      new EIGroupName(rec.getGroupName()),
      rec.getCreatorUser(),
      new EIToken(rec.getGroupToken()),
      status
    );
  }

  private static void checkGroupDoesNotExist(
    final DSLContext context,
    final EIGroupName groupName)
    throws EIServerDatabaseException
  {
    final var existingGroup =
      context.fetchOptional(GROUPS, GROUPS.NAME.eq(groupName.value()));

    if (existingGroup.isPresent()) {
      throw new EIServerDatabaseException(
        "Group already exists",
        "group-duplicate"
      );
    }
  }

  private static UsersRecord checkUserExists(
    final DSLContext context,
    final UUID userFounder)
    throws EIServerDatabaseException
  {
    return context.fetchOptional(USERS, USERS.ID.eq(userFounder))
      .orElseThrow(() -> {
        return new EIServerDatabaseException(
          "User does not exist",
          "user-nonexistent"
        );
      });
  }

  private static GroupsRecord checkGroupExists(
    final DSLContext context,
    final EIGroupName name)
    throws EIServerDatabaseException
  {
    return context.fetchOptional(GROUPS, GROUPS.NAME.eq(name.value()))
      .orElseThrow(() -> {
        return new EIServerDatabaseException(
          "Group does not exist",
          "group-nonexistent"
        );
      });
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
      checkGroupDoesNotExist(context, name);
      checkUserExists(context, userFounder);

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
  public void groupCreationRequestStart(
    final EIGroupCreationRequest request)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(request, "request");

    final var transaction = this.transaction();
    final var admin = transaction.adminId();
    final var context = transaction.createContext();

    try {
      final var userId = request.userFounder();
      final var groupName = request.groupName();

      checkUserExists(context, userId);
      checkGroupDoesNotExist(context, groupName);

      final var existingGroupRequest =
        context.fetchOptional(
          GROUPS_CREATION_REQUESTS,
          GROUPS_CREATION_REQUESTS.GROUP_NAME.eq(groupName.value()));

      if (existingGroupRequest.isPresent()) {
        throw new EIServerDatabaseException(
          "Group request already exists",
          "group-request-duplicate"
        );
      }

      final var token =
        request.token();

      final var timeNow =
        this.currentTime();

      context.insertInto(GROUPS_CREATION_REQUESTS)
        .set(GROUPS_CREATION_REQUESTS.CREATED, timeNow)
        .set(GROUPS_CREATION_REQUESTS.CREATOR_ADMIN, admin)
        .set(GROUPS_CREATION_REQUESTS.CREATOR_USER, userId)
        .set(GROUPS_CREATION_REQUESTS.GROUP_NAME, groupName.value())
        .set(GROUPS_CREATION_REQUESTS.GROUP_TOKEN, token.value())
        .execute();

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, timeNow)
          .set(AUDIT.TYPE, "GROUP_CREATION_REQUESTED")
          .set(AUDIT.USER_ID, admin)
          .set(AUDIT.MESSAGE, "%s|%s|%s".formatted(groupName, userId, token));

      insertAuditRecord(audit);
    } catch (final DataAccessException e) {
      throw handleDatabaseException(transaction, e);
    }
  }

  @Override
  public List<EIGroupCreationRequest> groupCreationRequestsForUser(
    final UUID userId)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(userId, "userId");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();

    try {
      checkUserExists(context, userId);

      return context.selectFrom(GROUPS_CREATION_REQUESTS)
        .where(GROUPS_CREATION_REQUESTS.CREATOR_USER.eq(userId))
        .orderBy(GROUPS_CREATION_REQUESTS.CREATED)
        .stream()
        .map(EIServerDatabaseGroupQueries::mapCreationRequestRecord)
        .toList();

    } catch (final DataAccessException e) {
      throw handleDatabaseException(transaction, e);
    }
  }

  @Override
  public void groupCreationRequestCompleteSuccessfully(
    final EIGroupCreationRequest request)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(request, "request");

    final var transaction = this.transaction();
    final var admin = transaction.adminId();
    final var context = transaction.createContext();

    try {
      final var userId = request.userFounder();
      final var groupName = request.groupName();

      checkUserExists(context, userId);
      checkGroupDoesNotExist(context, groupName);

      final var existingGroupRequest =
        context.fetchOptional(
            GROUPS_CREATION_REQUESTS,
            GROUPS_CREATION_REQUESTS.GROUP_NAME.eq(groupName.value()))
          .orElseThrow(() -> new EIServerDatabaseException(
            "Group request does not exist",
            "group-request-nonexistent"
          ));

      final var token =
        request.token();

      if (!Objects.equals(
        existingGroupRequest.get(GROUPS_CREATION_REQUESTS.GROUP_TOKEN),
        token.value())) {
        throw new EIServerDatabaseException(
          "Group request token does not match",
          "group-request-token"
        );
      }

      final var timeNow = this.currentTime();
      existingGroupRequest.set(GROUPS_CREATION_REQUESTS.COMPLETED, timeNow);
      existingGroupRequest.store();

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, timeNow)
          .set(AUDIT.TYPE, "GROUP_CREATION_REQUEST_SUCCEEDED")
          .set(AUDIT.USER_ID, admin)
          .set(AUDIT.MESSAGE, "%s|%s|%s".formatted(groupName, userId, token));

      insertAuditRecord(audit);

      this.groupCreate(groupName, userId);
    } catch (final DataAccessException e) {
      throw handleDatabaseException(transaction, e);
    }
  }

  @Override
  public void groupCreationRequestCompleteFailed(
    final EIGroupCreationRequest request,
    final String message)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(request, "request");

    final var transaction = this.transaction();
    final var admin = transaction.adminId();
    final var context = transaction.createContext();

    try {
      final var userId = request.userFounder();
      final var groupName = request.groupName();

      checkUserExists(context, userId);
      checkGroupDoesNotExist(context, groupName);

      final var existingGroupRequest =
        context.fetchOptional(
            GROUPS_CREATION_REQUESTS,
            GROUPS_CREATION_REQUESTS.GROUP_NAME.eq(groupName.value()))
          .orElseThrow(() -> new EIServerDatabaseException(
            "Group request does not exist",
            "group-request-nonexistent"
          ));

      final var token =
        request.token();

      if (!Objects.equals(
        existingGroupRequest.get(GROUPS_CREATION_REQUESTS.GROUP_TOKEN),
        token.value())) {
        throw new EIServerDatabaseException(
          "Group request token does not match",
          "group-request-token"
        );
      }

      final var timeNow = this.currentTime();
      existingGroupRequest.set(GROUPS_CREATION_REQUESTS.COMPLETED, timeNow);
      existingGroupRequest.set(GROUPS_CREATION_REQUESTS.FAILED, message);
      existingGroupRequest.store();

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, timeNow)
          .set(AUDIT.TYPE, "GROUP_CREATION_REQUEST_FAILED")
          .set(AUDIT.USER_ID, admin)
          .set(AUDIT.MESSAGE, "%s|%s|%s".formatted(groupName, userId, token));

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
      checkUserExists(context, userId);
      final var existingGroup =
        checkGroupExists(context, name);

      final var groupId =
        existingGroup.getId();
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
      checkUserExists(context, userId);

      final var existingGroup =
        checkGroupExists(context, name);
      final var groupId =
        existingGroup.getId();

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
      checkUserExists(context, userId);

      final var existingGroup =
        checkGroupExists(context, name);
      final var groupId =
        existingGroup.getId();

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
