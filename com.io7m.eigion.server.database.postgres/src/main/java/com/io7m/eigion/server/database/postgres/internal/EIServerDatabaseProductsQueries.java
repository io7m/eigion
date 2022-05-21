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

import com.io7m.eigion.model.EIProduct;
import com.io7m.eigion.model.EIProductCategory;
import com.io7m.eigion.model.EIProductIdentifier;
import com.io7m.eigion.model.EIRedaction;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseProductsQueriesType;
import com.io7m.eigion.server.database.postgres.internal.tables.records.AuditRecord;
import com.io7m.eigion.server.database.postgres.internal.tables.records.CategoriesRecord;
import com.io7m.eigion.server.database.postgres.internal.tables.records.ProductsRecord;
import com.io7m.jaffirm.core.Postconditions;
import com.io7m.jaffirm.core.Preconditions;
import org.jooq.DSLContext;
import org.jooq.InsertSetMoreStep;
import org.jooq.exception.DataAccessException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.io7m.eigion.server.database.api.EIServerDatabaseProductsQueriesType.IncludeRedacted.INCLUDE_REDACTED;
import static com.io7m.eigion.server.database.postgres.internal.Tables.PRODUCTS;
import static com.io7m.eigion.server.database.postgres.internal.Tables.PRODUCT_CATEGORIES;
import static com.io7m.eigion.server.database.postgres.internal.tables.Audit.AUDIT;
import static com.io7m.eigion.server.database.postgres.internal.tables.Categories.CATEGORIES;

