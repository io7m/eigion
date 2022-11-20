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
import com.io7m.eigion.model.EIGroupCreationRequestSearchParameters;
import com.io7m.eigion.model.EIGroupCreationRequestStatusType;
import com.io7m.eigion.model.EIGroupMembership;
import com.io7m.eigion.model.EIGroupMembershipWithUser;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIGroupPrefix;
import com.io7m.eigion.model.EIGroupRole;
import com.io7m.eigion.model.EIGroupSearchByNameParameters;
import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.server.database.api.EISDatabaseException;
import com.io7m.eigion.server.database.api.EISDatabaseGroupsPagedQueryType;
import com.io7m.eigion.server.database.api.EISDatabaseGroupsQueriesType;
import com.io7m.eigion.server.database.postgres.internal.tables.records.GroupsCreationRequestsRecord;
import com.io7m.jqpage.core.JQField;
import com.io7m.jqpage.core.JQKeysetRandomAccessPagination;
import com.io7m.jqpage.core.JQOrder;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.GROUP_DUPLICATE;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.GROUP_NONEXISTENT;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.GROUP_REQUEST_DUPLICATE;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.GROUP_REQUEST_NONEXISTENT;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.GROUP_REQUEST_WRONG_USER;
import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.Cancelled;
import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.Failed;
import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.InProgress;
import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.NAME_CANCELLED;
import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.NAME_FAILED;
import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.NAME_IN_PROGRESS;
import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.NAME_SUCCEEDED;
import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.Succeeded;
import static com.io7m.eigion.server.database.postgres.internal.EISDatabaseExceptions.DEFAULT_HANDLER;
import static com.io7m.eigion.server.database.postgres.internal.EISDatabaseExceptions.handleDatabaseException;
import static com.io7m.eigion.server.database.postgres.internal.EISDatabaseUsersQueries.USER_DOES_NOT_EXIST;
import static com.io7m.eigion.server.database.postgres.internal.Tables.AUDIT;
import static com.io7m.eigion.server.database.postgres.internal.Tables.GROUPS;
import static com.io7m.eigion.server.database.postgres.internal.Tables.GROUPS_CREATION_REQUESTS;
import static com.io7m.eigion.server.database.postgres.internal.Tables.GROUP_ROLES;
import static com.io7m.eigion.server.database.postgres.internal.enums.GroupCreationRequestStatusT.CANCELLED;
import static com.io7m.eigion.server.database.postgres.internal.enums.GroupCreationRequestStatusT.FAILED;
import static com.io7m.eigion.server.database.postgres.internal.enums.GroupCreationRequestStatusT.IN_PROGRESS;
import static com.io7m.eigion.server.database.postgres.internal.enums.GroupCreationRequestStatusT.SUCCEEDED;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.DB_STATEMENT;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.postgresql.util.PSQLState.FOREIGN_KEY_VIOLATION;
import static org.postgresql.util.PSQLState.NOT_NULL_VIOLATION;
import static org.postgresql.util.PSQLState.UNIQUE_VIOLATION;

