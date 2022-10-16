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

import java.util.UUID;

/**
 * A database transaction. If the transaction is closed, it is automatically
 * rolled back.
 */

public interface EIDatabaseTransactionType extends AutoCloseable
{
  @Override
  void close()
    throws EIDatabaseException;

  /**
   * Obtain queries for the transaction.
   *
   * @param queryClass The query type
   * @param <T>        The query type
   *
   * @return Queries
   *
   * @throws EIDatabaseException On errors
   */

  <T extends EIDatabaseQueriesType> T queries(Class<T> queryClass)
    throws EIDatabaseException;

  /**
   * Roll back the transaction.
   *
   * @throws EIDatabaseException On errors
   */

  void rollback()
    throws EIDatabaseException;

  /**
   * Commit the transaction.
   *
   * @throws EIDatabaseException On errors
   */

  void commit()
    throws EIDatabaseException;

  /**
   * Set the user ID for the transaction. This is the ID that will typically end
   * up in audit events.
   *
   * @param userId The user ID
   *
   * @throws EIDatabaseException On errors
   */

  void userIdSet(UUID userId)
    throws EIDatabaseException;

  /**
   * @return The current user ID
   *
   * @throws EIDatabaseException On errors
   */

  UUID userId()
    throws EIDatabaseException;

  /**
   * Set the admin ID for the transaction. This is the ID that will typically end
   * up in audit events.
   *
   * @param adminId The admin ID
   *
   * @throws EIDatabaseException On errors
   */

  void adminIdSet(UUID adminId)
    throws EIDatabaseException;

  /**
   * @return The current admin ID
   *
   * @throws EIDatabaseException On errors
   */

  UUID adminId()
    throws EIDatabaseException;

  /**
   * Determine the executor ID. This is the value set by whichever of
   * {@link #adminIdSet(UUID)} or {@link #userIdSet(UUID)} has been called
   * most recently.
   *
   * @return The current executor ID
   *
   * @throws EIDatabaseException On errors
   */

  UUID executorId()
    throws EIDatabaseException;
}
