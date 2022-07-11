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

package com.io7m.eigion.storage.derby;

import com.io7m.anethum.common.ParseException;
import com.io7m.eigion.storage.api.EIStorageAbstract;
import com.io7m.eigion.storage.api.EIStorageParameterDescription;
import com.io7m.eigion.storage.api.EIStorageParameters;
import com.io7m.eigion.storage.api.EIStorageType;
import com.io7m.eigion.storage.derby.internal.EIStorageDerby;
import com.io7m.trasco.api.TrEventExecutingSQL;
import com.io7m.trasco.api.TrEventType;
import com.io7m.trasco.api.TrEventUpgrading;
import com.io7m.trasco.api.TrException;
import com.io7m.trasco.api.TrExecutorConfiguration;
import com.io7m.trasco.api.TrSchemaRevisionSet;
import com.io7m.trasco.vanilla.TrExecutors;
import com.io7m.trasco.vanilla.TrSchemaRevisionSetParsers;
import org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.trasco.api.TrExecutorUpgrade.PERFORM_UPGRADES;
import static java.math.BigInteger.valueOf;
import static java.util.Map.entry;

/**
 * A storage implementation that uses Derby as a key/value store.
 */

public final class EIStorageDerbyFactory extends EIStorageAbstract
{
  private static final String LANG_SCHEMA_DOES_NOT_EXIST = "42Y07";
  private static final String LANG_TABLE_NOT_FOUND = "42X05";

  private static final Logger LOG =
    LoggerFactory.getLogger(EIStorageDerbyFactory.class);

  private static final EIStorageParameterDescription PARAMETER_FILE =
    new EIStorageParameterDescription(
      "file",
      "The database file",
      "Path",
      true
    );

  /**
   * A storage implementation that uses Derby as a key/value store.
   */

  public EIStorageDerbyFactory()
  {

  }

  private static void showEvent(
    final TrEventType event)
  {
    if (event instanceof TrEventExecutingSQL sql) {
      LOG.debug("executing: {}", sql);
      return;
    }

    if (event instanceof TrEventUpgrading upgrading) {
      LOG.info(
        "upgrading database from version {} -> {}",
        upgrading.fromVersion(),
        upgrading.toVersion()
      );
    }
  }

  private static void schemaVersionSet(
    final BigInteger version,
    final Connection connection)
    throws SQLException
  {
    final String statementText;
    if (Objects.equals(version, BigInteger.ZERO)) {
      statementText = "insert into schema_version (version_number) values (?)";
    } else {
      statementText = "update schema_version set version_number = ?";
    }

    try (var statement = connection.prepareStatement(statementText)) {
      statement.setLong(1, version.longValueExact());
      statement.execute();
    }
  }

  private static Optional<BigInteger> schemaVersionGet(
    final Connection connection)
    throws SQLException
  {
    Objects.requireNonNull(connection, "connection");

    try {
      final var statementText = "SELECT version_number FROM schema_version";
      LOG.debug("execute: {}", statementText);

      try (var statement = connection.prepareStatement(statementText)) {
        try (var result = statement.executeQuery()) {
          if (!result.next()) {
            throw new SQLException("schema_version table is empty!");
          }
          return Optional.of(valueOf(result.getLong(1)));
        }
      }
    } catch (final SQLException e) {
      final var state = e.getSQLState();
      if (state == null) {
        throw e;
      }
      switch (state) {
        case LANG_SCHEMA_DOES_NOT_EXIST, LANG_TABLE_NOT_FOUND -> {
          return Optional.empty();
        }
        default -> {
          throw e;
        }
      }
    }
  }

  @Override
  protected EIStorageType createActual(
    final EIStorageParameters parameters)
    throws IOException
  {
    try {
      final var dataSource = new EmbeddedConnectionPoolDataSource();
      dataSource.setDatabaseName(parameters.parameters().get(PARAMETER_FILE.name()));
      dataSource.setCreateDatabase("true");
      dataSource.setConnectionAttributes("create=true");

      final var parsers =
        new TrSchemaRevisionSetParsers();

      final TrSchemaRevisionSet revisions;
      try (var stream = EIStorageDerbyFactory.class.getResourceAsStream(
        "/com/io7m/eigion/storage/derby/internal/database.xml")) {
        revisions = parsers.parse(URI.create("urn:source"), stream);
      }

      try (var connection = dataSource.getConnection()) {
        connection.setAutoCommit(false);

        new TrExecutors().create(
          new TrExecutorConfiguration(
            EIStorageDerbyFactory::schemaVersionGet,
            EIStorageDerbyFactory::schemaVersionSet,
            EIStorageDerbyFactory::showEvent,
            revisions,
            PERFORM_UPGRADES,
            connection
          )
        ).execute();
        connection.commit();
      }

      return new EIStorageDerby(dataSource);
    } catch (final ParseException | SQLException | TrException e) {
      throw new IOException(e);
    }
  }

  @Override
  public String name()
  {
    return "derby";
  }

  @Override
  public String description()
  {
    return "Derby-based storage implementation";
  }

  @Override
  public Map<String, EIStorageParameterDescription> parameters()
  {
    return Map.ofEntries(
      entry("file", PARAMETER_FILE)
    );
  }
}
