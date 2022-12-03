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

package com.io7m.eigion.server.api;

import com.io7m.eigion.server.database.api.EISDatabaseConfiguration;
import com.io7m.eigion.server.database.api.EISDatabaseFactoryType;

import java.net.http.HttpClient;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.function.Supplier;

import static com.io7m.eigion.server.database.api.EISDatabaseCreate.CREATE_DATABASE;
import static com.io7m.eigion.server.database.api.EISDatabaseCreate.DO_NOT_CREATE_DATABASE;
import static com.io7m.eigion.server.database.api.EISDatabaseUpgrade.DO_NOT_UPGRADE_DATABASE;
import static com.io7m.eigion.server.database.api.EISDatabaseUpgrade.UPGRADE_DATABASE;

/**
 * Functions to produce server configurations.
 */

public final class EIServerConfigurations
{
  private EIServerConfigurations()
  {

  }

  /**
   * Read a server configuration from the given file.
   *
   * @param locale  The locale
   * @param clients A supplier of HTTP clients
   * @param clock   The clock
   * @param file    The file
   *
   * @return A server configuration
   */

  public static EIServerConfiguration ofFile(
    final Locale locale,
    final Clock clock,
    final Supplier<HttpClient> clients,
    final EIServerConfigurationFile file)
  {
    Objects.requireNonNull(locale, "locale");
    Objects.requireNonNull(clients, "clients");
    Objects.requireNonNull(clock, "clock");
    Objects.requireNonNull(file, "file");

    final var fileDbConfig =
      file.databaseConfiguration();

    final var databaseConfiguration =
      new EISDatabaseConfiguration(
        fileDbConfig.user(),
        fileDbConfig.password(),
        fileDbConfig.address(),
        fileDbConfig.port(),
        fileDbConfig.databaseName(),
        fileDbConfig.create() ? CREATE_DATABASE : DO_NOT_CREATE_DATABASE,
        fileDbConfig.upgrade() ? UPGRADE_DATABASE : DO_NOT_UPGRADE_DATABASE,
        clock
      );

    final var databaseFactories =
      ServiceLoader.load(EISDatabaseFactoryType.class)
        .iterator();

    final var database =
      findDatabase(databaseFactories, fileDbConfig.kind());

    return new EIServerConfiguration(
      locale,
      clock,
      clients,
      database,
      databaseConfiguration,
      file.httpConfiguration().pikeService(),
      file.httpConfiguration().amberjackService(),
      file.idstoreConfiguration(),
      file.openTelemetry()
    );
  }

  private static EISDatabaseFactoryType findDatabase(
    final Iterator<EISDatabaseFactoryType> databaseFactories,
    final EIServerDatabaseKind kind)
  {
    if (!databaseFactories.hasNext()) {
      throw new ServiceConfigurationError(
        "No available implementations of type %s"
          .formatted(EISDatabaseFactoryType.class)
      );
    }

    final var kinds = new ArrayList<String>();
    while (databaseFactories.hasNext()) {
      final var database = databaseFactories.next();
      kinds.add(database.kind());
      if (Objects.equals(database.kind(), kind.name())) {
        return database;
      }
    }

    throw new ServiceConfigurationError(
      "No available databases of kind %s (Available databases include: %s)"
        .formatted(EISDatabaseFactoryType.class, kinds)
    );
  }
}
