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

package com.io7m.eigion.tests;

import com.io7m.eigion.server.api.EIServerConfiguration;
import com.io7m.eigion.server.database.api.EIServerDatabaseConfiguration;
import com.io7m.eigion.server.database.api.EIServerDatabaseCreate;
import com.io7m.eigion.server.database.api.EIServerDatabaseUpgrade;
import com.io7m.eigion.server.database.postgres.EIServerDatabases;
import com.io7m.eigion.server.vanilla.EIServers;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.time.Clock;
import java.util.Locale;

public final class EIServerDemo
{
  private EIServerDemo()
  {

  }

  public static void main(
    final String[] args)
    throws Exception
  {
    final var databaseConfiguration =
      new EIServerDatabaseConfiguration(
        "postgres",
        "12345678",
        "localhost",
        5432,
        "postgres",
        EIServerDatabaseCreate.CREATE_DATABASE,
        EIServerDatabaseUpgrade.UPGRADE_DATABASE,
        Clock.systemUTC()
      );

    final var serverConfiguration =
      new EIServerConfiguration(
        new EIServerDatabases(),
        databaseConfiguration,
        new InetSocketAddress("localhost", 40000),
        new InetSocketAddress("localhost", 40001),
        Files.createTempDirectory("eigion"),
        Locale.getDefault(),
        Clock.systemUTC()
      );

    final var servers = new EIServers();

    try (var server = servers.createServer(serverConfiguration)) {
      server.start();
      while (true) {
        Thread.sleep(1_000L);
      }
    }
  }
}