record EIServerDatabaseProductsQueries(
  EIServerDatabaseTransaction transaction)
  implements EIServerDatabaseProductsQueriesType
{
  private static ProductsRecord fetchProductOrFail(
    final EIProductIdentifier id,
    final IncludeRedacted includeRedacted,
    final DSLContext context)
    throws EIServerDatabaseException
  {
    final var productOpt =
      switch (includeRedacted) {
        case INCLUDE_REDACTED -> {
          yield context.fetchOptional(
            PRODUCTS,
            PRODUCTS.PRODUCT_GROUP.eq(id.group())
              .and(PRODUCTS.PRODUCT_NAME.eq(id.name()))
          );
        }
        case EXCLUDE_REDACTED -> {
          yield context.fetchOptional(
            PRODUCTS,
            PRODUCTS.PRODUCT_GROUP.eq(id.group())
              .and(PRODUCTS.PRODUCT_NAME.eq(id.name()))
              .and(PRODUCTS.REDACTED.isNull())
          );
        }
      };

    if (productOpt.isEmpty()) {
      throw new EIServerDatabaseException(
        "Product does not exist",
        "product-nonexistent"
      );
    }

    return productOpt.get();
  }

  private static CategoriesRecord fetchCategoryOrFail(
    final EIProductCategory category,
    final IncludeRedacted includeRedacted,
    final DSLContext context)
    throws EIServerDatabaseException
  {
    final var categoryOpt =
      switch (includeRedacted) {
        case INCLUDE_REDACTED -> {
          yield context.fetchOptional(
            CATEGORIES,
            CATEGORIES.NAME.eq(category.value())
          );
        }
        case EXCLUDE_REDACTED -> {
          yield context.fetchOptional(
            CATEGORIES,
            CATEGORIES.NAME.eq(category.value())
              .and(CATEGORIES.REDACTED.isNull())
          );
        }
      };

    if (categoryOpt.isEmpty()) {
      throw new EIServerDatabaseException(
        "Category does not exist",
        "category-nonexistent"
      );
    }

    return categoryOpt.get();
  }

  private static EIProductIdentifier toProductId(
    final ProductsRecord r)
  {
    return new EIProductIdentifier(r.getProductGroup(), r.getProductName());
  }

  private static String redactionReason(
    final Optional<EIRedaction> redacted,
    final EIProductIdentifier id)
  {
    return redacted.map(r -> id.show() + ": " + r.reason())
      .orElseGet(id::show);
  }

  private static EIProductCategory toCategory(
    final CategoriesRecord r)
  {
    return new EIProductCategory(
      r.get(CATEGORIES.NAME)
    );
  }

  private static void insertAuditRecord(
    final InsertSetMoreStep<AuditRecord> audit)
  {
    final var inserted = audit.execute();
    Postconditions.checkPostconditionV(
      inserted == 1,
      "Expected to insert one audit record (inserted %d)",
      Integer.valueOf(inserted)
    );
  }

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
          yield context.fetch(CATEGORIES, CATEGORIES.REDACTED.isNull())
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
        final var inserted =
          context.insertInto(CATEGORIES)
            .set(CATEGORIES.NAME, text)
            .set(CATEGORIES.REDACTED, (String) null)
            .execute();

        Preconditions.checkPreconditionV(
          inserted == 1,
          "Expected to insert 1 record (inserted %d)",
          Integer.valueOf(inserted)
        );
      }

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, OffsetDateTime.now(this.transaction.clock()))
          .set(AUDIT.TYPE, "CATEGORY_CREATED")
          .set(AUDIT.MESSAGE, text);

      insertAuditRecord(audit);
      return new EIProductCategory(text);
    } catch (final DataAccessException e) {
      throw new EIServerDatabaseException(e.getMessage(), e, "sql-error");
    }
  }

  @Override
  public EIProductCategory categoryRedact(
    final String category,
    final Optional<EIRedaction> redacted)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(category, "category");
    Objects.requireNonNull(redacted, "redacted");

    final var context = this.transaction.createContext();

    try {
      final var existingOpt =
        context.fetchOptional(CATEGORIES, CATEGORIES.NAME.eq(category));

      if (existingOpt.isEmpty()) {
        throw new EIServerDatabaseException(
          "No such category: " + category, "category-nonexistent");
      }

      final var existing = existingOpt.get();
      existing.setRedacted(redacted.map(EIRedaction::reason).orElse(null));
      existing.store();

      final var redactType =
        redacted.isPresent() ? "CATEGORY_REDACTED" : "CATEGORY_UNREDACTED";
      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, OffsetDateTime.now(this.transaction.clock()))
          .set(AUDIT.TYPE, redactType)
          .set(AUDIT.MESSAGE, category);

      insertAuditRecord(audit);
      return new EIProductCategory(category);
    } catch (final DataAccessException e) {
      throw new EIServerDatabaseException(e.getMessage(), e, "sql-error");
    }
  }

  @Override
  public EIProduct productCreate(
    final EIProductIdentifier id,
    final UUID userId)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(userId, "userId");

    final var context = this.transaction.createContext();

    try {
      final var existing =
        context.fetchOptional(
          PRODUCTS,
          PRODUCTS.PRODUCT_GROUP.eq(id.group())
            .and(PRODUCTS.PRODUCT_NAME.eq(id.name()))
        );
      if (existing.isPresent()) {
        throw new EIServerDatabaseException(
          "Product already exists",
          "product-duplicate"
        );
      }

      final var inserted =
        context.insertInto(PRODUCTS)
          .set(PRODUCTS.PRODUCT_GROUP, id.group())
          .set(PRODUCTS.PRODUCT_NAME, id.name())
          .set(PRODUCTS.CREATED_BY, userId)
          .execute();

      Preconditions.checkPreconditionV(
        inserted == 1,
        "Expected to insert 1 record (inserted %d)",
        Integer.valueOf(inserted)
      );

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, OffsetDateTime.now(this.transaction.clock()))
          .set(AUDIT.TYPE, "PRODUCT_CREATED")
          .set(AUDIT.MESSAGE, id.show());

      insertAuditRecord(audit);
      return new EIProduct(id, List.of(), Set.of());
    } catch (final DataAccessException e) {
      throw new EIServerDatabaseException(e.getMessage(), e, "sql-error");
    }
  }

  @Override
  public void productRedact(
    final EIProductIdentifier id,
    final Optional<EIRedaction> redacted)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(redacted, "redacted");

    final var context = this.transaction.createContext();

    try {
      final var existing =
        fetchProductOrFail(id, INCLUDE_REDACTED, context);

      existing.setRedacted(redacted.map(EIRedaction::reason).orElse(null));
      existing.store();

      final var auditType =
        redacted.isPresent() ? "PRODUCT_REDACTED" : "PRODUCT_UNREDACTED";

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, OffsetDateTime.now(this.transaction.clock()))
          .set(AUDIT.TYPE, auditType)
          .set(AUDIT.MESSAGE, redactionReason(redacted, id));

      insertAuditRecord(audit);
    } catch (final DataAccessException e) {
      throw new EIServerDatabaseException(e.getMessage(), e, "sql-error");
    }
  }

  @Override
  public Set<EIProductIdentifier> productsAll(
    final IncludeRedacted includeRedacted)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(includeRedacted, "includeRedacted");

    final var context = this.transaction.createContext();

    try {
      return switch (includeRedacted) {
        case INCLUDE_REDACTED -> {
          try (var select = context.selectFrom(PRODUCTS)) {
            yield select.stream()
              .map(EIServerDatabaseProductsQueries::toProductId)
              .collect(Collectors.toUnmodifiableSet());
          }
        }
        case EXCLUDE_REDACTED -> {
          try (var select = context.selectFrom(PRODUCTS)) {
            yield select.where(PRODUCTS.REDACTED.isNull())
              .stream()
              .map(EIServerDatabaseProductsQueries::toProductId)
              .collect(Collectors.toUnmodifiableSet());
          }
        }
      };
    } catch (final DataAccessException e) {
      throw new EIServerDatabaseException(e.getMessage(), e, "sql-error");
    }
  }

  @Override
  public EIProduct product(
    final EIProductIdentifier id,
    final IncludeRedacted includeRedacted)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(includeRedacted, "includeRedacted");

    final var context = this.transaction.createContext();

    try {
      final var productRec =
        fetchProductOrFail(id, includeRedacted, context);

      final var categoryRecords =
        switch (includeRedacted) {
          case INCLUDE_REDACTED -> {
            try (var select = context.select()) {
              yield select.from(CATEGORIES)
                .join(PRODUCT_CATEGORIES)
                .on(PRODUCT_CATEGORIES.CATEGORY_PRODUCT.eq(productRec.getId()))
                .fetch();
            }
          }
          case EXCLUDE_REDACTED -> {
            try (var select = context.select()) {
              yield select.from(CATEGORIES)
                .join(PRODUCT_CATEGORIES)
                .on(PRODUCT_CATEGORIES.CATEGORY_PRODUCT.eq(productRec.getId()))
                .where(CATEGORIES.REDACTED.isNull())
                .fetch();
            }
          }
        };

      final var categories =
        categoryRecords.stream()
          .map(r -> new EIProductCategory(r.get(CATEGORIES.NAME)))
          .collect(Collectors.toUnmodifiableSet());

      return new EIProduct(
        new EIProductIdentifier(
          productRec.getProductGroup(),
          productRec.getProductName()),
        List.of(),
        categories
      );
    } catch (final DataAccessException e) {
      throw new EIServerDatabaseException(e.getMessage(), e, "sql-error");
    }
  }

  @Override
  public void productCategoryAdd(
    final EIProductIdentifier id,
    final EIProductCategory category)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(category, "category");

    final var context =
      this.transaction.createContext();

    try {
      final var productRec =
        fetchProductOrFail(id, INCLUDE_REDACTED, context);
      final var categoryRec =
        fetchCategoryOrFail(category, INCLUDE_REDACTED, context);

      final var existingOpt =
        context.fetchOptional(
          PRODUCT_CATEGORIES,
          PRODUCT_CATEGORIES.CATEGORY_PRODUCT.eq(productRec.getId())
            .and(PRODUCT_CATEGORIES.CATEGORY_ID.eq(categoryRec.getId()))
        );

      if (existingOpt.isPresent()) {
        return;
      }

      final var inserted =
        context.insertInto(PRODUCT_CATEGORIES)
          .set(PRODUCT_CATEGORIES.CATEGORY_PRODUCT, productRec.getId())
          .set(PRODUCT_CATEGORIES.CATEGORY_ID, categoryRec.getId())
          .execute();

      Preconditions.checkPreconditionV(
        inserted == 1,
        "Expected to insert 1 record (inserted %d)",
        Integer.valueOf(inserted)
      );

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, OffsetDateTime.now(this.transaction.clock()))
          .set(AUDIT.TYPE, "PRODUCT_CATEGORY_ADDED")
          .set(AUDIT.MESSAGE, id.show() + ":" + category.value());

      insertAuditRecord(audit);
    } catch (final DataAccessException e) {
      throw new EIServerDatabaseException(e.getMessage(), e, "sql-error");
    }
  }

  @Override
  public void productCategoryRemove(
    final EIProductIdentifier id,
    final EIProductCategory category)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(category, "category");

    final var context =
      this.transaction.createContext();

    try {
      final var productRec =
        fetchProductOrFail(id, INCLUDE_REDACTED, context);
      final var categoryRec =
        fetchCategoryOrFail(category, INCLUDE_REDACTED, context);

      final var existingOpt =
        context.fetchOptional(
          PRODUCT_CATEGORIES,
          PRODUCT_CATEGORIES.CATEGORY_PRODUCT.eq(productRec.getId())
            .and(PRODUCT_CATEGORIES.CATEGORY_ID.eq(categoryRec.getId()))
        );

      if (existingOpt.isEmpty()) {
        return;
      }

      final var deleted =
        context.delete(PRODUCT_CATEGORIES)
          .where(PRODUCT_CATEGORIES.CATEGORY_PRODUCT.eq(productRec.getId()))
          .and(PRODUCT_CATEGORIES.CATEGORY_ID.eq(categoryRec.getId()))
          .execute();

      Postconditions.checkPostconditionV(
        deleted == 1,
        "Expected to delete one product category (deleted %d)",
        Integer.valueOf(deleted)
      );

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, OffsetDateTime.now(this.transaction.clock()))
          .set(AUDIT.TYPE, "PRODUCT_CATEGORY_REMOVED")
          .set(AUDIT.MESSAGE, id.show() + ":" + category.value());

      insertAuditRecord(audit);
    } catch (final DataAccessException e) {
      throw new EIServerDatabaseException(e.getMessage(), e, "sql-error");
    }
  }
}
