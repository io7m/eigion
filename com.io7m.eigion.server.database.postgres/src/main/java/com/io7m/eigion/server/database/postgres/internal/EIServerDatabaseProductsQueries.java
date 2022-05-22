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

import com.io7m.eigion.model.EILink;
import com.io7m.eigion.model.EIProduct;
import com.io7m.eigion.model.EIProductCategory;
import com.io7m.eigion.model.EIProductDescription;
import com.io7m.eigion.model.EIProductIdentifier;
import com.io7m.eigion.model.EIProductRelease;
import com.io7m.eigion.model.EIRedactableType;
import com.io7m.eigion.model.EIRedaction;
import com.io7m.eigion.model.EIRedactionRequest;
import com.io7m.eigion.model.EIRichText;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseIncludeRedacted;
import com.io7m.eigion.server.database.api.EIServerDatabaseProductsQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseRequiresUser;
import com.io7m.eigion.server.database.api.EIServerDatabaseUsersQueriesType;
import com.io7m.eigion.server.database.postgres.internal.tables.records.AuditRecord;
import com.io7m.eigion.server.database.postgres.internal.tables.records.ProductLinksRecord;
import com.io7m.jaffirm.core.Postconditions;
import com.io7m.jaffirm.core.Preconditions;
import com.io7m.junreachable.UnimplementedCodeException;
import org.jooq.DSLContext;
import org.jooq.InsertSetMoreStep;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.io7m.eigion.server.database.api.EIServerDatabaseIncludeRedacted.INCLUDE_REDACTED;
import static com.io7m.eigion.server.database.postgres.internal.EIServerDatabaseExceptions.handleDatabaseException;
import static com.io7m.eigion.server.database.postgres.internal.EIServerDatabaseProductsQueries.ProductInformationComponents.INCLUDE_CATEGORIES;
import static com.io7m.eigion.server.database.postgres.internal.EIServerDatabaseProductsQueries.ProductInformationComponents.INCLUDE_DESCRIPTION;
import static com.io7m.eigion.server.database.postgres.internal.Tables.CATEGORY_REDACTIONS;
import static com.io7m.eigion.server.database.postgres.internal.Tables.PRODUCTS;
import static com.io7m.eigion.server.database.postgres.internal.Tables.PRODUCT_CATEGORIES;
import static com.io7m.eigion.server.database.postgres.internal.Tables.PRODUCT_LINKS;
import static com.io7m.eigion.server.database.postgres.internal.Tables.PRODUCT_REDACTIONS;
import static com.io7m.eigion.server.database.postgres.internal.tables.Audit.AUDIT;
import static com.io7m.eigion.server.database.postgres.internal.tables.Categories.CATEGORIES;
import static java.lang.Long.valueOf;

