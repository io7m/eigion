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
import com.io7m.eigion.model.EIProductRelease;
import com.io7m.eigion.model.EIProductVersion;
import com.io7m.eigion.model.EIRedactionRequest;
import com.io7m.eigion.model.EIRichText;

import java.util.Optional;
import java.util.Set;

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

  Set<EIProductCategory> categories(
    EIServerDatabaseIncludeRedacted includeRedacted)
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

  @EIServerDatabaseRequiresUser
  EIProductCategory categoryCreate(
    String text)
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

  @EIServerDatabaseRequiresUser
  EIProductCategory categoryRedact(
    String category,
    Optional<EIRedactionRequest> redacted)
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

  @EIServerDatabaseRequiresUser
  default EIProductCategory categoryRedact(
    final EIProductCategory category,
    final Optional<EIRedactionRequest> redacted)
    throws EIServerDatabaseException
  {
    return this.categoryRedact(category.value(), redacted);
  }

  /**
   * Create a product.
   *
   * @param id The product ID
   *
   * @return A new product
   *
   * @throws EIServerDatabaseException On errors
   */

  @EIServerDatabaseRequiresUser
  EIProduct productCreate(
    EIProductIdentifier id)
    throws EIServerDatabaseException;

  /**
   * Redact a product.
   *
   * @param id       The product ID
   * @param redacted The redaction
   *
   * @throws EIServerDatabaseException On errors
   */

  @EIServerDatabaseRequiresUser
  void productRedact(
    EIProductIdentifier id,
    Optional<EIRedactionRequest> redacted)
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
    EIServerDatabaseIncludeRedacted includeRedacted)
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
    EIServerDatabaseIncludeRedacted includeRedacted)
    throws EIServerDatabaseException;

  /**
   * Add a category to a product. Both product and category must exist.
   *
   * @param id       The product
   * @param category The category
   *
   * @throws EIServerDatabaseException On errors
   */

  @EIServerDatabaseRequiresUser
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

  @EIServerDatabaseRequiresUser
  void productCategoryRemove(
    EIProductIdentifier id,
    EIProductCategory category)
    throws EIServerDatabaseException;

  /**
   * Set the product title.
   *
   * @param id    The product ID
   * @param title The title
   *
   * @throws EIServerDatabaseException On errors
   */

  @EIServerDatabaseRequiresUser
  void productSetTitle(
    EIProductIdentifier id,
    String title)
    throws EIServerDatabaseException;

  /**
   * Set the product description.
   *
   * @param id          The product ID
   * @param description The description
   *
   * @throws EIServerDatabaseException On errors
   */

  @EIServerDatabaseRequiresUser
  void productSetDescription(
    EIProductIdentifier id,
    EIRichText description)
    throws EIServerDatabaseException;

  /**
   * Create a new release.
   *
   * @param id      The product ID
   * @param release The release information
   *
   * @throws EIServerDatabaseException On errors
   */

  @EIServerDatabaseRequiresUser
  void productReleaseCreate(
    EIProductIdentifier id,
    EIProductRelease release)
    throws EIServerDatabaseException;

  /**
   * Redact a release.
   *
   * @param id        The product ID
   * @param version   The version
   * @param redaction The redaction
   *
   * @throws EIServerDatabaseException On errors
   */

  @EIServerDatabaseRequiresUser
  void productReleaseRedact(
    EIProductIdentifier id,
    EIProductVersion version,
    Optional<EIRedactionRequest> redaction)
    throws EIServerDatabaseException;
}
