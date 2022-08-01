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

import java.util.Optional;
import java.util.UUID;

/**
 * A database transaction. If the transaction is closed, it is automatically
 * rolled back.
 */

public interface EIServerDatabaseTransactionType extends AutoCloseable
{
  @Override
  void close()
    throws EIServerDatabaseException;

  /**
   * Set the user ID for the transaction. This is the ID that will typically end
   * up in audit events.
   *
   * @param userId The user ID
   *
   * @throws EIServerDatabaseException On errors
   */

  void userIdSet(UUID userId)
    throws EIServerDatabaseException;

  /**
   * @return The current user ID
   *
   * @throws EIServerDatabaseException On errors
   */

  UUID userId()
    throws EIServerDatabaseException;

  /**
   * Set the admin ID for the transaction. This is the ID that will typically
   * end up in audit events.
   *
   * @param adminId The admin ID
   *
   * @throws EIServerDatabaseException On errors
   */

  void adminIdSet(UUID adminId)
    throws EIServerDatabaseException;

  /**
   * @return The current admin ID
   *
   * @throws EIServerDatabaseException On errors
   */

  UUID adminId()
    throws EIServerDatabaseException;

  /**
   * @return The current admin ID, if defined
   *
   * @throws EIServerDatabaseException On errors
   */

  Optional<UUID> adminIdIfPresent()
    throws EIServerDatabaseException;

  /**
   * Obtain queries for the transaction.
   *
   * @param queryClass The query type
   * @param <T>        The query type
   *
   * @return Queries
   *
   * @throws EIServerDatabaseException On errors
   */

  <T extends EIServerDatabaseQueriesType> T queries(Class<T> queryClass)
    throws EIServerDatabaseException;

  /**
   * Roll back the transaction.
   *
   * @throws EIServerDatabaseException On errors
   */

  void rollback()
    throws EIServerDatabaseException;

  /**
   * Commit the transaction.
   *
   * @throws EIServerDatabaseException On errors
   */

  void commit()
    throws EIServerDatabaseException;
}
