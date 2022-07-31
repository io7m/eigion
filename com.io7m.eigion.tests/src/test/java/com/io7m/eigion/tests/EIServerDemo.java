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

import com.io7m.eigion.domaincheck.EIDomainCheckers;
import com.io7m.eigion.model.EIGroupPrefix;
import com.io7m.eigion.model.EIPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.eigion.model.EIPasswordException;
import com.io7m.eigion.server.api.EIServerConfiguration;
import com.io7m.eigion.server.api.EIServerType;
import com.io7m.eigion.server.database.api.EIServerDatabaseAdminsQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseConfiguration;
import com.io7m.eigion.server.database.api.EIServerDatabaseCreate;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseUpgrade;
import com.io7m.eigion.server.database.postgres.EIServerDatabases;
import com.io7m.eigion.server.vanilla.EIServers;
import com.io7m.eigion.storage.api.EIStorageParameters;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.EIGION;

public final class EIServerDemo
{
  private EIServerDemo()
  {

  }

  public static void main(
    final String[] args)
    throws Exception
  {
    System.setProperty("org.jooq.no-tips", "true");
    System.setProperty("org.jooq.no-logo", "true");

    final var databaseConfiguration =
      new EIServerDatabaseConfiguration(
        "postgres",
        "12345678",
        "localhost",
        54320,
        "postgres",
        EIServerDatabaseCreate.CREATE_DATABASE,
        EIServerDatabaseUpgrade.UPGRADE_DATABASE,
        Clock.systemUTC()
      );

    final var serverConfiguration =
      new EIServerConfiguration(
        Locale.getDefault(),
        Clock.systemUTC(),
        new EIDomainCheckers(),
        new EIServerDatabases(),
        databaseConfiguration,
        new EIFakeStorageFactory(),
        new EIStorageParameters(Map.of()),
        new InetSocketAddress("localhost", 40000),
        new InetSocketAddress("localhost", 40001),
        Files.createTempDirectory("eigion"),
        new EIGroupPrefix("com.eigion.users.")
      );

    final var servers = new EIServers();

    try (var server = servers.createServer(serverConfiguration)) {
      server.start();
      createInitialAdmin(server);

      while (true) {
        Thread.sleep(1_000L);
      }
    }
  }

  private static void createInitialAdmin(
    final EIServerType server)
  {
    try {
      final var db = server.database();
      try (var c = db.openConnection(EIGION)) {
        try (var t = c.openTransaction()) {
          final var q = t.queries(EIServerDatabaseAdminsQueriesType.class);
          final var algo = EIPasswordAlgorithmPBKDF2HmacSHA256.create();
          final var password = algo.createHashed("12345678");
          q.adminCreateInitial(
            UUID.randomUUID(),
            "someone",
            "someone@example.com",
            OffsetDateTime.now(),
            password
          );
          t.commit();
        }
      }
    } catch (final EIServerDatabaseException | EIPasswordException e) {
      // Don't care
    }
  }
}
