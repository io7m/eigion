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

import com.io7m.eigion.model.EIGroupMembership;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIGroupRole;
import com.io7m.eigion.model.EIGroupRoleSet;
import com.io7m.eigion.model.EIGroupSearchByNameParameters;
import com.io7m.eigion.model.EIPage;
import com.io7m.eigion.server.database.api.EISDatabaseException;
import com.io7m.eigion.server.database.api.EISDatabaseGroupByNameSearchType;
import com.io7m.eigion.server.database.api.EISDatabaseGroupRolesSearchType;
import com.io7m.eigion.server.database.api.EISDatabaseGroupsQueriesType;
import com.io7m.eigion.server.database.api.EISDatabasePagedQueryType;
import com.io7m.jqpage.core.JQField;
import com.io7m.jqpage.core.JQKeysetRandomAccessPageDefinition;
import com.io7m.jqpage.core.JQKeysetRandomAccessPagination;
import com.io7m.jqpage.core.JQOrder;
import org.jooq.Condition;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.GROUP_DUPLICATE;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.GROUP_NONEXISTENT;
import static com.io7m.eigion.server.database.postgres.internal.EISDatabaseExceptions.DEFAULT_HANDLER;
import static com.io7m.eigion.server.database.postgres.internal.EISDatabaseExceptions.handleDatabaseException;
import static com.io7m.eigion.server.database.postgres.internal.EISDatabaseUsersQueries.USER_DOES_NOT_EXIST;
import static com.io7m.eigion.server.database.postgres.internal.Tables.AUDIT;
import static com.io7m.eigion.server.database.postgres.internal.Tables.GROUPS;
import static com.io7m.eigion.server.database.postgres.internal.Tables.GROUP_ROLES;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.DB_STATEMENT;
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

  private static EIGroupRoleSet groupRolesFromIntegers(
    final Integer[] integers)
  {
    return EIGroupRoleSet.of(
      Stream.of(integers)
        .map(i -> EIGroupRole.ofIndex(i.intValue()))
        .collect(Collectors.toUnmodifiableSet())
    );
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
  public EISDatabasePagedQueryType<EISDatabaseGroupsQueriesType, EIGroupMembership> groupRoles(
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

      return new GroupRolesSearch(pages);
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(this.transaction(), e, DEFAULT_HANDLER);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public EISDatabasePagedQueryType<EISDatabaseGroupsQueriesType, EIGroupName>
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

      return new GroupByNameSearch(pages);
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(this.transaction(), e, DEFAULT_HANDLER);
    } finally {
      querySpan.end();
    }
  }

  private static final class GroupRolesSearch
    extends EISAbstractSearch<EISDatabaseGroupsQueries, EISDatabaseGroupsQueriesType, EIGroupMembership>
    implements EISDatabaseGroupRolesSearchType
  {
    GroupRolesSearch(
      final List<JQKeysetRandomAccessPageDefinition> inPages)
    {
      super(inPages);
    }

    @Override
    protected EIPage<EIGroupMembership> page(
      final EISDatabaseGroupsQueries queries,
      final JQKeysetRandomAccessPageDefinition page)
      throws EISDatabaseException
    {
      final var transaction =
        queries.transaction();
      final var context =
        transaction.createContext();

      final var querySpan =
        transaction.createQuerySpan(
          "EISDatabaseGroupsQueries.groupRoles.page");

      try {
        final var select =
          page.queryFields(context, List.of(
            GROUPS.NAME,
            GROUP_ROLES.ROLES,
            GROUP_ROLES.USER_ID
          ));

        querySpan.setAttribute(DB_STATEMENT, select.toString());

        final var items =
          select.fetch().map(record -> {
            return new EIGroupMembership(
              record.get(GROUP_ROLES.USER_ID),
              new EIGroupName(record.get(GROUPS.NAME)),
              groupRolesFromIntegers(record.get(GROUP_ROLES.ROLES))
            );
          });

        return new EIPage<>(
          items,
          (int) page.index(),
          this.pageCount(),
          page.firstOffset()
        );
      } catch (final DataAccessException e) {
        querySpan.recordException(e);
        throw handleDatabaseException(transaction, e, DEFAULT_HANDLER);
      } finally {
        querySpan.end();
      }
    }
  }

  private static final class GroupByNameSearch
    extends EISAbstractSearch<EISDatabaseGroupsQueries, EISDatabaseGroupsQueriesType, EIGroupName>
    implements EISDatabaseGroupByNameSearchType
  {
    GroupByNameSearch(
      final List<JQKeysetRandomAccessPageDefinition> inPages)
    {
      super(inPages);
    }

    @Override
    protected EIPage<EIGroupName> page(
      final EISDatabaseGroupsQueries queries,
      final JQKeysetRandomAccessPageDefinition page)
      throws EISDatabaseException
    {
      final var transaction =
        queries.transaction();
      final var context =
        transaction.createContext();

      final var querySpan =
        transaction.createQuerySpan(
          "EISDatabaseGroupsQueries.groupSearchByName.page");

      try {
        final var select =
          page.queryFields(context, List.of(GROUPS.NAME));

        querySpan.setAttribute(DB_STATEMENT, select.toString());

        final var items =
          select.fetch().map(record -> {
            return new EIGroupName(record.get(GROUPS.NAME));
          });

        return new EIPage<>(
          items,
          (int) page.index(),
          this.pageCount(),
          page.firstOffset()
        );
      } catch (final DataAccessException e) {
        querySpan.recordException(e);
        throw handleDatabaseException(transaction, e, DEFAULT_HANDLER);
      } finally {
        querySpan.end();
      }
    }
  }
}
