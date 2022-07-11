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

package com.io7m.eigion.storage.derby.internal;

import com.io7m.eigion.hash.EIHash;
import com.io7m.eigion.storage.api.EIStorageName;
import com.io7m.eigion.storage.api.EIStorageType;
import com.io7m.eigion.storage.api.EIStored;
import org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource;
import org.jooq.impl.DSL;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.eigion.storage.derby.internal.tables.Binaries.BINARIES;
import static org.jooq.SQLDialect.DERBY;

/**
 * A storage implementation that uses Derby as a key/value store.
 */

public final class EIStorageDerby implements EIStorageType
{
  private final EmbeddedConnectionPoolDataSource dataSource;

  /**
   * A storage implementation that uses Derby as a key/value store.
   *
   * @param inDataSource The data source
   */

  public EIStorageDerby(
    final EmbeddedConnectionPoolDataSource inDataSource)
  {
    this.dataSource =
      Objects.requireNonNull(inDataSource, "dataSource");
  }

  private Connection openConnection()
    throws SQLException
  {
    final var pooledConnection =
      this.dataSource.getPooledConnection();
    final var connection =
      pooledConnection.getConnection();
    connection.setAutoCommit(false);
    return connection;
  }


  @Override
  public void put(
    final EIStorageName name,
    final String contentType,
    final EIHash hash,
    final InputStream data)
    throws IOException
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(contentType, "contentType");
    Objects.requireNonNull(hash, "hash");
    Objects.requireNonNull(data, "data");

    try (var connection = this.openConnection()) {
      final var context = DSL.using(connection, DERBY);
      final var blob = data.readAllBytes();
      final var rec = context.newRecord(BINARIES);
      rec.setContentType(contentType);
      rec.setData(blob);
      rec.setHashAlgorithm(hash.algorithm());
      rec.setHashValue(hash.hash());
      rec.setName(name.name());
      rec.store();
      connection.commit();
    } catch (final SQLException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void delete(
    final EIStorageName name)
    throws IOException
  {
    Objects.requireNonNull(name, "name");

    try (var connection = this.openConnection()) {
      final var context = DSL.using(connection, DERBY);
      final var rec = context.fetchOne(BINARIES);
      if (rec != null) {
        rec.delete();
        connection.commit();
      }
    } catch (final SQLException e) {
      throw new IOException(e);
    }
  }

  @Override
  public Optional<EIStored> get(
    final EIStorageName name)
    throws IOException
  {
    Objects.requireNonNull(name, "name");

    try (var connection = this.openConnection()) {
      final var context = DSL.using(connection, DERBY);
      final var rec = context.fetchOne(BINARIES);
      if (rec != null) {
        final var data =
          rec.getData();
        final var contentType =
          rec.getContentType();
        final var hashAlgorithm =
          rec.getHashAlgorithm();
        final var hashValue =
          rec.getHashValue();
        final var contentSize =
          Integer.toUnsignedLong(data.length);

        final var stored =
          new EIStored(
            name,
            contentType,
            contentSize,
            new EIHash(hashAlgorithm, hashValue),
            new ByteArrayInputStream(data)
          );

        connection.rollback();
        return Optional.of(stored);
      }

      connection.rollback();
      return Optional.empty();
    } catch (final SQLException e) {
      throw new IOException(e);
    }
  }
}
