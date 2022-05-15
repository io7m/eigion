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

import java.time.Clock;
import java.util.Objects;

/**
 * The server database configuration.
 *
 * @param user         The username with which to connect
 * @param password     The password with which to connect
 * @param port         The database TCP/IP port
 * @param upgrade      The upgrade specification
 * @param create       The creation specification
 * @param address      The database address
 * @param databaseName The database name
 * @param clock        A clock for time retrievals
 */

public record EIServerDatabaseConfiguration(
  String user,
  String password,
  String address,
  int port,
  String databaseName,
  EIServerDatabaseCreate create,
  EIServerDatabaseUpgrade upgrade,
  Clock clock)
{
  /**
   * The server database configuration.
   *
   * @param user         The username with which to connect
   * @param password     The password with which to connect
   * @param port         The database TCP/IP port
   * @param upgrade      The upgrade specification
   * @param create       The creation specification
   * @param address      The database address
   * @param databaseName The database name
   * @param clock        A clock for time retrievals
   */

  public EIServerDatabaseConfiguration
  {
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(address, "address");
    Objects.requireNonNull(databaseName, "databaseName");
    Objects.requireNonNull(create, "create");
    Objects.requireNonNull(upgrade, "upgrade");
    Objects.requireNonNull(clock, "clock");
  }
}
