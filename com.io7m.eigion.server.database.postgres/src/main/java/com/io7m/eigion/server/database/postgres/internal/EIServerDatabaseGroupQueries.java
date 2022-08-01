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
import com.io7m.eigion.model.EIGroupCreationRequestStatusType.InProgress;
import com.io7m.eigion.model.EIGroupCreationRequestStatusType.Succeeded;
import com.io7m.eigion.model.EIGroupInvite;
import com.io7m.eigion.model.EIGroupInviteStatus;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIGroupRole;
import com.io7m.eigion.model.EIGroupRoles;
import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.model.EIUserDisplayName;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseGroupsQueriesType;
import com.io7m.eigion.server.database.postgres.internal.enums.GroupCreationRequestStatusT;
import com.io7m.eigion.server.database.postgres.internal.enums.GroupInviteStatusT;
import com.io7m.eigion.server.database.postgres.internal.tables.Users;
import com.io7m.eigion.server.database.postgres.internal.tables.records.GroupUsersRecord;
import com.io7m.eigion.server.database.postgres.internal.tables.records.GroupsCreationRequestsRecord;
import com.io7m.eigion.server.database.postgres.internal.tables.records.GroupsRecord;
import com.io7m.eigion.server.database.postgres.internal.tables.records.UsersRecord;
import com.io7m.jaffirm.core.Postconditions;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.TableOnConditionStep;
import org.jooq.exception.DataAccessException;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.Cancelled;
import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.NAME_CANCELLED;
import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.NAME_FAILED;
import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.NAME_IN_PROGRESS;
import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.NAME_SUCCEEDED;
import static com.io7m.eigion.model.EIGroupInviteStatus.IN_PROGRESS;
import static com.io7m.eigion.server.database.postgres.internal.EIServerDatabaseExceptions.handleDatabaseException;
import static com.io7m.eigion.server.database.postgres.internal.Tables.GROUPS;
import static com.io7m.eigion.server.database.postgres.internal.Tables.GROUPS_CREATION_REQUESTS;
import static com.io7m.eigion.server.database.postgres.internal.Tables.GROUP_INVITES;
import static com.io7m.eigion.server.database.postgres.internal.Tables.GROUP_USERS;
import static com.io7m.eigion.server.database.postgres.internal.Tables.USERS;
import static com.io7m.eigion.server.database.postgres.internal.enums.GroupCreationRequestStatusT.CANCELLED;
import static com.io7m.eigion.server.database.postgres.internal.enums.GroupCreationRequestStatusT.FAILED;
import static com.io7m.eigion.server.database.postgres.internal.enums.GroupCreationRequestStatusT.SUCCEEDED;
import static com.io7m.eigion.server.database.postgres.internal.tables.Audit.AUDIT;

