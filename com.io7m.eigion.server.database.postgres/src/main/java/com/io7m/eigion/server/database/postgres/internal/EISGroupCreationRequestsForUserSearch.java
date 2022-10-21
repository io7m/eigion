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
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIPage;
import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.server.database.api.EISDatabaseException;
import com.io7m.eigion.server.database.api.EISDatabaseGroupsPagedQueryType;
import com.io7m.eigion.server.database.api.EISDatabaseGroupsQueriesType;
import com.io7m.jqpage.core.JQKeysetRandomAccessPageDefinition;
import org.jooq.exception.DataAccessException;

import java.util.List;

import static com.io7m.eigion.server.database.postgres.internal.EISDatabaseExceptions.DEFAULT_HANDLER;
import static com.io7m.eigion.server.database.postgres.internal.EISDatabaseExceptions.handleDatabaseException;
import static com.io7m.eigion.server.database.postgres.internal.Tables.GROUPS_CREATION_REQUESTS;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.DB_STATEMENT;

final class EISGroupCreationRequestsForUserSearch
  extends EISAbstractSearch<EISDatabaseGroupsQueries, EISDatabaseGroupsQueriesType, EIGroupCreationRequest>
  implements EISDatabaseGroupsPagedQueryType<EIGroupCreationRequest>
{
  EISGroupCreationRequestsForUserSearch(
    final List<JQKeysetRandomAccessPageDefinition> inPages)
  {
    super(inPages);
  }

  @Override
  protected EIPage<EIGroupCreationRequest> page(
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
        "EISDatabaseGroupsQueries.groupCreationRequestsSearch.page");

    try {
      final var select =
        page.query(context);

      querySpan.setAttribute(DB_STATEMENT, select.toString());

      final var items =
        select.fetch().map(record -> {
          return new EIGroupCreationRequest(
            new EIGroupName(record.get(GROUPS_CREATION_REQUESTS.GROUP_NAME)),
            record.get(GROUPS_CREATION_REQUESTS.CREATOR_USER),
            new EIToken(record.get(GROUPS_CREATION_REQUESTS.GROUP_TOKEN)),
            EISDatabaseGroupsQueries.mapStatus(record)
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