final class EISDatabaseGroupsQueries
  extends EISBaseQueries
  implements EISDatabaseGroupsQueriesType
{
  EISDatabaseGroupsQueries(
    final EISDatabaseTransaction inTransaction)
  {
    super(inTransaction);
  }

  private static Integer[] groupRolesToIntegers(
    final Set<EIGroupRole> roles)
  {
    final var results = new Integer[roles.size()];
    var index = 0;
    for (final var role : roles) {
      results[index] = Integer.valueOf(role.index());
      ++index;
    }
    return results;
  }

  private static Optional<EISDatabaseException> handleGroupCreationFailure(
    final DataAccessException ex)
  {
    if (Objects.equals(ex.sqlState(), UNIQUE_VIOLATION.getState())) {
      return Optional.of(
        new EISDatabaseException("Group already exists.", GROUP_DUPLICATE)
      );
    }
    return Optional.empty();
  }

  private static Optional<EISDatabaseException> handleGroupUserUpdateFailure(
    final DataAccessException ex)
  {
    final var state = ex.sqlState();
    if (Objects.equals(state, FOREIGN_KEY_VIOLATION.getState())) {
      if (ex.getMessage().contains("group_roles_user_id_fkey")) {
        return Optional.of(USER_DOES_NOT_EXIST.get());
      }
    }
    if (Objects.equals(state, NOT_NULL_VIOLATION.getState())) {
      if (ex.getMessage().contains("group_id")) {
        return Optional.of(
          new EISDatabaseException(
            "Group does not exist.",
            GROUP_NONEXISTENT
          ));
      }
    }
    return Optional.empty();
  }

  @Override
  public void groupCreate(
    final UUID userId,
    final EIGroupName name)
    throws EISDatabaseException
  {
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(name, "name");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "EISDatabaseGroupsQueries.groupCreate");

    try {
      final var time = this.currentTime();
      context.insertInto(GROUPS)
        .set(GROUPS.NAME, name.value())
        .set(GROUPS.CREATOR, userId)
        .set(GROUPS.CREATED, time)
        .set(GROUPS.PERSONAL, FALSE)
        .execute();

      context.insertInto(AUDIT)
        .set(AUDIT.TIME, time)
        .set(AUDIT.TYPE, "GROUP_CREATED")
        .set(AUDIT.USER_ID, userId)
        .set(AUDIT.MESSAGE, name.value())
        .execute();

    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(
        this.transaction(),
        e,
        EISDatabaseGroupsQueries::handleGroupCreationFailure
      );
    } finally {
      querySpan.end();
    }
  }

  @Override
  public EIGroupName groupCreatePersonal(
    final UUID userId,
    final EIGroupPrefix prefix)
    throws EISDatabaseException
  {
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(prefix, "prefix");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "EISDatabaseGroupsQueries.groupCreatePersonal");

    try {
      final var time = this.currentTime();

      final var id =
        context.insertInto(GROUPS)
          .set(GROUPS.NAME, "undefined")
          .set(GROUPS.CREATED, time)
          .set(GROUPS.CREATOR, userId)
          .set(GROUPS.PERSONAL, TRUE)
          .returning(GROUPS.ID)
          .fetchOne(GROUPS.ID);

      final var groupName =
        prefix.toGroupName(id.longValue());

      context.update(GROUPS)
        .set(GROUPS.NAME, groupName.value())
        .where(GROUPS.ID.eq(id))
        .execute();

      context.insertInto(AUDIT)
        .set(AUDIT.TIME, time)
        .set(AUDIT.TYPE, "GROUP_CREATED")
        .set(AUDIT.USER_ID, userId)
        .set(AUDIT.MESSAGE, groupName.value())
        .execute();

      return groupName;
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(
        this.transaction(),
        e,
        EISDatabaseGroupsQueries::handleGroupCreationFailure
      );
    } finally {
      querySpan.end();
    }
  }

  @Override
  public void groupUserUpdate(
    final EIGroupName name,
    final UUID userId,
    final Set<EIGroupRole> roles)
    throws EISDatabaseException
  {
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(roles, "roles");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "EISDatabaseGroupsQueries.groupUserUpdate");

    try {
      final var groupIdExpression =
        context.select(GROUPS.ID)
          .from(GROUPS)
          .where(GROUPS.NAME.eq(name.value()));

      final var roleIndices =
        groupRolesToIntegers(roles);

      final var insert =
        context.insertInto(GROUP_ROLES)
          .set(GROUP_ROLES.USER_ID, userId)
          .set(GROUP_ROLES.GROUP_ID, groupIdExpression)
          .set(GROUP_ROLES.ROLES, roleIndices)
          .onDuplicateKeyUpdate()
          .set(GROUP_ROLES.ROLES, roleIndices);

      querySpan.setAttribute(DB_STATEMENT, insert.toString());

      insert.execute();
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(
        this.transaction(),
        e,
        EISDatabaseGroupsQueries::handleGroupUserUpdateFailure
      );
    } finally {
      querySpan.end();
    }
  }

  @Override
  public EISDatabaseGroupsPagedQueryType<EIGroupMembershipWithUser> groupRoles(
    final EIGroupName name,
    final long limit)
    throws EISDatabaseException
  {
    Objects.requireNonNull(name, "name");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "EISDatabaseGroupsQueries.groupRoles.create");

    try {
      final var allConditions =
        GROUPS.NAME.eq(name.value());

      final var baseTable =
        GROUP_ROLES.join(GROUPS).on(GROUPS.ID.eq(GROUP_ROLES.GROUP_ID));

      final var pages =
        JQKeysetRandomAccessPagination.createPageDefinitions(
          context,
          baseTable,
          List.of(
            new JQField(GROUP_ROLES.GROUP_ID, JQOrder.ASCENDING),
            new JQField(GROUP_ROLES.USER_ID, JQOrder.ASCENDING)
          ),
          List.of(allConditions),
          List.of(),
          limit,
          statement -> {
            querySpan.setAttribute(DB_STATEMENT, statement.toString());
          }
        );

      return new EISGroupRolesSearch(pages);
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(this.transaction(), e, DEFAULT_HANDLER);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public EISDatabaseGroupsPagedQueryType<EIGroupName>
  groupSearchByName(
    final EIGroupSearchByNameParameters parameters)
    throws EISDatabaseException
  {
    Objects.requireNonNull(parameters, "parameters");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "EISDatabaseGroupsQueries.groupSearchByName.create");

    try {
      final var allConditions =
        parameters.name()
          .map(n -> (Condition) GROUPS.NAME.like(n.toLowerCase()))
          .orElse(DSL.trueCondition());

      final var pages =
        JQKeysetRandomAccessPagination.createPageDefinitions(
          context,
          GROUPS,
          List.of(
            new JQField(GROUPS.NAME, JQOrder.ASCENDING)
          ),
          List.of(allConditions),
          List.of(),
          parameters.limit(),
          statement -> {
            querySpan.setAttribute(DB_STATEMENT, statement.toString());
          }
        );

      return new EISGroupByNameSearch(pages);
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(this.transaction(), e, DEFAULT_HANDLER);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public void groupCreationRequestStart(
    final EIGroupCreationRequest request)
    throws EISDatabaseException
  {
    Objects.requireNonNull(request, "request");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "EISDatabaseGroupsQueries.groupCreationRequestStart");

    try {
      final var userId =
        request.userFounder();
      final var groupName =
        request.groupName();
      final var token =
        request.token();

      final var timeNow = this.currentTime();
      context.insertInto(GROUPS_CREATION_REQUESTS)
        .set(GROUPS_CREATION_REQUESTS.CREATED, timeNow)
        .set(GROUPS_CREATION_REQUESTS.CREATOR_USER, userId)
        .set(GROUPS_CREATION_REQUESTS.GROUP_NAME, groupName.value())
        .set(GROUPS_CREATION_REQUESTS.GROUP_TOKEN, token.value())
        .set(GROUPS_CREATION_REQUESTS.STATUS, IN_PROGRESS.getLiteral())
        .set(GROUPS_CREATION_REQUESTS.MESSAGE, "")
        .execute();

      context.insertInto(AUDIT)
        .set(AUDIT.TIME, timeNow)
        .set(AUDIT.TYPE, "GROUP_CREATION_REQUESTED")
        .set(AUDIT.USER_ID, userId)
        .set(AUDIT.MESSAGE, "%s|%s".formatted(groupName, token))
        .execute();

    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(
        this.transaction(),
        e,
        EISDatabaseGroupsQueries::handleGroupCreationRequestStartFailure
      );
    } finally {
      querySpan.end();
    }
  }

  private static Optional<EISDatabaseException>
  handleGroupCreationRequestStartFailure(
    final DataAccessException ex)
  {
    final var state = ex.sqlState();
    if (Objects.equals(state, FOREIGN_KEY_VIOLATION.getState())) {
      if (ex.getMessage().contains("groups_creation_requests_creator_user_fkey")) {
        return Optional.of(USER_DOES_NOT_EXIST.get());
      }
    }

    if (Objects.equals(state, UNIQUE_VIOLATION.getState())) {
      if (ex.getMessage().contains("groups_creation_requests_pkey")) {
        return Optional.of(
          new EISDatabaseException(
            "Duplicate group request.", ex, GROUP_REQUEST_DUPLICATE)
        );
      }
    }

    return Optional.empty();
  }

  @Override
  public List<EIGroupCreationRequest> groupCreationRequestsForUser(
    final UUID userId)
    throws EISDatabaseException
  {
    Objects.requireNonNull(userId, "userId");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "EISDatabaseGroupsQueries.groupCreationRequestsForUser");

    try {
      return context.selectFrom(GROUPS_CREATION_REQUESTS)
        .where(GROUPS_CREATION_REQUESTS.CREATOR_USER.eq(userId))
        .orderBy(GROUPS_CREATION_REQUESTS.CREATED)
        .stream()
        .map(EISDatabaseGroupsQueries::mapCreationRequestRecord)
        .toList();
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(this.transaction(), e, DEFAULT_HANDLER);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public EISDatabaseGroupsPagedQueryType<EIGroupCreationRequest> groupCreationRequestsSearch(
    final EIGroupCreationRequestSearchParameters parameters)
    throws EISDatabaseException
  {
    Objects.requireNonNull(parameters, "parameters");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "EISDatabaseGroupsQueries.groupCreationRequestsSearch.create");

    try {
      final var conditions = new ArrayList<Condition>();
      parameters.owner().ifPresent(GROUPS_CREATION_REQUESTS.CREATOR_USER::eq);

      final var pages =
        JQKeysetRandomAccessPagination.createPageDefinitions(
          context,
          GROUPS_CREATION_REQUESTS,
          List.of(
            new JQField(GROUPS_CREATION_REQUESTS.CREATED, JQOrder.ASCENDING)
          ),
          conditions,
          List.of(),
          1000L,
          statement -> {
            querySpan.setAttribute(DB_STATEMENT, statement.toString());
          }
        );

      return new EISGroupCreationRequestsForUserSearch(pages);
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(this.transaction(), e, DEFAULT_HANDLER);
    } finally {
      querySpan.end();
    }
  }

  static EIGroupCreationRequestStatusType mapStatus(
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

  @Override
  public List<EIGroupCreationRequest> groupCreationRequestsObsolete()
    throws EISDatabaseException
  {
    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "EISDatabaseGroupsQueries.groupCreationRequestsObsolete");

    try {
      return context.selectFrom(
          GROUPS_CREATION_REQUESTS.join(GROUPS)
            .on(GROUPS_CREATION_REQUESTS.GROUP_NAME.eq(GROUPS.NAME)))
        .where(GROUPS_CREATION_REQUESTS.COMPLETED.isNull())
        .orderBy(GROUPS_CREATION_REQUESTS.CREATED)
        .stream()
        .map(EISDatabaseGroupsQueries::mapCreationRequestRecord)
        .toList();
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(this.transaction(), e, DEFAULT_HANDLER);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public List<EIGroupCreationRequest> groupCreationRequestsActive()
    throws EISDatabaseException
  {
    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "EISDatabaseGroupsQueries.groupCreationRequestsActive");

    try {
      return context.selectFrom(
          GROUPS_CREATION_REQUESTS
            .leftAntiJoin(GROUPS)
            .on(GROUPS_CREATION_REQUESTS.GROUP_NAME.eq(GROUPS.NAME))
        ).where(GROUPS_CREATION_REQUESTS.COMPLETED.isNull())
        .orderBy(GROUPS_CREATION_REQUESTS.CREATED)
        .stream()
        .map(EISDatabaseGroupsQueries::mapCreationRequestRecord)
        .toList();
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(this.transaction(), e, DEFAULT_HANDLER);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public Optional<EIGroupCreationRequest> groupCreationRequest(
    final EIToken token)
    throws EISDatabaseException
  {
    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "EISDatabaseGroupsQueries.groupCreationRequest");

    try {
      return context.selectFrom(GROUPS_CREATION_REQUESTS)
        .where(GROUPS_CREATION_REQUESTS.GROUP_TOKEN.eq(token.value()))
        .fetchOptional()
        .map(EISDatabaseGroupsQueries::mapCreationRequestRecord);
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(this.transaction(), e, DEFAULT_HANDLER);
    } finally {
      querySpan.end();
    }
  }

  private static void checkUserForRequest(
    final UUID userId,
    final GroupsCreationRequestsRecord existing)
    throws EISDatabaseException
  {
    final var requestUser =
      existing.get(GROUPS_CREATION_REQUESTS.CREATOR_USER);

    if (!Objects.equals(requestUser, userId)) {
      throw new EISDatabaseException(
        "Group request not owned by this user",
        GROUP_REQUEST_WRONG_USER
      );
    }
  }

  @Override
  public void groupCreationRequestComplete(
    final EIGroupCreationRequest request)
    throws EISDatabaseException
  {
    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "EISDatabaseGroupsQueries.groupCreationRequestComplete");

    try {
      final var userId = request.userFounder();
      final var groupName = request.groupName();
      final var token = request.token();

      final var existing =
        context.fetchOptional(
            GROUPS_CREATION_REQUESTS,
            GROUPS_CREATION_REQUESTS.GROUP_TOKEN.eq(token.value())
              .and(GROUPS_CREATION_REQUESTS.GROUP_NAME.eq(groupName.value())))
          .orElseThrow(() -> new EISDatabaseException(
            "Group request does not exist",
            GROUP_REQUEST_NONEXISTENT
          ));

      checkUserForRequest(userId, existing);

      final var status = request.status();
      if (status instanceof InProgress) {
        existing.set(
          GROUPS_CREATION_REQUESTS.STATUS,
          IN_PROGRESS.getLiteral());
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

        context.insertInto(AUDIT)
          .set(AUDIT.TIME, this.currentTime())
          .set(AUDIT.TYPE, "GROUP_CREATION_REQUEST_CANCELLED")
          .set(AUDIT.USER_ID, userId)
          .set(AUDIT.MESSAGE, "%s|%s".formatted(groupName, token))
          .execute();
        return;
      }

      if (status instanceof Failed failed) {
        existing.set(GROUPS_CREATION_REQUESTS.STATUS, FAILED.getLiteral());
        existing.set(
          GROUPS_CREATION_REQUESTS.COMPLETED,
          failed.timeCompletedValue());
        existing.set(GROUPS_CREATION_REQUESTS.MESSAGE, failed.message());
        existing.store();

        context.insertInto(AUDIT)
          .set(AUDIT.TIME, this.currentTime())
          .set(AUDIT.TYPE, "GROUP_CREATION_REQUEST_FAILED")
          .set(AUDIT.USER_ID, userId)
          .set(AUDIT.MESSAGE, "%s|%s".formatted(groupName, token))
          .execute();
        return;
      }

      if (status instanceof Succeeded succeeded) {
        existing.set(GROUPS_CREATION_REQUESTS.STATUS, SUCCEEDED.getLiteral());
        existing.set(
          GROUPS_CREATION_REQUESTS.COMPLETED,
          succeeded.timeCompletedValue());
        existing.set(GROUPS_CREATION_REQUESTS.MESSAGE, "");
        existing.store();

        context.insertInto(AUDIT)
          .set(AUDIT.TIME, this.currentTime())
          .set(AUDIT.TYPE, "GROUP_CREATION_REQUEST_SUCCEEDED")
          .set(AUDIT.USER_ID, userId)
          .set(AUDIT.MESSAGE, "%s|%s".formatted(groupName, token))
          .execute();

        final var groupOpt =
          context.selectFrom(GROUPS)
            .where(GROUPS.NAME.eq(groupName.value()))
            .fetchOptional();

        if (groupOpt.isEmpty()) {
          this.groupCreate(
            userId, groupName);
          this.groupUserUpdate(
            groupName, userId, EnumSet.allOf(EIGroupRole.class));
        }
      }
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(this.transaction(), e, DEFAULT_HANDLER);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public EISDatabaseGroupsPagedQueryType<EIGroupMembership> groupUserRoles(
    final UUID userId)
    throws EISDatabaseException
  {
    Objects.requireNonNull(userId, "userId");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "EISDatabaseGroupsQueries.groupUserRoles.create");

    try {
      final var table =
        GROUPS.join(GROUP_ROLES).on(GROUPS.ID.eq(GROUP_ROLES.GROUP_ID));

      final var pages =
        JQKeysetRandomAccessPagination.createPageDefinitions(
          context,
          table,
          List.of(
            new JQField(GROUPS.NAME, JQOrder.ASCENDING)
          ),
          List.of(
            DSL.condition(GROUP_ROLES.USER_ID.eq(userId))
          ),
          List.of(),
          1000L,
          statement -> {
            querySpan.setAttribute(DB_STATEMENT, statement.toString());
          }
        );

      return new EISGroupUserRolesSearch(pages);
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(this.transaction(), e, DEFAULT_HANDLER);
    } finally {
      querySpan.end();
    }
  }
}
