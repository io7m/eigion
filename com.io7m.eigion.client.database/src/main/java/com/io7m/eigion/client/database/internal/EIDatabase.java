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

package com.io7m.eigion.client.database.internal;

import com.io7m.eigion.client.database.api.EIDatabaseType;
import org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * An open database.
 */

public final class EIDatabase implements EIDatabaseType
{
  private final EmbeddedConnectionPoolDataSource dataSource;

  /**
   * An open database.
   *
   * @param inDataSource The underlying data source
   */

  public EIDatabase(
    final EmbeddedConnectionPoolDataSource inDataSource)
  {
    this.dataSource =
      Objects.requireNonNull(inDataSource, "dataSource");
  }

  @Override
  public void close()
  {

  }

  @Override
  public Connection openConnection()
    throws SQLException
  {
    final var connection =
      this.dataSource.getPooledConnection().getConnection();
    connection.setAutoCommit(false);
    return connection;
  }
}
