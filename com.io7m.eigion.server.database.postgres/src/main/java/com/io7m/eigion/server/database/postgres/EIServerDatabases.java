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

package com.io7m.eigion.server.database.postgres;

import com.io7m.anethum.common.ParseException;
import com.io7m.eigion.product.parser.api.EIProductReleaseParsersType;
import com.io7m.eigion.product.parser.api.EIProductReleaseSerializersType;
import com.io7m.eigion.server.database.api.EIServerDatabaseConfiguration;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseFactoryType;
import com.io7m.eigion.server.database.api.EIServerDatabaseType;
import com.io7m.eigion.server.database.postgres.internal.EIServerDatabase;
import com.io7m.trasco.api.TrEventExecutingSQL;
import com.io7m.trasco.api.TrEventType;
import com.io7m.trasco.api.TrEventUpgrading;
import com.io7m.trasco.api.TrException;
import com.io7m.trasco.api.TrExecutorConfiguration;
import com.io7m.trasco.api.TrSchemaRevisionSet;
import com.io7m.trasco.vanilla.TrExecutors;
import com.io7m.trasco.vanilla.TrSchemaRevisionSetParsers;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.postgresql.util.PSQLState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;

import static com.io7m.trasco.api.TrExecutorUpgrade.FAIL_INSTEAD_OF_UPGRADING;
import static com.io7m.trasco.api.TrExecutorUpgrade.PERFORM_UPGRADES;
import static java.math.BigInteger.valueOf;

/**
 * The default postgres server database implementation.
 */

public final class EIServerDatabases implements EIServerDatabaseFactoryType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIServerDatabases.class);

  private final EIProductReleaseSerializersType productsSerializers;
  private final EIProductReleaseParsersType productsParsers;

  /**
   * The default postgres server database implementation.
   *
   * @param inProductsSerializers A product release serializer factory
   * @param inProductsParsers     A product release parser factory
   */

  public EIServerDatabases(
    final EIProductReleaseParsersType inProductsParsers,
    final EIProductReleaseSerializersType inProductsSerializers)
  {
    this.productsParsers =
      Objects.requireNonNull(inProductsParsers, "productsParsers");
    this.productsSerializers =
      Objects.requireNonNull(inProductsSerializers, "productsSerializers");
  }

  /**
   * The default postgres server database implementation.
   */

  public EIServerDatabases()
  {
    this(
      ServiceLoader.load(EIProductReleaseParsersType.class)
        .findFirst()
        .orElseThrow(),
      ServiceLoader.load(EIProductReleaseSerializersType.class)
        .findFirst()
        .orElseThrow()
    );
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
      if (state.equals(PSQLState.UNDEFINED_TABLE.getState())) {
        connection.rollback();
        return Optional.empty();
      }

      throw e;
    }
  }

  @Override
  public EIServerDatabaseType open(
    final EIServerDatabaseConfiguration configuration,
    final Consumer<String> startupMessages)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(configuration, "configuration");
    Objects.requireNonNull(startupMessages, "startupMessages");

    try {
      final var url = new StringBuilder(128);
      url.append("jdbc:postgresql://");
      url.append(configuration.address());
      url.append(":");
      url.append(configuration.port());
      url.append("/");
      url.append(configuration.databaseName());

      final var config = new HikariConfig();
      config.setJdbcUrl(url.toString());
      config.setUsername(configuration.user());
      config.setPassword(configuration.password());
      config.setAutoCommit(false);

      final var dataSource = new HikariDataSource(config);
      final var parsers = new TrSchemaRevisionSetParsers();

      final TrSchemaRevisionSet revisions;
      try (var stream = EIServerDatabases.class.getResourceAsStream(
        "/com/io7m/eigion/server/database/postgres/internal/database.xml")) {
        revisions = parsers.parse(URI.create("urn:source"), stream);
      }

      try (var connection = dataSource.getConnection()) {
        connection.setAutoCommit(false);

        new TrExecutors().create(
          new TrExecutorConfiguration(
            EIServerDatabases::schemaVersionGet,
            EIServerDatabases::schemaVersionSet,
            event -> publishTrEvent(startupMessages, event),
            revisions,
            switch (configuration.upgrade()) {
              case UPGRADE_DATABASE -> PERFORM_UPGRADES;
              case DO_NOT_UPGRADE_DATABASE -> FAIL_INSTEAD_OF_UPGRADING;
            },
            connection
          )
        ).execute();
        connection.commit();
      }

      return new EIServerDatabase(
        configuration.clock(),
        dataSource,
        this.productsSerializers,
        this.productsParsers
      );
    } catch (final IOException e) {
      throw new EIServerDatabaseException(e.getMessage(), e, "resource");
    } catch (final TrException e) {
      throw new EIServerDatabaseException(e.getMessage(), e, "trasco");
    } catch (final ParseException e) {
      throw new EIServerDatabaseException(e.getMessage(), e, "sql-revisions");
    } catch (final SQLException e) {
      throw new EIServerDatabaseException(e.getMessage(), e, "sql-error");
    }
  }

  private static void publishEvent(
    final Consumer<String> startupMessages,
    final String message)
  {
    try {
      LOG.trace("{}", message);
      startupMessages.accept(message);
    } catch (final Exception e) {
      LOG.error("ignored consumer exception: ", e);
    }
  }

  private static void publishTrEvent(
    final Consumer<String> startupMessages,
    final TrEventType event)
  {
    if (event instanceof TrEventExecutingSQL sql) {
      publishEvent(
        startupMessages,
        String.format("Executing SQL: %s", sql.statement())
      );
      return;
    }

    if (event instanceof TrEventUpgrading upgrading) {
      publishEvent(
        startupMessages,
        String.format(
          "Upgrading database from version %s -> %s",
          upgrading.fromVersion(),
          upgrading.toVersion())
      );
      return;
    }
  }
}
