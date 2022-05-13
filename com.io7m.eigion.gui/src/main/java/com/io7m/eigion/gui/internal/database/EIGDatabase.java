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


package com.io7m.eigion.gui.internal.database;

import com.io7m.eigion.database.EIDatabases;
import com.io7m.eigion.database.api.EIDatabaseConfiguration;
import com.io7m.eigion.database.api.EIDatabaseType;
import com.io7m.eigion.gui.EIGConfiguration;
import com.io7m.eigion.services.api.EIServiceType;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;

import static com.io7m.eigion.database.api.EIDatabaseCreate.CREATE_DATABASE;
import static com.io7m.eigion.database.api.EIDatabaseUpgrade.UPGRADE_DATABASE;

/**
 * The database service.
 */

public final class EIGDatabase implements EIServiceType
{
  private final EIDatabaseType database;

  /**
   * The database service.
   *
   * @param inDatabase A database
   */

  private EIGDatabase(
    final EIDatabaseType inDatabase)
  {
    this.database =
      Objects.requireNonNull(inDatabase, "database");
  }

  /**
   * Create a database service.
   *
   * @param inConfiguration A database configuration
   *
   * @return A database service
   *
   * @throws IOException  On errors
   * @throws SQLException On errors
   */

  public static EIGDatabase create(
    final EIGConfiguration inConfiguration)
    throws SQLException, IOException
  {
    final var databases = new EIDatabases();

    final var database =
      databases.open(new EIDatabaseConfiguration(
        inConfiguration.directories()
          .configurationDirectory()
          .resolve("database"),
        CREATE_DATABASE,
        UPGRADE_DATABASE
      ));

    return new EIGDatabase(database);
  }

  @Override
  public String description()
  {
    return "Local database service";
  }
}
