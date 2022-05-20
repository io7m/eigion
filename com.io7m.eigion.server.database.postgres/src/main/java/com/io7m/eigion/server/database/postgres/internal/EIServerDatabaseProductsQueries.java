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

import com.io7m.eigion.model.EIProductCategory;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseProductsQueriesType;
import com.io7m.eigion.server.database.postgres.internal.tables.records.CategoriesRecord;
import org.jooq.exception.DataAccessException;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.io7m.eigion.server.database.postgres.internal.tables.Audit.AUDIT;
import static com.io7m.eigion.server.database.postgres.internal.tables.Categories.CATEGORIES;
import static java.lang.Boolean.FALSE;

record EIServerDatabaseProductsQueries(
  EIServerDatabaseTransaction transaction)
  implements EIServerDatabaseProductsQueriesType
{
  @Override
  public Set<EIProductCategory> categories(
    final IncludeRedacted includeRedacted)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(includeRedacted, "includeRedacted");

    final var context = this.transaction.createContext();

    try {
      return switch (includeRedacted) {
        case INCLUDE_REDACTED -> {
          yield context.fetch(CATEGORIES)
            .stream()
            .map(EIServerDatabaseProductsQueries::toCategory)
            .collect(Collectors.toUnmodifiableSet());
        }
        case EXCLUDE_REDACTED -> {
          yield context.fetch(CATEGORIES, CATEGORIES.REDACTED.eq(FALSE))
            .stream()
            .map(EIServerDatabaseProductsQueries::toCategory)
            .collect(Collectors.toUnmodifiableSet());
        }
      };
    } catch (final DataAccessException e) {
      throw new EIServerDatabaseException(e.getMessage(), e, "sql-error");
    }
  }

  @Override
  public EIProductCategory categoryCreate(
    final String text)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(text, "text");

    final var context = this.transaction.createContext();

    try {
      final var existingOpt =
        context.fetchOptional(CATEGORIES, CATEGORIES.NAME.eq(text));

      if (existingOpt.isEmpty()) {
        context.insertInto(CATEGORIES)
          .set(CATEGORIES.NAME, text)
          .set(CATEGORIES.REDACTED, FALSE)
          .execute();
      }

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, OffsetDateTime.now(this.transaction.clock()))
          .set(AUDIT.TYPE, "CATEGORY_CREATED")
          .set(AUDIT.MESSAGE, text);

      audit.execute();
      return new EIProductCategory(text);
    } catch (final DataAccessException e) {
      throw new EIServerDatabaseException(e.getMessage(), e, "sql-error");
    }
  }

  @Override
  public EIProductCategory categoryRedact(
    final String category,
    final boolean redacted)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(category, "category");

    final var context = this.transaction.createContext();

    try {
      final var existingOpt =
        context.fetchOptional(CATEGORIES, CATEGORIES.NAME.eq(category));

      if (existingOpt.isEmpty()) {
        throw new EIServerDatabaseException(
          "No such category: " + category, "category-nonexistent");
      }

      final var existing = existingOpt.get();
      existing.setRedacted(Boolean.valueOf(redacted));
      existing.store();

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, OffsetDateTime.now(this.transaction.clock()))
          .set(AUDIT.TYPE, redacted ? "CATEGORY_REDACTED" : "CATEGORY_UNREDACTED")
          .set(AUDIT.MESSAGE, category);

      audit.execute();
      return new EIProductCategory(category);
    } catch (final DataAccessException e) {
      throw new EIServerDatabaseException(e.getMessage(), e, "sql-error");
    }
  }

  private static EIProductCategory toCategory(
    final CategoriesRecord r)
  {
    return new EIProductCategory(
      r.get(CATEGORIES.NAME)
    );
  }
}
