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

import com.io7m.anethum.common.ParseException;
import com.io7m.anethum.common.SerializeException;
import com.io7m.eigion.model.EICreation;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EILink;
import com.io7m.eigion.model.EIProduct;
import com.io7m.eigion.model.EIProductCategory;
import com.io7m.eigion.model.EIProductDescription;
import com.io7m.eigion.model.EIProductIdentifier;
import com.io7m.eigion.model.EIProductRelease;
import com.io7m.eigion.model.EIProductSummary;
import com.io7m.eigion.model.EIProductSummaryPage;
import com.io7m.eigion.model.EIProductVersion;
import com.io7m.eigion.model.EIRedactableType;
import com.io7m.eigion.model.EIRedaction;
import com.io7m.eigion.model.EIRedactionRequest;
import com.io7m.eigion.model.EIRichText;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseIncludeRedacted;
import com.io7m.eigion.server.database.api.EIServerDatabaseProductsQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseRequiresUser;
import com.io7m.eigion.server.database.postgres.internal.tables.records.ProductLinksRecord;
import com.io7m.jaffirm.core.Invariants;
import com.io7m.jaffirm.core.Postconditions;
import com.io7m.jaffirm.core.Preconditions;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.exception.DataAccessException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.CATEGORY_NONEXISTENT;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.GROUP_NONEXISTENT;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.IO_ERROR;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.PRODUCT_DUPLICATE;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.PRODUCT_NONEXISTENT;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.RELEASE_DUPLICATE;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.RELEASE_NONEXISTENT;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.SERIALIZATION_ERROR;
import static com.io7m.eigion.product.parser.api.EIProductSchemas.RELEASE_CONTENT_TYPE;
import static com.io7m.eigion.server.database.api.EIServerDatabaseIncludeRedacted.INCLUDE_REDACTED;
import static com.io7m.eigion.server.database.postgres.internal.EIServerDatabaseExceptions.handleDatabaseException;
import static com.io7m.eigion.server.database.postgres.internal.EIServerDatabaseProductsQueries.ProductInformationComponents.INCLUDE_CATEGORIES;
import static com.io7m.eigion.server.database.postgres.internal.EIServerDatabaseProductsQueries.ProductInformationComponents.INCLUDE_DESCRIPTION;
import static com.io7m.eigion.server.database.postgres.internal.EIServerDatabaseProductsQueries.ProductInformationComponents.INCLUDE_RELEASES;
import static com.io7m.eigion.server.database.postgres.internal.Tables.CATEGORY_REDACTIONS;
import static com.io7m.eigion.server.database.postgres.internal.Tables.GROUPS;
import static com.io7m.eigion.server.database.postgres.internal.Tables.PRODUCTS;
import static com.io7m.eigion.server.database.postgres.internal.Tables.PRODUCT_CATEGORIES;
import static com.io7m.eigion.server.database.postgres.internal.Tables.PRODUCT_LINKS;
import static com.io7m.eigion.server.database.postgres.internal.Tables.PRODUCT_REDACTIONS;
import static com.io7m.eigion.server.database.postgres.internal.Tables.PRODUCT_RELEASES;
import static com.io7m.eigion.server.database.postgres.internal.Tables.PRODUCT_RELEASE_REDACTIONS;
import static com.io7m.eigion.server.database.postgres.internal.tables.Audit.AUDIT;
import static com.io7m.eigion.server.database.postgres.internal.tables.Categories.CATEGORIES;
import static java.lang.Long.valueOf;

