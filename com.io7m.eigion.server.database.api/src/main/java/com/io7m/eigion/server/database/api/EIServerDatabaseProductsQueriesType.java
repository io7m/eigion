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

import com.io7m.eigion.model.EIProductCategory;

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
    boolean redacted)
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
    final boolean redacted)
    throws EIServerDatabaseException
  {
    return this.categoryRedact(category.value(), redacted);
  }

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
