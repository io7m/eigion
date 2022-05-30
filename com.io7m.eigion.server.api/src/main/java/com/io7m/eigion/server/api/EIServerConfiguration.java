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

import com.io7m.eigion.server.database.api.EIServerDatabaseConfiguration;
import com.io7m.eigion.server.database.api.EIServerDatabaseFactoryType;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;

/**
 * The configuration for a server.
 *
 * @param databases             The factory of databases that will be used for
 *                              the server
 * @param databaseConfiguration The database configuration for the server
 * @param adminAddress          The address upon which the admin API listens
 * @param publicAddress         The address upon which the public API listens
 * @param sessionDirectory      The server's persistent session directory
 * @param locale                The locale
 * @param clock                 The clock
 */

public record EIServerConfiguration(
  EIServerDatabaseFactoryType databases,
  EIServerDatabaseConfiguration databaseConfiguration,
  InetSocketAddress adminAddress,
  InetSocketAddress publicAddress,
  Path sessionDirectory,
  Locale locale,
  Clock clock)
{
  /**
   * The configuration for a server.
   *
   * @param databases             The factory of databases that will be used for
   *                              the server
   * @param databaseConfiguration The database configuration for the server
   * @param adminAddress          The address upon which the admin API listens
   * @param publicAddress         The address upon which the public API listens
   * @param sessionDirectory      The server's persistent session directory
   * @param locale                The locale
   * @param clock                 The clock
   */

  public EIServerConfiguration
  {
    Objects.requireNonNull(databases, "databases");
    Objects.requireNonNull(databaseConfiguration, "databaseConfiguration");
    Objects.requireNonNull(adminAddress, "adminAddress");
    Objects.requireNonNull(publicAddress, "publicAddress");
    Objects.requireNonNull(sessionDirectory, "sessionDirectory");
    Objects.requireNonNull(locale, "locale");
    Objects.requireNonNull(clock, "clock");
  }

  /**
   * @return The current time based on the configuration's clock
   */

  public OffsetDateTime now()
  {
    return OffsetDateTime.now(this.clock)
      .withNano(0);
  }
}