final class EIServerDatabaseProductsQueries
  extends EIBaseQueries
  implements EIServerDatabaseProductsQueriesType
{
  private static final EnumSet<ProductInformationComponents> INCLUDE_NOTHING =
    EnumSet.noneOf(ProductInformationComponents.class);
  private static final EnumSet<ProductInformationComponents> INCLUDE_EVERYTHING =
    EnumSet.allOf(ProductInformationComponents.class);

  EIServerDatabaseProductsQueries(
    final EIServerDatabaseTransaction inTransaction)
  {
    super(inTransaction);
  }

  private static List<EIDatabaseProduct> fetchProducts(
    final EIServerDatabaseIncludeRedacted includeRedacted,
    final DSLContext context)
  {
    return context.select()
      .from(PRODUCTS)
      .join(GROUPS)
      .on(GROUPS.NAME.eq(PRODUCTS.PRODUCT_GROUP))
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

        return toProductWithRedaction(r, description, List.of());
      })
      .filter(r -> filterRedactedIfNecessary(r, includeRedacted))
      .toList();
  }

  private static Optional<EIRedaction> toReleaseRedaction(
    final Record r)
  {
    final var reason = r.get(PRODUCT_RELEASE_REDACTIONS.REASON);
    if (reason != null) {
      return Optional.of(new EIRedaction(
        r.get(PRODUCT_RELEASE_REDACTIONS.CREATOR),
        r.get(PRODUCT_RELEASE_REDACTIONS.CREATED),
        reason
      ));
    }
    return Optional.empty();
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
    final EIProductDescription description,
    final List<EIProductRelease> releases)
  {
    return new EIDatabaseProduct(
      productRec.<Long>get(PRODUCTS.ID).longValue(),
      new EIProduct(
        new EIProductIdentifier(
          new EIGroupName(productRec.get(GROUPS.NAME)),
          productRec.get(PRODUCTS.PRODUCT_NAME)
        ),
        releases,
        description,
        toProductRedaction(productRec),
        new EICreation(
          productRec.get(PRODUCTS.CREATED_BY),
          productRec.get(PRODUCTS.CREATED)
        )
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
        CATEGORY_NONEXISTENT
      );
    }

    final var categoryRec = categoryRecOpt.get();
    if (!filterRedactedIfNecessary(categoryRec, includeRedacted)) {
      throw new EIServerDatabaseException(
        "Category does not exist",
        CATEGORY_NONEXISTENT
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

  private static Optional<Long> fetchProductReleaseID(
    final EIProductIdentifier id,
    final EIProductVersion version,
    final DSLContext context)
  {
    final var vMajor =
      valueOf(version.major().longValue());
    final var vMinor =
      valueOf(version.minor().longValue());
    final var vPatch =
      valueOf(version.patch().longValue());
    final var vQuali =
      version.qualifier().orElse("");

    final var prodRec =
      context.select()
        .from(PRODUCTS)
        .join(GROUPS)
        .on(GROUPS.NAME.eq(PRODUCTS.PRODUCT_GROUP))
        .where(GROUPS.NAME.eq(id.group().value())
                 .and(PRODUCTS.PRODUCT_NAME.eq(id.name())))
        .fetchOptional();

    if (prodRec.isEmpty()) {
      return Optional.empty();
    }

    final var productId =
      prodRec.get().get(PRODUCTS.ID);

    final var releaseSelect =
      context.select()
        .from(PRODUCT_RELEASES)
        .where(PRODUCT_RELEASES.PRODUCT_ID.eq(productId)
                 .and(PRODUCT_RELEASES.VERSION_MAJOR.eq(vMajor))
                 .and(PRODUCT_RELEASES.VERSION_MINOR.eq(vMinor))
                 .and(PRODUCT_RELEASES.VERSION_PATCH.eq(vPatch))
                 .and(PRODUCT_RELEASES.VERSION_QUALIFIER.eq(vQuali))
        );

    return releaseSelect
      .fetchOptional()
      .map(r -> r.get(PRODUCT_RELEASES.ID));
  }

  private static Long fetchProductReleaseIDOrFail(
    final EIProductIdentifier id,
    final EIProductVersion version,
    final DSLContext context)
    throws EIServerDatabaseException
  {
    return fetchProductReleaseID(id, version, context).orElseThrow(() -> {
      return new EIServerDatabaseException(
        "Product release does not exist",
        RELEASE_NONEXISTENT
      );
    });
  }

  private EIDatabaseProduct fetchProductOrFail(
    final EIProductIdentifier id,
    final EIServerDatabaseIncludeRedacted includeRedacted,
    final EnumSet<ProductInformationComponents> includes,
    final DSLContext context)
    throws EIServerDatabaseException
  {
    return this.fetchProductOptional(id, includeRedacted, includes, context)
      .orElseThrow(() -> {
        return new EIServerDatabaseException(
          "Product does not exist",
          PRODUCT_NONEXISTENT
        );
      });
  }

  private Optional<EIDatabaseProduct> fetchProductOptional(
    final EIProductIdentifier id,
    final EIServerDatabaseIncludeRedacted includeRedacted,
    final EnumSet<ProductInformationComponents> includes,
    final DSLContext context)
    throws EIServerDatabaseException
  {
    final var productRec =
      context.select()
        .from(PRODUCTS)
        .join(GROUPS)
        .on(GROUPS.NAME.eq(PRODUCTS.PRODUCT_GROUP))
        .leftOuterJoin(PRODUCT_REDACTIONS)
        .on(PRODUCT_REDACTIONS.PRODUCT.eq(PRODUCTS.ID))
        .where(
          GROUPS.NAME.eq(id.group().value())
            .and(PRODUCTS.PRODUCT_NAME.eq(id.name()))
        ).fetchAny();

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
    final var releases =
      this.fetchProductReleases(productId, includeRedacted, includes, context);

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
      toProductWithRedaction(productRec, description, releases);

    if (!filterRedactedIfNecessary(product, includeRedacted)) {
      return Optional.empty();
    }

    return Optional.of(product);
  }

  private List<EIProductRelease> fetchProductReleases(
    final Long productId,
    final EIServerDatabaseIncludeRedacted includeRedacted,
    final EnumSet<ProductInformationComponents> includes,
    final DSLContext context)
    throws EIServerDatabaseException
  {
    try {
      if (includes.contains(INCLUDE_RELEASES)) {
        return context.select()
          .from(PRODUCT_RELEASES)
          .leftOuterJoin(PRODUCT_RELEASE_REDACTIONS)
          .on(PRODUCT_RELEASE_REDACTIONS.RELEASE.eq(PRODUCT_RELEASES.ID))
          .where(PRODUCT_RELEASES.PRODUCT_ID.eq(productId))
          .stream()
          .map(r -> {
            try {
              return this.toProductReleaseWithRedaction(r);
            } catch (final EIServerDatabaseException e) {
              throw new EIServerDatabaseExceptionUnchecked(e);
            }
          })
          .filter(c -> filterRedactedIfNecessary(c, includeRedacted))
          .toList();
      }
      return List.of();
    } catch (final EIServerDatabaseExceptionUnchecked e) {
      throw e.causeTyped;
    }
  }

  private EIProductRelease toProductReleaseWithRedaction(
    final Record r)
    throws EIServerDatabaseException
  {
    final var manifestType =
      r.get(PRODUCT_RELEASES.MANIFEST_TYPE);
    final var manifest =
      r.get(PRODUCT_RELEASES.MANIFEST);

    final EIProductRelease release =
      switch (manifestType) {
        case RELEASE_CONTENT_TYPE -> {
          try (var stream = new ByteArrayInputStream(manifest)) {
            yield this.transaction().productReleaseParsers()
              .parse(URI.create("urn:source"), stream);
          } catch (final IOException e) {
            throw new EIServerDatabaseException(e.getMessage(), e, IO_ERROR);
          } catch (final ParseException e) {
            throw new EIServerDatabaseException(
              e.getMessage(),
              e,
              SERIALIZATION_ERROR);
          }
        }
        default -> {
          throw new EIServerDatabaseException(
            "Unrecognized manifest type: " + manifestType,
            SERIALIZATION_ERROR
          );
        }
      };

    final var major =
      BigInteger.valueOf(r.get(PRODUCT_RELEASES.VERSION_MAJOR).longValue());
    final var minor =
      BigInteger.valueOf(r.get(PRODUCT_RELEASES.VERSION_MINOR).longValue());
    final var patch =
      BigInteger.valueOf(r.get(PRODUCT_RELEASES.VERSION_PATCH).longValue());
    final var qualifier =
      r.get(PRODUCT_RELEASES.VERSION_QUALIFIER);

    final Optional<String> qualifierOpt;
    if (Objects.equals(qualifier, "")) {
      qualifierOpt = Optional.empty();
    } else {
      qualifierOpt = Optional.of(qualifier);
    }

    final var version =
      new EIProductVersion(major, minor, patch, qualifierOpt);

    Invariants.checkInvariantV(
      version.equals(release.version()),
      "Database manifest version %s must equal table version %s",
      release.version().show(),
      version.show()
    );

    final var creation =
      new EICreation(
        r.get(PRODUCT_RELEASES.CREATED_BY),
        r.get(PRODUCT_RELEASES.CREATED)
      );

    return new EIProductRelease(
      version,
      release.productDependencies(),
      release.bundleDependencies(),
      release.changes(),
      toReleaseRedaction(r),
      creation
    );
  }

  @Override
  public Set<EIProductCategory> categories(
    final EIServerDatabaseIncludeRedacted includeRedacted)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(includeRedacted, "includeRedacted");

    final var context = this.transaction().createContext();

    try {
      return fetchCategories(includeRedacted, context)
        .stream()
        .map(EIDatabaseProductCategory::category)
        .collect(Collectors.toUnmodifiableSet());
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
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
      this.transaction().userId();
    final var context =
      this.transaction().createContext();

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
      throw handleDatabaseException(this.transaction(), e);
    }
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
      this.transaction().userId();
    final var context =
      this.transaction().createContext();

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

        Postconditions.checkPostconditionV(
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
      throw handleDatabaseException(this.transaction(), e);
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
      this.transaction().userId();
    final var context =
      this.transaction().createContext();

    try {
      final var existing =
        this.fetchProductOptional(
          id,
          INCLUDE_REDACTED,
          INCLUDE_NOTHING,
          context);

      if (existing.isPresent()) {
        throw new EIServerDatabaseException(
          "Product already exists",
          PRODUCT_DUPLICATE
        );
      }

      final var group =
        context.fetchOptional(GROUPS, GROUPS.NAME.eq(id.group().value()))
          .orElseThrow(() -> {
            return new EIServerDatabaseException(
              "Group does not exist",
              GROUP_NONEXISTENT
            );
          });

      final var time = this.currentTime();

      final var inserted =
        context.insertInto(PRODUCTS)
          .set(PRODUCTS.PRODUCT_GROUP, group.get(GROUPS.NAME))
          .set(PRODUCTS.PRODUCT_NAME, id.name())
          .set(PRODUCTS.CREATED_BY, owner)
          .set(PRODUCTS.CREATED, time)
          .set(PRODUCTS.PRODUCT_TITLE, "")
          .set(PRODUCTS.PRODUCT_DESCRIPTION, "")
          .set(PRODUCTS.PRODUCT_DESCRIPTION_TYPE, "text/plain")
          .execute();

      Postconditions.checkPostconditionV(
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
        Optional.empty(),
        new EICreation(owner, time)
      );
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
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
      this.transaction().userId();
    final var context =
      this.transaction().createContext();

    try {
      final var existing =
        this.fetchProductOrFail(id, INCLUDE_REDACTED, INCLUDE_NOTHING, context);

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

        Postconditions.checkPostconditionV(
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
      throw handleDatabaseException(this.transaction(), e);
    }
  }

  @Override
  public Set<EIProductIdentifier> productsAll(
    final EIServerDatabaseIncludeRedacted includeRedacted)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(includeRedacted, "includeRedacted");

    final var context = this.transaction().createContext();

    try {
      return fetchProducts(includeRedacted, context)
        .stream().map(r -> r.product().id())
        .collect(Collectors.toUnmodifiableSet());
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
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

    final var context = this.transaction().createContext();

    try {
      return this.fetchProductOrFail(
        id,
        includeRedacted,
        INCLUDE_EVERYTHING,
        context
      ).product();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
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
      this.transaction().userId();
    final var context =
      this.transaction().createContext();

    try {
      final var productRec =
        this.fetchProductOrFail(id, INCLUDE_REDACTED, INCLUDE_NOTHING, context);
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

      Postconditions.checkPostconditionV(
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
      throw handleDatabaseException(this.transaction(), e);
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
      this.transaction().userId();
    final var context =
      this.transaction().createContext();

    try {
      final var productRec =
        this.fetchProductOrFail(id, INCLUDE_REDACTED, INCLUDE_NOTHING, context);
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
      throw handleDatabaseException(this.transaction(), e);
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
      this.transaction().userId();
    final var context =
      this.transaction().createContext();

    try {
      final var product =
        this.fetchProductOrFail(id, INCLUDE_REDACTED, INCLUDE_NOTHING, context);

      final var updated =
        context.update(PRODUCTS)
          .set(PRODUCTS.PRODUCT_TITLE, title)
          .where(PRODUCTS.ID.eq(valueOf(product.id())))
          .execute();

      Postconditions.checkPostconditionV(
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
      throw handleDatabaseException(this.transaction(), e);
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
      this.transaction().userId();
    final var context =
      this.transaction().createContext();

    try {
      final var product =
        this.fetchProductOrFail(id, INCLUDE_REDACTED, INCLUDE_NOTHING, context);

      final var updated =
        context.update(PRODUCTS)
          .set(PRODUCTS.PRODUCT_DESCRIPTION_TYPE, description.contentType())
          .set(PRODUCTS.PRODUCT_DESCRIPTION, description.text())
          .where(PRODUCTS.ID.eq(valueOf(product.id())))
          .execute();

      Postconditions.checkPostconditionV(
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
      throw handleDatabaseException(this.transaction(), e);
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
      this.transaction().userId();
    final var context =
      this.transaction().createContext();

    try {
      final var product =
        this.fetchProductOrFail(
          id,
          INCLUDE_REDACTED,
          INCLUDE_EVERYTHING,
          context);

      final var version = release.version();
      for (final var existing : product.product.releases()) {
        if (Objects.equals(existing.version(), version)) {
          throw new EIServerDatabaseException(
            String.format("Release version %s already exists", version.show()),
            RELEASE_DUPLICATE
          );
        }
      }

      final var time =
        this.currentTime();
      final var q =
        version.qualifier().orElse("");

      final var serializers =
        this.transaction().productReleaseSerializers();

      final var byteStream = new ByteArrayOutputStream();
      serializers.serialize(URI.create("urn:source"), byteStream, release);
      final var manifest = byteStream.toByteArray();

      final var inserted =
        context.insertInto(PRODUCT_RELEASES)
          .set(PRODUCT_RELEASES.PRODUCT_ID, product.id())
          .set(PRODUCT_RELEASES.CREATED, time)
          .set(PRODUCT_RELEASES.CREATED_BY, owner)
          .set(PRODUCT_RELEASES.VERSION_MAJOR, version.major().longValueExact())
          .set(PRODUCT_RELEASES.VERSION_MINOR, version.minor().longValueExact())
          .set(PRODUCT_RELEASES.VERSION_PATCH, version.patch().longValueExact())
          .set(PRODUCT_RELEASES.VERSION_QUALIFIER, q)
          .set(PRODUCT_RELEASES.MANIFEST_TYPE, RELEASE_CONTENT_TYPE)
          .set(PRODUCT_RELEASES.MANIFEST, manifest)
          .execute();

      Postconditions.checkPostconditionV(
        inserted == 1,
        "Expected to insert 1 record (inserted %d)",
        Integer.valueOf(inserted)
      );

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, time)
          .set(AUDIT.TYPE, "PRODUCT_RELEASE_CREATED")
          .set(AUDIT.USER_ID, owner)
          .set(AUDIT.MESSAGE, id.show() + ":" + version.show());

      insertAuditRecord(audit);
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    } catch (final SerializeException e) {
      throw new EIServerDatabaseException(e.getMessage(), e, SERIALIZATION_ERROR);
    }
  }

  @Override
  @EIServerDatabaseRequiresUser
  public void productReleaseRedact(
    final EIProductIdentifier id,
    final EIProductVersion version,
    final Optional<EIRedactionRequest> redaction)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(redaction, "redaction");

    final var owner =
      this.transaction().userId();
    final var context =
      this.transaction().createContext();

    try {
      final var releaseId =
        fetchProductReleaseIDOrFail(id, version, context);

      final var time =
        this.currentTime();

      /*
       * Delete any existing redaction. If there is one, we're either going
       * to replace it or completely remove it.
       */

      context.delete(PRODUCT_RELEASE_REDACTIONS)
        .where(PRODUCT_RELEASE_REDACTIONS.RELEASE.eq(releaseId))
        .execute();

      /*
       * Create a new redaction if required.
       */

      if (redaction.isPresent()) {
        final var redact = redaction.get();

        final var inserted =
          context.insertInto(PRODUCT_RELEASE_REDACTIONS)
            .set(PRODUCT_RELEASE_REDACTIONS.RELEASE, releaseId)
            .set(PRODUCT_RELEASE_REDACTIONS.REASON, redact.reason())
            .set(PRODUCT_RELEASE_REDACTIONS.CREATOR, owner)
            .set(PRODUCT_RELEASE_REDACTIONS.CREATED, redact.created())
            .execute();

        Postconditions.checkPostconditionV(
          inserted == 1,
          "Expected to insert 1 record (inserted %d)",
          Integer.valueOf(inserted)
        );
      }

      final var redactionType =
        redaction.isPresent()
          ? "PRODUCT_RELEASE_REDACTED" : "PRODUCT_RELEASE_UNREDACTED";

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, time)
          .set(AUDIT.TYPE, redactionType)
          .set(AUDIT.USER_ID, owner)
          .set(AUDIT.MESSAGE, id.show() + ":" + version.show());

      insertAuditRecord(audit);
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    }
  }

  @Override
  public EIProductSummaryPage productSummaries(
    final Optional<EIProductIdentifier> startOffset,
    final BigInteger limit)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(startOffset, "startOffset");
    Objects.requireNonNull(limit, "limit");

    final var context =
      this.transaction().createContext();

    try {
      final Select<Record> baseQuery;
      if (startOffset.isPresent()) {
        final var id = startOffset.get();
        baseQuery = context.select()
          .from(PRODUCTS)
          .join(GROUPS)
          .on(GROUPS.NAME.eq(PRODUCTS.PRODUCT_GROUP))
          .leftOuterJoin(PRODUCT_REDACTIONS)
          .on(PRODUCT_REDACTIONS.PRODUCT.eq(PRODUCTS.ID))
          .where(PRODUCT_REDACTIONS.REASON.isNull())
          .orderBy(GROUPS.NAME, PRODUCTS.PRODUCT_NAME)
          .seek(id.group().value(), id.name())
          .limit(limit);
      } else {
        baseQuery = context.select()
          .from(PRODUCTS)
          .join(GROUPS)
          .on(GROUPS.NAME.eq(PRODUCTS.PRODUCT_GROUP))
          .leftOuterJoin(PRODUCT_REDACTIONS)
          .on(PRODUCT_REDACTIONS.PRODUCT.eq(PRODUCTS.ID))
          .where(PRODUCT_REDACTIONS.REASON.isNull())
          .orderBy(GROUPS.NAME, PRODUCTS.PRODUCT_NAME)
          .limit(limit);
      }

      final var items =
        baseQuery.stream()
          .map(r -> {
            return new EIProductSummary(
              new EIProductIdentifier(
                new EIGroupName(r.get(GROUPS.NAME)),
                r.get(PRODUCTS.PRODUCT_NAME)
              ),
              r.get(PRODUCTS.PRODUCT_TITLE),
              toProductRedaction(r)
            );
          })
          .toList();

      return new EIProductSummaryPage(items);
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    }
  }

  enum ProductInformationComponents
  {
    INCLUDE_CATEGORIES,
    INCLUDE_RELEASES,
    INCLUDE_DESCRIPTION
  }

  private record EIDatabaseProductRelease(
    long id,
    EIProductRelease release)
    implements EIRedactableType
  {
    @Override
    public Optional<EIRedaction> redaction()
    {
      return this.release.redaction();
    }
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

  private final class EIServerDatabaseExceptionUnchecked
    extends RuntimeException
  {
    private final EIServerDatabaseException causeTyped;

    private EIServerDatabaseExceptionUnchecked(
      final EIServerDatabaseException cause)
    {
      super(cause);
      this.causeTyped = Objects.requireNonNull(cause, "cause");
    }
  }
}