final class EIServerDatabaseGroupQueries
  extends EIBaseQueries
  implements EIServerDatabaseGroupsQueriesType
{
  private static final Users USERS_INVITING =
    USERS.as("USERS_I");
  private static final Users USERS_BEING_INVITED =
    USERS.as("USERS_B");

  /**
   * A JOIN expression that allows for supplying usernames for invites. This
   * is almost always needed when querying invites and so is factored into
   * a reusable expression here.
   */

  private static final TableOnConditionStep<Record> BASE_INVITES_JOIN =
    GROUP_INVITES
      .join(USERS_INVITING)
      .on(GROUP_INVITES.USER_INVITING.eq(USERS_INVITING.ID))
      .join(USERS_BEING_INVITED)
      .on(GROUP_INVITES.USER_BEING_INVITED.eq(USERS_BEING_INVITED.ID));

  EIServerDatabaseGroupQueries(
    final EIServerDatabaseTransaction inTransaction)
  {
    super(inTransaction);
  }

  private static EIGroupCreationRequestStatusType mapStatus(
    final Record rec)
  {
    final var started =
      rec.get(GROUPS_CREATION_REQUESTS.CREATED);

    final var statusName = rec.get(GROUPS_CREATION_REQUESTS.STATUS);
    return switch (statusName) {
      case NAME_FAILED -> {
        yield new Failed(
          started,
          rec.get(GROUPS_CREATION_REQUESTS.COMPLETED),
          rec.get(GROUPS_CREATION_REQUESTS.MESSAGE)
        );
      }
      case NAME_SUCCEEDED -> {
        yield new Succeeded(
          started,
          rec.get(GROUPS_CREATION_REQUESTS.COMPLETED)
        );
      }
      case NAME_IN_PROGRESS -> {
        yield new InProgress(
          started
        );
      }
      case NAME_CANCELLED -> {
        yield new Cancelled(
          started,
          rec.get(GROUPS_CREATION_REQUESTS.COMPLETED)
        );
      }
      default -> throw new IllegalStateException(
        "Unrecognized status: %s".formatted(statusName)
      );
    };
  }

  private static EIGroupCreationRequest mapCreationRequestRecord(
    final Record rec)
  {
    return new EIGroupCreationRequest(
      new EIGroupName(rec.get(GROUPS_CREATION_REQUESTS.GROUP_NAME)),
      rec.get(GROUPS_CREATION_REQUESTS.CREATOR_USER),
      new EIToken(rec.get(GROUPS_CREATION_REQUESTS.GROUP_TOKEN)),
      mapStatus(rec)
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

  private static void checkUserForRequest(
    final UUID userId,
    final GroupsCreationRequestsRecord existing)
    throws EIServerDatabaseException
  {
    final var requestUser =
      existing.get(GROUPS_CREATION_REQUESTS.CREATOR_USER);

    if (!Objects.equals(requestUser, userId)) {
      throw new EIServerDatabaseException(
        "Group request not owned by this user",
        "group-request-wrong-user"
      );
    }
  }

  private static EIGroupRoles mapGroupRoles(
    final Record r)
  {
    return new EIGroupRoles(
      new EIGroupName(r.get(GROUPS.NAME)),
      Arrays.stream(r.get(GROUP_USERS.ROLES)
                      .split(","))
        .filter(s -> !s.isBlank())
        .map(EIGroupRole::valueOf)
        .collect(Collectors.toUnmodifiableSet())
    );
  }

  private static EIGroupInvite mapInvite(
    final String inviterName,
    final Record r)
  {
    return new EIGroupInvite(
      r.get(GROUP_INVITES.USER_INVITING),
      new EIUserDisplayName(inviterName),
      r.get(GROUP_INVITES.USER_BEING_INVITED),
      new EIUserDisplayName(r.get(USERS.NAME)),
      new EIGroupName(r.get(GROUP_INVITES.GROUP_NAME)),
      new EIToken(r.get(GROUP_INVITES.INVITE_TOKEN)),
      mapInviteStatus(r.get(GROUP_INVITES.STATUS)),
      r.get(GROUP_INVITES.CREATED),
      Optional.ofNullable(r.get(GROUP_INVITES.COMPLETED))
    );
  }

  private static EIGroupInviteStatus mapInviteStatus(
    final String status)
  {
    return EIGroupInviteStatus.valueOf(status);
  }

  private static EIGroupInvite joinToInvite(
    final Record r)
  {
    return new EIGroupInvite(
      r.get(USERS_INVITING.ID),
      new EIUserDisplayName(r.get(USERS_INVITING.NAME)),
      r.get(USERS_BEING_INVITED.ID),
      new EIUserDisplayName(r.get(USERS_BEING_INVITED.NAME)),
      new EIGroupName(r.get(GROUP_INVITES.GROUP_NAME)),
      new EIToken(r.get(GROUP_INVITES.INVITE_TOKEN)),
      EIGroupInviteStatus.valueOf(r.get(GROUP_INVITES.STATUS)),
      r.get(GROUP_INVITES.CREATED),
      Optional.ofNullable(r.get(GROUP_INVITES.COMPLETED))
    );
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
          .orderBy(GROUPS.SERIAL.desc())
          .limit(Long.valueOf(1L))
          .fetchOne(GROUPS.SERIAL);

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
          .set(GROUPS.CREATOR, userFounder)
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
          .set(AUDIT.USER_ID, userFounder)
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
    final var context = transaction.createContext();

    try {
      final var userId = request.userFounder();
      final var groupName = request.groupName();

      checkUserExists(context, userId);
      checkGroupDoesNotExist(context, groupName);

      final var token =
        request.token();

      final var existingGroupRequest =
        context.fetchOptional(
          GROUPS_CREATION_REQUESTS,
          GROUPS_CREATION_REQUESTS.GROUP_TOKEN.eq(token.value()));

      if (existingGroupRequest.isPresent()) {
        throw new EIServerDatabaseException(
          "Group request already exists",
          "group-request-duplicate"
        );
      }

      final var timeNow = this.currentTime();
      context.insertInto(GROUPS_CREATION_REQUESTS)
        .set(GROUPS_CREATION_REQUESTS.CREATED, timeNow)
        .set(GROUPS_CREATION_REQUESTS.CREATOR_USER, userId)
        .set(GROUPS_CREATION_REQUESTS.GROUP_NAME, groupName.value())
        .set(GROUPS_CREATION_REQUESTS.GROUP_TOKEN, token.value())
        .set(
          GROUPS_CREATION_REQUESTS.STATUS,
          GroupCreationRequestStatusT.IN_PROGRESS.getLiteral())
        .set(GROUPS_CREATION_REQUESTS.MESSAGE, "")
        .execute();

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, timeNow)
          .set(AUDIT.TYPE, "GROUP_CREATION_REQUESTED")
          .set(AUDIT.USER_ID, userId)
          .set(AUDIT.MESSAGE, "%s|%s".formatted(groupName, token));

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
  public List<EIGroupCreationRequest> groupCreationRequestsObsolete()
    throws EIServerDatabaseException
  {
    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();

    try {
      return context.selectFrom(
          GROUPS_CREATION_REQUESTS.join(GROUPS)
            .on(GROUPS_CREATION_REQUESTS.GROUP_NAME.eq(GROUPS.NAME)))
        .where(GROUPS_CREATION_REQUESTS.COMPLETED.isNull())
        .orderBy(GROUPS_CREATION_REQUESTS.CREATED)
        .stream()
        .map(EIServerDatabaseGroupQueries::mapCreationRequestRecord)
        .toList();

    } catch (final DataAccessException e) {
      throw handleDatabaseException(transaction, e);
    }
  }

  @Override
  public List<EIGroupCreationRequest> groupCreationRequestsActive()
    throws EIServerDatabaseException
  {
    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();

    try {
      return context.selectFrom(
          GROUPS_CREATION_REQUESTS
            .leftAntiJoin(GROUPS)
            .on(GROUPS_CREATION_REQUESTS.GROUP_NAME.eq(GROUPS.NAME))
        ).where(GROUPS_CREATION_REQUESTS.COMPLETED.isNull())
        .orderBy(GROUPS_CREATION_REQUESTS.CREATED)
        .stream()
        .map(EIServerDatabaseGroupQueries::mapCreationRequestRecord)
        .toList();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(transaction, e);
    }
  }

  @Override
  public Optional<EIGroupCreationRequest> groupCreationRequest(
    final EIToken token)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(token, "token");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();

    try {
      return context.selectFrom(GROUPS_CREATION_REQUESTS)
        .where(GROUPS_CREATION_REQUESTS.GROUP_TOKEN.eq(token.value()))
        .fetchOptional()
        .map(EIServerDatabaseGroupQueries::mapCreationRequestRecord);
    } catch (final DataAccessException e) {
      throw handleDatabaseException(transaction, e);
    }
  }

  @Override
  public void groupCreationRequestComplete(
    final EIGroupCreationRequest request)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(request, "request");

    final var transaction = this.transaction();
    final var context = transaction.createContext();

    try {
      final var userId = request.userFounder();
      final var groupName = request.groupName();
      final var token = request.token();

      checkUserExists(context, userId);
      checkGroupDoesNotExist(context, groupName);

      final var existing =
        context.fetchOptional(
            GROUPS_CREATION_REQUESTS,
            GROUPS_CREATION_REQUESTS.GROUP_TOKEN.eq(token.value())
              .and(GROUPS_CREATION_REQUESTS.GROUP_NAME.eq(groupName.value())))
          .orElseThrow(() -> new EIServerDatabaseException(
            "Group request does not exist",
            "group-request-nonexistent"
          ));

      checkUserForRequest(userId, existing);

      final var status = request.status();
      if (status instanceof InProgress) {
        existing.set(
          GROUPS_CREATION_REQUESTS.STATUS,
          GroupCreationRequestStatusT.IN_PROGRESS.getLiteral());
        existing.store();
        return;
      }

      if (status instanceof Cancelled cancelled) {
        existing.set(GROUPS_CREATION_REQUESTS.STATUS, CANCELLED.getLiteral());
        existing.set(
          GROUPS_CREATION_REQUESTS.COMPLETED,
          cancelled.timeCompletedValue());
        existing.set(GROUPS_CREATION_REQUESTS.MESSAGE, "");
        existing.store();

        final var audit =
          context.insertInto(AUDIT)
            .set(AUDIT.TIME, this.currentTime())
            .set(AUDIT.TYPE, "GROUP_CREATION_REQUEST_CANCELLED")
            .set(AUDIT.USER_ID, userId)
            .set(AUDIT.MESSAGE, "%s|%s".formatted(groupName, token));

        insertAuditRecord(audit);
        return;
      }

      if (status instanceof Failed failed) {
        existing.set(GROUPS_CREATION_REQUESTS.STATUS, FAILED.getLiteral());
        existing.set(
          GROUPS_CREATION_REQUESTS.COMPLETED,
          failed.timeCompletedValue());
        existing.set(GROUPS_CREATION_REQUESTS.MESSAGE, failed.message());
        existing.store();

        final var audit =
          context.insertInto(AUDIT)
            .set(AUDIT.TIME, this.currentTime())
            .set(AUDIT.TYPE, "GROUP_CREATION_REQUEST_FAILED")
            .set(AUDIT.USER_ID, userId)
            .set(AUDIT.MESSAGE, "%s|%s".formatted(groupName, token));

        insertAuditRecord(audit);
        return;
      }

      if (status instanceof Succeeded succeeded) {
        existing.set(GROUPS_CREATION_REQUESTS.STATUS, SUCCEEDED.getLiteral());
        existing.set(
          GROUPS_CREATION_REQUESTS.COMPLETED,
          succeeded.timeCompletedValue());
        existing.set(GROUPS_CREATION_REQUESTS.MESSAGE, "");
        existing.store();

        final var audit =
          context.insertInto(AUDIT)
            .set(AUDIT.TIME, this.currentTime())
            .set(AUDIT.TYPE, "GROUP_CREATION_REQUEST_SUCCEEDED")
            .set(AUDIT.USER_ID, userId)
            .set(AUDIT.MESSAGE, "%s|%s".formatted(groupName, token));

        insertAuditRecord(audit);

        final var groupOpt =
          context.selectFrom(GROUPS)
            .where(GROUPS.NAME.eq(groupName.value()))
            .fetchOptional();

        if (groupOpt.isEmpty()) {
          this.groupCreate(groupName, userId);
        }
      }
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
    final var context = transaction.createContext();

    try {
      checkUserExists(context, userId);
      final var existingGroup =
        checkGroupExists(context, name);

      final var groupId =
        existingGroup.getName();
      final var timeNow =
        this.currentTime();

      final var groupRecordOpt =
        context.fetchOptional(
          GROUP_USERS,
          GROUP_USERS.GROUP_NAME.eq(groupId).and(GROUP_USERS.USER_ID.eq(userId)));

      final var roleString =
        roles.stream()
          .map(Enum::toString)
          .sorted()
          .collect(Collectors.joining(","));

      final var auditUser =
        transaction.adminIdIfPresent()
          .orElse(userId);

      final GroupUsersRecord groupRecord;
      if (groupRecordOpt.isPresent()) {
        groupRecord = groupRecordOpt.get();
      } else {
        groupRecord = context.newRecord(GROUP_USERS);
        groupRecord.set(GROUP_USERS.GROUP_NAME, groupId);
        groupRecord.set(GROUP_USERS.USER_ID, userId);

        final var audit =
          context.insertInto(AUDIT)
            .set(AUDIT.TIME, timeNow)
            .set(AUDIT.TYPE, "GROUP_USERS_ADDED")
            .set(AUDIT.USER_ID, auditUser)
            .set(AUDIT.MESSAGE, "%s|%s".formatted(name, userId));

        insertAuditRecord(audit);
      }

      groupRecord.set(GROUP_USERS.ROLES, roleString);
      groupRecord.store();

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, timeNow)
          .set(AUDIT.TYPE, "GROUP_USERS_ROLES_CHANGED")
          .set(AUDIT.USER_ID, auditUser)
          .set(AUDIT.MESSAGE, "%s|%s|%s".formatted(name, userId, roleString));

      insertAuditRecord(audit);
    } catch (final DataAccessException e) {
      throw handleDatabaseException(transaction, e);
    }
  }

  @Override
  public List<EIGroupRoles> groupMembershipGet(
    final UUID userId)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(userId, "userId");

    final var transaction = this.transaction();
    final var context = transaction.createContext();

    try {
      checkUserExists(context, userId);

      return context.selectFrom(
          GROUP_USERS.join(GROUPS)
            .on(GROUPS.NAME.eq(GROUP_USERS.GROUP_NAME)))
        .where(GROUP_USERS.USER_ID.eq(userId))
        .stream()
        .map(EIServerDatabaseGroupQueries::mapGroupRoles)
        .toList();
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
        existingGroup.getName();

      final var timeNow =
        this.currentTime();

      final var groupRecordOpt =
        context.fetchOptional(
          GROUP_USERS,
          GROUP_USERS.GROUP_NAME.eq(groupId).and(GROUP_USERS.USER_ID.eq(userId)));

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

  @Override
  public EIGroupInvite groupInvite(
    final EIGroupName group,
    final UUID userBeingInvited)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(group, "group");
    Objects.requireNonNull(userBeingInvited, "userBeingInvited");

    final var transaction = this.transaction();
    final var context = transaction.createContext();
    final var userInviting = transaction.userId();

    try {
      final var userB =
        checkUserExists(context, userBeingInvited);
      final var userI =
        checkUserExists(context, userInviting);

      if (userInviting.equals(userBeingInvited)) {
        throw new EIServerDatabaseException(
          "Group inviter and invitee must be different.",
          "group-inviter-invitee"
        );
      }

      checkGroupExists(context, group);

      final var join =
        BASE_INVITES_JOIN;

      final var inviteRecord =
        context.select(
            GROUP_INVITES.COMPLETED,
            GROUP_INVITES.CREATED,
            GROUP_INVITES.GROUP_NAME,
            GROUP_INVITES.INVITE_TOKEN,
            GROUP_INVITES.STATUS,
            GROUP_INVITES.USER_BEING_INVITED,
            GROUP_INVITES.USER_INVITING,
            USERS_BEING_INVITED.ID,
            USERS_BEING_INVITED.NAME,
            USERS_INVITING.ID,
            USERS_INVITING.NAME)
          .from(join)
          .where(
            GROUP_INVITES.GROUP_NAME.eq(group.value())
              .and(GROUP_INVITES.USER_INVITING.eq(userInviting))
              .and(GROUP_INVITES.USER_BEING_INVITED.eq(userBeingInvited)))
          .limit(1L)
          .fetch();

      if (inviteRecord.isNotEmpty()) {
        return joinToInvite(inviteRecord.get(0));
      }

      final var timeNow =
        this.currentTime();
      final var token =
        EIToken.generate();

      final var newRecord = context.newRecord(GROUP_INVITES);
      newRecord.set(GROUP_INVITES.GROUP_NAME, group.value());
      newRecord.set(GROUP_INVITES.USER_INVITING, userInviting);
      newRecord.set(GROUP_INVITES.USER_BEING_INVITED, userBeingInvited);
      newRecord.set(GROUP_INVITES.INVITE_TOKEN, token.value());
      newRecord.set(
        GROUP_INVITES.STATUS,
        GroupInviteStatusT.IN_PROGRESS.getLiteral());
      newRecord.set(GROUP_INVITES.CREATED, timeNow);
      newRecord.store();

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, timeNow)
          .set(AUDIT.TYPE, "INVITE_CREATED")
          .set(AUDIT.USER_ID, userInviting)
          .set(
            AUDIT.MESSAGE,
            "%s|%s|%s".formatted(group, userBeingInvited, token.value()));

      insertAuditRecord(audit);

      return new EIGroupInvite(
        userInviting,
        new EIUserDisplayName(userI.getName()),
        userBeingInvited,
        new EIUserDisplayName(userB.getName()),
        group,
        token,
        IN_PROGRESS,
        timeNow,
        Optional.empty()
      );
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    }
  }

  @Override
  public List<EIGroupInvite> groupInvitesCreatedByUser()
    throws EIServerDatabaseException
  {
    final var transaction = this.transaction();
    final var user = transaction.userId();
    final var context = transaction.createContext();

    try {
      return context.select(
          GROUP_INVITES.COMPLETED,
          GROUP_INVITES.CREATED,
          GROUP_INVITES.GROUP_NAME,
          GROUP_INVITES.INVITE_TOKEN,
          GROUP_INVITES.STATUS,
          GROUP_INVITES.USER_BEING_INVITED,
          GROUP_INVITES.USER_INVITING,
          USERS_BEING_INVITED.ID,
          USERS_BEING_INVITED.NAME,
          USERS_INVITING.ID,
          USERS_INVITING.NAME)
        .from(BASE_INVITES_JOIN)
        .where(GROUP_INVITES.USER_INVITING.eq(user))
        .orderBy(GROUP_INVITES.CREATED)
        .stream()
        .map(EIServerDatabaseGroupQueries::joinToInvite)
        .toList();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    }
  }

  @Override
  public List<EIGroupInvite> groupInvitesReceivedByUser()
    throws EIServerDatabaseException
  {
    final var transaction = this.transaction();
    final var user = transaction.userId();
    final var context = transaction.createContext();

    try {
      return context.select(
          GROUP_INVITES.COMPLETED,
          GROUP_INVITES.CREATED,
          GROUP_INVITES.GROUP_NAME,
          GROUP_INVITES.INVITE_TOKEN,
          GROUP_INVITES.STATUS,
          GROUP_INVITES.USER_BEING_INVITED,
          GROUP_INVITES.USER_INVITING,
          USERS_BEING_INVITED.ID,
          USERS_BEING_INVITED.NAME,
          USERS_INVITING.ID,
          USERS_INVITING.NAME)
        .from(BASE_INVITES_JOIN)
        .where(GROUP_INVITES.USER_BEING_INVITED.eq(user))
        .orderBy(GROUP_INVITES.CREATED)
        .stream()
        .map(EIServerDatabaseGroupQueries::joinToInvite)
        .toList();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    }
  }
}
