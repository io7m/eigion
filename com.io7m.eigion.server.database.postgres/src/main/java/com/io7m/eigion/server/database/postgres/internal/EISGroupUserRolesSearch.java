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
import com.io7m.eigion.model.EIPage;
import com.io7m.eigion.server.database.api.EISDatabaseException;
import com.io7m.eigion.server.database.api.EISDatabaseGroupsPagedQueryType;
import com.io7m.eigion.server.database.api.EISDatabaseGroupsQueriesType;
import com.io7m.jqpage.core.JQKeysetRandomAccessPageDefinition;
import org.jooq.exception.DataAccessException;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.io7m.eigion.server.database.postgres.internal.EISDatabaseExceptions.DEFAULT_HANDLER;
import static com.io7m.eigion.server.database.postgres.internal.EISDatabaseExceptions.handleDatabaseException;
import static com.io7m.eigion.server.database.postgres.internal.Tables.GROUPS;
import static com.io7m.eigion.server.database.postgres.internal.Tables.GROUP_ROLES;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.DB_STATEMENT;

final class EISGroupUserRolesSearch
  extends EISAbstractSearch<EISDatabaseGroupsQueries, EISDatabaseGroupsQueriesType, EIGroupMembership>
  implements EISDatabaseGroupsPagedQueryType<EIGroupMembership>
{
  EISGroupUserRolesSearch(
    final List<JQKeysetRandomAccessPageDefinition> inPages)
  {
    super(inPages);
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
        "EISDatabaseGroupsQueries.groupUserRoles.page");

    try {
      final var select =
        page.queryFields(context, List.of(
          GROUPS.NAME,
          GROUP_ROLES.ROLES
        ));

      querySpan.setAttribute(DB_STATEMENT, select.toString());

      final var items =
        select.fetch().map(record -> {
          return new EIGroupMembership(
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