final class EIServerDatabaseProductsQueries
  implements EIServerDatabaseProductsQueriesType
{
  private static final EnumSet<ProductInformationComponents> INCLUDE_NOTHING =
    EnumSet.noneOf(ProductInformationComponents.class);

  private final EIServerDatabaseTransaction transaction;
  private final EIServerDatabaseUsersQueriesType users;

  EIServerDatabaseProductsQueries(
    final EIServerDatabaseTransaction inTransaction,
    final EIServerDatabaseUsersQueriesType inUsers)
  {
    this.transaction =
      Objects.requireNonNull(inTransaction, "transaction");
    this.users =
      Objects.requireNonNull(inUsers, "users");
  }

  private static List<EIDatabaseProduct> fetchProducts(
    final EIServerDatabaseIncludeRedacted includeRedacted,
    final DSLContext context)
  {
    return context.select()
      .from(PRODUCTS)
      .leftOuterJoin(PRODUCT_REDACTIONS)
      .on(PRODUCT_REDACTIONS.PRODUCT.eq(PRODUCTS.ID))
      .stream()
      .map(r -> {
        final var descriptionText =
          new EIRichText(
            r.get(PRODUCTS.PRODUCT_DESCRIPTION_TYPE),
            r.get(PRODUCTS.PRODUCT_DESCRIPTION)
          );

        final var description =
          new EIProductDescription(
            r.get(PRODUCTS.PRODUCT_TITLE),
            descriptionText,
            Set.of(),
            List.of()
          );

        return toProductWithRedaction(r, description);
      })
      .filter(r -> filterRedactedIfNecessary(r, includeRedacted))
      .toList();
  }

  private static Optional<EIDatabaseProduct> fetchProductOptional(
    final EIProductIdentifier id,
    final EIServerDatabaseIncludeRedacted includeRedacted,
    final EnumSet<ProductInformationComponents> includes,
    final DSLContext context)
  {
    final var productRec =
      context.select()
        .from(PRODUCTS)
        .leftOuterJoin(PRODUCT_REDACTIONS)
        .on(PRODUCT_REDACTIONS.PRODUCT.eq(PRODUCTS.ID))
        .where(
          PRODUCTS.PRODUCT_GROUP.eq(id.group())
            .and(PRODUCTS.PRODUCT_NAME.eq(id.name()))
        )
        .fetchAny();

    if (productRec == null) {
      return Optional.empty();
    }

    final var productId =
      productRec.get(PRODUCTS.ID);

    final var descriptionText =
      new EIRichText(
        productRec.get(PRODUCTS.PRODUCT_DESCRIPTION_TYPE),
        productRec.get(PRODUCTS.PRODUCT_DESCRIPTION)
      );

    final var categories =
      fetchProductCategories(productId, includeRedacted, includes, context);
    final var links =
      fetchProductLinks(productId, includes, context);

    final var description =
      new EIProductDescription(
        productRec.get(PRODUCTS.PRODUCT_TITLE),
        descriptionText,
        categories.stream()
          .map(EIDatabaseProductCategory::category)
          .collect(Collectors.toUnmodifiableSet()),
        links
      );

    final var product =
      toProductWithRedaction(productRec, description);

    if (!filterRedactedIfNecessary(product, includeRedacted)) {
      return Optional.empty();
    }

    return Optional.of(product);
  }

  private static Set<EIDatabaseProductCategory> fetchProductCategories(
    final Long productId,
    final EIServerDatabaseIncludeRedacted includeRedacted,
    final EnumSet<ProductInformationComponents> includes,
    final DSLContext context)
  {
    if (includes.contains(INCLUDE_CATEGORIES)) {
      return context.select()
        .from(CATEGORIES)
        .leftOuterJoin(CATEGORY_REDACTIONS)
        .on(CATEGORY_REDACTIONS.CATEGORY.eq(CATEGORIES.ID))
        .join(PRODUCT_CATEGORIES)
        .on(PRODUCT_CATEGORIES.CATEGORY_ID.eq(CATEGORIES.ID))
        .join(PRODUCTS)
        .on(PRODUCT_CATEGORIES.CATEGORY_PRODUCT.eq(productId))
        .where(PRODUCT_CATEGORIES.CATEGORY_PRODUCT.eq(productId))
        .stream()
        .map(EIServerDatabaseProductsQueries::toCategoryWithRedaction)
        .filter(c -> filterRedactedIfNecessary(c, includeRedacted))
        .collect(Collectors.toUnmodifiableSet());
    }
    return Set.of();
  }

  private static List<EILink> fetchProductLinks(
    final Long productId,
    final EnumSet<ProductInformationComponents> includes,
    final DSLContext context)
  {
    if (includes.contains(INCLUDE_DESCRIPTION)) {
      return context.selectFrom(PRODUCT_LINKS)
        .where(PRODUCT_LINKS.PRODUCT.eq(productId))
        .stream()
        .map(EIServerDatabaseProductsQueries::toLink)
        .toList();
    }
    return List.of();
  }

  private static EILink toLink(
    final ProductLinksRecord rec)
  {
    return new EILink(
      rec.getLinkRelation(),
      URI.create(rec.getLinkLocation())
    );
  }

  private static EIDatabaseProduct fetchProductOrFail(
    final EIProductIdentifier id,
    final EIServerDatabaseIncludeRedacted includeRedacted,
    final EnumSet<ProductInformationComponents> includes,
    final DSLContext context)
    throws EIServerDatabaseException
  {
    return fetchProductOptional(id, includeRedacted, includes, context)
      .orElseThrow(() -> {
        return new EIServerDatabaseException(
          "Product does not exist",
          "product-nonexistent"
        );
      });
  }

  private static Optional<EIRedaction> toProductRedaction(
    final Record r)
  {
    final var reason = r.get(PRODUCT_REDACTIONS.REASON);
    if (reason != null) {
      return Optional.of(new EIRedaction(
        r.get(PRODUCT_REDACTIONS.CREATOR),
        r.get(PRODUCT_REDACTIONS.CREATED),
        reason
      ));
    }
    return Optional.empty();
  }

  private static EIDatabaseProduct toProductWithRedaction(
    final Record productRec,
    final EIProductDescription description)
  {
    return new EIDatabaseProduct(
      productRec.<Long>get(PRODUCTS.ID).longValue(),
      new EIProduct(
        new EIProductIdentifier(
          productRec.get(PRODUCTS.PRODUCT_GROUP),
          productRec.get(PRODUCTS.PRODUCT_NAME)
        ),
        List.of(),
        description,
        toProductRedaction(productRec)
      )
    );
  }

  private static List<EIDatabaseProductCategory> fetchCategories(
    final EIServerDatabaseIncludeRedacted includeRedacted,
    final DSLContext context)
  {
    final var query =
      context.select()
        .from(CATEGORIES)
        .leftOuterJoin(CATEGORY_REDACTIONS)
        .on(CATEGORY_REDACTIONS.CATEGORY.eq(CATEGORIES.ID));

    return query.stream()
      .map(EIServerDatabaseProductsQueries::toCategoryWithRedaction)
      .filter(c -> filterRedactedIfNecessary(c, includeRedacted))
      .toList();
  }

  private static EIDatabaseProductCategory fetchCategoryOrFail(
    final EIProductCategory category,
    final EIServerDatabaseIncludeRedacted includeRedacted,
    final DSLContext context)
    throws EIServerDatabaseException
  {
    final var query =
      context.select()
        .from(CATEGORIES)
        .leftOuterJoin(CATEGORY_REDACTIONS)
        .on(CATEGORY_REDACTIONS.CATEGORY.eq(CATEGORIES.ID))
        .where(CATEGORIES.NAME.eq(category.value()));

    final var categoryRecOpt =
      query.fetchOptional()
        .map(EIServerDatabaseProductsQueries::toCategoryWithRedaction);

    if (categoryRecOpt.isEmpty()) {
      throw new EIServerDatabaseException(
        "Category does not exist",
        "category-nonexistent"
      );
    }

    final var categoryRec = categoryRecOpt.get();
    if (!filterRedactedIfNecessary(categoryRec, includeRedacted)) {
      throw new EIServerDatabaseException(
        "Category does not exist",
        "category-nonexistent"
      );
    }

    return categoryRec;
  }

  private static String redactionReason(
    final Optional<EIRedactionRequest> redacted,
    final EIProductIdentifier id)
  {
    return redacted.map(r -> id.show() + ": " + r.reason())
      .orElseGet(id::show);
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

  private static boolean filterRedactedIfNecessary(
    final EIRedactableType r,
    final EIServerDatabaseIncludeRedacted includeRedacted)
  {
    return switch (includeRedacted) {
      case INCLUDE_REDACTED -> true;
      case EXCLUDE_REDACTED -> r.redaction().isEmpty();
    };
  }

  private static EIDatabaseProductCategory toCategoryWithRedaction(
    final Record r)
  {
    final var reason = r.get(CATEGORY_REDACTIONS.REASON);
    if (reason != null) {
      return new EIDatabaseProductCategory(
        r.<Long>get(CATEGORIES.ID).longValue(),
        new EIProductCategory(
          r.get(CATEGORIES.NAME),
          Optional.of(new EIRedaction(
            r.get(CATEGORY_REDACTIONS.CREATOR),
            r.get(CATEGORY_REDACTIONS.CREATED),
            reason
          )))
      );
    }

    return new EIDatabaseProductCategory(
      r.<Long>get(CATEGORIES.ID).longValue(),
      new EIProductCategory(
        r.get(CATEGORIES.NAME),
        Optional.empty())
    );
  }

  @Override
  public Set<EIProductCategory> categories(
    final EIServerDatabaseIncludeRedacted includeRedacted)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(includeRedacted, "includeRedacted");

    final var context = this.transaction.createContext();

    try {
      return fetchCategories(includeRedacted, context)
        .stream()
        .map(EIDatabaseProductCategory::category)
        .collect(Collectors.toUnmodifiableSet());
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction, e);
    }
  }

  @Override
  @EIServerDatabaseRequiresUser
  public EIProductCategory categoryCreate(
    final String text)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(text, "text");

    final var owner =
      this.transaction.userId();
    final var context =
      this.transaction.createContext();

    try {
      final var existingOpt =
        context.fetchOptional(CATEGORIES, CATEGORIES.NAME.eq(text));
      final var time =
        this.currentTime();

      if (existingOpt.isEmpty()) {
        final var inserted =
          context.insertInto(CATEGORIES)
            .set(CATEGORIES.NAME, text)
            .set(CATEGORIES.CREATED, time)
            .execute();

        Preconditions.checkPreconditionV(
          inserted == 1,
          "Expected to insert 1 record (inserted %d)",
          Integer.valueOf(inserted)
        );
      }

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, time)
          .set(AUDIT.TYPE, "CATEGORY_CREATED")
          .set(AUDIT.USER_ID, owner)
          .set(AUDIT.MESSAGE, text);

      insertAuditRecord(audit);
      return new EIProductCategory(text, Optional.empty());
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction, e);
    }
  }

  private OffsetDateTime currentTime()
  {
    return OffsetDateTime.now(this.transaction.clock()).withNano(0);
  }

  @Override
  @EIServerDatabaseRequiresUser
  public EIProductCategory categoryRedact(
    final String category,
    final Optional<EIRedactionRequest> redacted)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(category, "category");
    Objects.requireNonNull(redacted, "redacted");

    final var owner =
      this.transaction.userId();
    final var context =
      this.transaction.createContext();

    try {
      final var categoryRec =
        fetchCategoryOrFail(
          new EIProductCategory(category, Optional.empty()),
          INCLUDE_REDACTED,
          context
        );

      /*
       * Delete any existing redaction. If there is one, we're either going
       * to replace it or completely remove it.
       */

      context.delete(CATEGORY_REDACTIONS)
        .where(CATEGORY_REDACTIONS.CATEGORY.eq(valueOf(categoryRec.id())))
        .execute();

      final var time = this.currentTime();

      /*
       * Create a new redaction if required.
       */

      if (redacted.isPresent()) {
        final var redact = redacted.get();

        final var inserted =
          context.insertInto(CATEGORY_REDACTIONS)
            .set(CATEGORY_REDACTIONS.CATEGORY, valueOf(categoryRec.id()))
            .set(CATEGORY_REDACTIONS.REASON, redact.reason())
            .set(CATEGORY_REDACTIONS.CREATOR, owner)
            .set(CATEGORY_REDACTIONS.CREATED, redact.created())
            .execute();

        Preconditions.checkPreconditionV(
          inserted == 1,
          "Expected to insert 1 record (inserted %d)",
          Integer.valueOf(inserted)
        );
      }

      /*
       * Log the redaction.
       */

      final var redactType =
        redacted.isPresent() ? "CATEGORY_REDACTED" : "CATEGORY_UNREDACTED";
      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, time)
          .set(AUDIT.TYPE, redactType)
          .set(AUDIT.USER_ID, owner)
          .set(AUDIT.MESSAGE, category);

      insertAuditRecord(audit);
      return new EIProductCategory(
        category,
        redacted.map(r -> new EIRedaction(owner, r.created(), r.reason()))
      );
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction, e);
    }
  }

  @Override
  @EIServerDatabaseRequiresUser
  public EIProduct productCreate(
    final EIProductIdentifier id)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");

    final var owner =
      this.transaction.userId();
    final var context =
      this.transaction.createContext();

    try {
      final var existing =
        fetchProductOptional(id, INCLUDE_REDACTED, INCLUDE_NOTHING, context);

      if (existing.isPresent()) {
        throw new EIServerDatabaseException(
          "Product already exists",
          "product-duplicate"
        );
      }

      final var time = this.currentTime();

      final var inserted =
        context.insertInto(PRODUCTS)
          .set(PRODUCTS.PRODUCT_GROUP, id.group())
          .set(PRODUCTS.PRODUCT_NAME, id.name())
          .set(PRODUCTS.CREATED_BY, owner)
          .set(PRODUCTS.CREATED, time)
          .set(PRODUCTS.PRODUCT_TITLE, "")
          .set(PRODUCTS.PRODUCT_DESCRIPTION, "")
          .set(PRODUCTS.PRODUCT_DESCRIPTION_TYPE, "text/plain")
          .execute();

      Preconditions.checkPreconditionV(
        inserted == 1,
        "Expected to insert 1 record (inserted %d)",
        Integer.valueOf(inserted)
      );

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, time)
          .set(AUDIT.TYPE, "PRODUCT_CREATED")
          .set(AUDIT.USER_ID, owner)
          .set(AUDIT.MESSAGE, id.show());

      insertAuditRecord(audit);
      return new EIProduct(
        id,
        List.of(),
        new EIProductDescription(
          "",
          new EIRichText("text/plain", ""),
          Set.of(),
          List.of()
        ),
        Optional.empty()
      );
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction, e);
    }
  }

  @Override
  @EIServerDatabaseRequiresUser
  public void productRedact(
    final EIProductIdentifier id,
    final Optional<EIRedactionRequest> redacted)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(redacted, "redacted");

    final var owner =
      this.transaction.userId();
    final var context =
      this.transaction.createContext();

    try {
      final var existing =
        fetchProductOrFail(id, INCLUDE_REDACTED, INCLUDE_NOTHING, context);

      final var time = this.currentTime();

      /*
       * Delete any existing redaction. If there is one, we're either going
       * to replace it or completely remove it.
       */

      context.delete(PRODUCT_REDACTIONS)
        .where(PRODUCT_REDACTIONS.PRODUCT.eq(valueOf(existing.id())))
        .execute();

      /*
       * Create a new redaction if required.
       */

      if (redacted.isPresent()) {
        final var redact =
          redacted.get();

        final var inserted =
          context.insertInto(PRODUCT_REDACTIONS)
            .set(PRODUCT_REDACTIONS.PRODUCT, valueOf(existing.id()))
            .set(PRODUCT_REDACTIONS.REASON, redact.reason())
            .set(PRODUCT_REDACTIONS.CREATOR, owner)
            .set(PRODUCT_REDACTIONS.CREATED, redact.created())
            .execute();

        Preconditions.checkPreconditionV(
          inserted == 1,
          "Expected to insert 1 record (inserted %d)",
          Integer.valueOf(inserted)
        );
      }

      final var auditType =
        redacted.isPresent() ? "PRODUCT_REDACTED" : "PRODUCT_UNREDACTED";

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, time)
          .set(AUDIT.TYPE, auditType)
          .set(AUDIT.USER_ID, owner)
          .set(AUDIT.MESSAGE, redactionReason(redacted, id));

      insertAuditRecord(audit);
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction, e);
    }
  }

  @Override
  public Set<EIProductIdentifier> productsAll(
    final EIServerDatabaseIncludeRedacted includeRedacted)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(includeRedacted, "includeRedacted");

    final var context = this.transaction.createContext();

    try {
      return fetchProducts(includeRedacted, context)
        .stream().map(r -> r.product().id())
        .collect(Collectors.toUnmodifiableSet());
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction, e);
    }
  }

  @Override
  public EIProduct product(
    final EIProductIdentifier id,
    final EIServerDatabaseIncludeRedacted includeRedacted)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(includeRedacted, "includeRedacted");

    final var context = this.transaction.createContext();

    try {
      return fetchProductOrFail(
        id,
        includeRedacted,
        EnumSet.allOf(ProductInformationComponents.class),
        context
      ).product();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction, e);
    }
  }

  @Override
  @EIServerDatabaseRequiresUser
  public void productCategoryAdd(
    final EIProductIdentifier id,
    final EIProductCategory category)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(category, "category");

    final var owner =
      this.transaction.userId();
    final var context =
      this.transaction.createContext();

    try {
      final var productRec =
        fetchProductOrFail(id, INCLUDE_REDACTED, INCLUDE_NOTHING, context);
      final var categoryRec =
        fetchCategoryOrFail(category, INCLUDE_REDACTED, context);

      /*
       * Delete any existing product/category association. If one already
       * existed, it will be immediately replaced.
       */

      context.delete(PRODUCT_CATEGORIES)
        .where(PRODUCT_CATEGORIES.CATEGORY_PRODUCT.eq(valueOf(productRec.id()))
                 .and(PRODUCT_CATEGORIES.CATEGORY_ID.eq(valueOf(categoryRec.id()))))
        .execute();

      final var inserted =
        context.insertInto(PRODUCT_CATEGORIES)
          .set(PRODUCT_CATEGORIES.CATEGORY_PRODUCT, valueOf(productRec.id()))
          .set(PRODUCT_CATEGORIES.CATEGORY_ID, valueOf(categoryRec.id()))
          .execute();

      Preconditions.checkPreconditionV(
        inserted == 1,
        "Expected to insert 1 record (inserted %d)",
        Integer.valueOf(inserted)
      );

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, this.currentTime())
          .set(AUDIT.TYPE, "PRODUCT_CATEGORY_ADDED")
          .set(AUDIT.USER_ID, owner)
          .set(AUDIT.MESSAGE, id.show() + ":" + category.value());

      insertAuditRecord(audit);
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction, e);
    }
  }

  @Override
  @EIServerDatabaseRequiresUser
  public void productCategoryRemove(
    final EIProductIdentifier id,
    final EIProductCategory category)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(category, "category");

    final var owner =
      this.transaction.userId();
    final var context =
      this.transaction.createContext();

    try {
      final var productRec =
        fetchProductOrFail(id, INCLUDE_REDACTED, INCLUDE_NOTHING, context);
      final var categoryRec =
        fetchCategoryOrFail(category, INCLUDE_REDACTED, context);

      /*
       * Delete any existing product/category association.
       */

      final var deleted =
        context.delete(PRODUCT_CATEGORIES)
          .where(PRODUCT_CATEGORIES.CATEGORY_PRODUCT.eq(valueOf(productRec.id()))
                   .and(PRODUCT_CATEGORIES.CATEGORY_ID.eq(valueOf(categoryRec.id()))))
          .execute();

      if (deleted > 0) {
        final var audit =
          context.insertInto(AUDIT)
            .set(AUDIT.TIME, this.currentTime())
            .set(AUDIT.TYPE, "PRODUCT_CATEGORY_REMOVED")
            .set(AUDIT.USER_ID, owner)
            .set(AUDIT.MESSAGE, id.show() + ":" + category.value());

        insertAuditRecord(audit);
      }
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction, e);
    }
  }

  @Override
  @EIServerDatabaseRequiresUser
  public void productSetTitle(
    final EIProductIdentifier id,
    final String title)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(title, "title");

    final var owner =
      this.transaction.userId();
    final var context =
      this.transaction.createContext();

    try {
      final var product =
        fetchProductOrFail(id, INCLUDE_REDACTED, INCLUDE_NOTHING, context);

      final var updated =
        context.update(PRODUCTS)
          .set(PRODUCTS.PRODUCT_TITLE, title)
          .where(PRODUCTS.ID.eq(valueOf(product.id())))
          .execute();

      Preconditions.checkPreconditionV(
        updated == 1,
        "Expected to update 1 record (update %d)",
        Integer.valueOf(updated)
      );

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, this.currentTime())
          .set(AUDIT.TYPE, "PRODUCT_TITLE_SET")
          .set(AUDIT.USER_ID, owner)
          .set(AUDIT.MESSAGE, id.show() + ":" + title);

      insertAuditRecord(audit);
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction, e);
    }
  }

  @Override
  @EIServerDatabaseRequiresUser
  public void productSetDescription(
    final EIProductIdentifier id,
    final EIRichText description)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(description, "description");

    final var owner =
      this.transaction.userId();
    final var context =
      this.transaction.createContext();

    try {
      final var product =
        fetchProductOrFail(id, INCLUDE_REDACTED, INCLUDE_NOTHING, context);

      final var updated =
        context.update(PRODUCTS)
          .set(PRODUCTS.PRODUCT_DESCRIPTION_TYPE, description.contentType())
          .set(PRODUCTS.PRODUCT_DESCRIPTION, description.text())
          .where(PRODUCTS.ID.eq(valueOf(product.id())))
          .execute();

      Preconditions.checkPreconditionV(
        updated == 1,
        "Expected to update 1 record (update %d)",
        Integer.valueOf(updated)
      );

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, this.currentTime())
          .set(AUDIT.TYPE, "PRODUCT_DESCRIPTION_SET")
          .set(AUDIT.USER_ID, owner)
          .set(AUDIT.MESSAGE, id.show());

      insertAuditRecord(audit);
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction, e);
    }
  }

  @Override
  @EIServerDatabaseRequiresUser
  public void productReleaseCreate(
    final EIProductIdentifier id,
    final EIProductRelease release)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(release, "release");

    final var owner =
      this.transaction.userId();
    final var context =
      this.transaction.createContext();

    try {
      final var product =
        fetchProductOrFail(id, INCLUDE_REDACTED, INCLUDE_NOTHING, context);

      throw new UnimplementedCodeException();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction, e);
    }
  }

  enum ProductInformationComponents
  {
    INCLUDE_CATEGORIES,
    INCLUDE_RELEASES,
    INCLUDE_DESCRIPTION
  }

  private record EIDatabaseProduct(
    long id,
    EIProduct product)
    implements EIRedactableType
  {
    @Override
    public Optional<EIRedaction> redaction()
    {
      return this.product.redaction();
    }
  }

  private record EIDatabaseProductCategory(
    long id,
    EIProductCategory category)
    implements EIRedactableType
  {
    @Override
    public Optional<EIRedaction> redaction()
    {
      return this.category.redaction();
    }
  }
}
