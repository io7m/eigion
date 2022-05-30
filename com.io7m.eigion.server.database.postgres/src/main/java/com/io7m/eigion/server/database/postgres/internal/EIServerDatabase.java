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

import com.io7m.eigion.product.parser.api.EIProductReleaseParsersType;
import com.io7m.eigion.product.parser.api.EIProductReleaseSerializersType;
import com.io7m.eigion.server.database.api.EIServerDatabaseConnectionType;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseRole;
import com.io7m.eigion.server.database.api.EIServerDatabaseType;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.conf.RenderNameCase;
import org.jooq.conf.Settings;

import java.sql.SQLException;
import java.time.Clock;
import java.util.Objects;

/**
 * The default postgres server database implementation.
 */

public final class EIServerDatabase implements EIServerDatabaseType
{
  private final Clock clock;
  private final HikariDataSource dataSource;
  private final EIProductReleaseSerializersType productsSerializers;
  private final Settings settings;
  private final EIProductReleaseParsersType productReleaseParsers;

  /**
   * The default postgres server database implementation.
   *
   * @param inClock                 The clock
   * @param inDataSource            A pooled data source
   * @param inProductsSerializers   A product release serializer factory
   * @param inProductReleaseParsers A product release parser factory
   */

  public EIServerDatabase(
    final Clock inClock,
    final HikariDataSource inDataSource,
    final EIProductReleaseSerializersType inProductsSerializers,
    final EIProductReleaseParsersType inProductReleaseParsers)
  {
    this.clock =
      Objects.requireNonNull(inClock, "clock");
    this.dataSource =
      Objects.requireNonNull(inDataSource, "dataSource");
    this.productsSerializers =
      Objects.requireNonNull(inProductsSerializers, "inProductsSerializers");
    this.productReleaseParsers =
      Objects.requireNonNull(inProductReleaseParsers, "productReleaseParsers");
    this.settings =
      new Settings().withRenderNameCase(RenderNameCase.LOWER);
  }

  @Override
  public void close()
  {
    this.dataSource.close();
  }

  @Override
  public EIServerDatabaseConnectionType openConnection(
    final EIServerDatabaseRole role)
    throws EIServerDatabaseException
  {
    try {
      final var conn = this.dataSource.getConnection();
      conn.setAutoCommit(false);
      return new EIServerDatabaseConnection(this, conn, role);
    } catch (final SQLException e) {
      throw new EIServerDatabaseException(e.getMessage(), e, "sql-error");
    }
  }

  /**
   * @return The jooq SQL settings
   */

  public Settings settings()
  {
    return this.settings;
  }

  /**
   * @return The clock used for time-related queries
   */

  public Clock clock()
  {
    return this.clock;
  }

  /**
   * @return A product release serializer factory
   */

  public EIProductReleaseSerializersType productReleaseSerializers()
  {
    return this.productsSerializers;
  }

  /**
   * @return A product release parser factory
   */

  public EIProductReleaseParsersType productReleaseParsers()
  {
    return this.productReleaseParsers;
  }

  @Override
  public String description()
  {
    return "Server database service.";
  }
}
