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

package com.io7m.eigion.server.database.api;

import com.io7m.eigion.model.EIProduct;
import com.io7m.eigion.model.EIProductCategory;
import com.io7m.eigion.model.EIProductIdentifier;
import com.io7m.eigion.model.EIRedaction;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The database queries involving products.
 */

public non-sealed interface EIServerDatabaseProductsQueriesType
  extends EIServerDatabaseQueriesType
{
  /**
   * @param includeRedacted Whether to include redacted items.
   *
   * @return A read-only snapshot of the current product categories
   *
   * @throws EIServerDatabaseException On errors
   */

  Set<EIProductCategory> categories(IncludeRedacted includeRedacted)
    throws EIServerDatabaseException;

  /**
   * Create a category.
   *
   * @param text The category name
   *
   * @return The category
   *
   * @throws EIServerDatabaseException On errors
   */

  EIProductCategory categoryCreate(String text)
    throws EIServerDatabaseException;

  /**
   * Redact (or unredact) a category.
   *
   * @param category The category
   * @param redacted Whether the category should be redacted
   *
   * @return The category
   *
   * @throws EIServerDatabaseException On errors
   */

  EIProductCategory categoryRedact(
    String category,
    Optional<EIRedaction> redacted)
    throws EIServerDatabaseException;

  /**
   * Redact (or unredact) a category.
   *
   * @param category The category
   * @param redacted Whether the category should be redacted
   *
   * @return The category
   *
   * @throws EIServerDatabaseException On errors
   */

  default EIProductCategory categoryRedact(
    final EIProductCategory category,
    final Optional<EIRedaction> redacted)
    throws EIServerDatabaseException
  {
    return this.categoryRedact(category.value(), redacted);
  }

  /**
   * Create a product.
   *
   * @param id     The product ID
   * @param userId The user that created the product
   *
   * @return A new product
   *
   * @throws EIServerDatabaseException On errors
   */

  EIProduct productCreate(
    EIProductIdentifier id,
    UUID userId)
    throws EIServerDatabaseException;

  /**
   * Redact a product.
   *
   * @param id       The product ID
   * @param redacted The redaction
   *
   * @throws EIServerDatabaseException On errors
   */

  void productRedact(
    EIProductIdentifier id,
    Optional<EIRedaction> redacted)
    throws EIServerDatabaseException;

  /**
   * Return all product identifiers in the database.
   *
   * @param includeRedacted Whether to include redacted content
   *
   * @return The database products
   *
   * @throws EIServerDatabaseException On errors
   */

  Set<EIProductIdentifier> productsAll(
    IncludeRedacted includeRedacted)
    throws EIServerDatabaseException;

  /**
   * Return the product with the given identifier.
   *
   * @param id              The product identifier
   * @param includeRedacted Whether to include redacted content
   *
   * @return The product
   *
   * @throws EIServerDatabaseException On errors
   */

  EIProduct product(
    EIProductIdentifier id,
    IncludeRedacted includeRedacted)
    throws EIServerDatabaseException;

  /**
   * Add a category to a product. Both product and category must exist.
   *
   * @param id       The product
   * @param category The category
   *
   * @throws EIServerDatabaseException On errors
   */

  void productCategoryAdd(
    EIProductIdentifier id,
    EIProductCategory category)
    throws EIServerDatabaseException;

  /**
   * Remove a category from a product. Both product and category must exist.
   * Does nothing if the product is not already in the category.
   *
   * @param id       The product
   * @param category The category
   *
   * @throws EIServerDatabaseException On errors
   */

  void productCategoryRemove(
    EIProductIdentifier id,
    EIProductCategory category)
    throws EIServerDatabaseException;

  /**
   * Whether to include redacted items.
   */

  enum IncludeRedacted
  {
    /**
     * Include redacted items.
     */

    INCLUDE_REDACTED,

    /**
     * Exclude redacted items.
     */

    EXCLUDE_REDACTED
  }
}
