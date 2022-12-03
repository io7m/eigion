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


package com.io7m.eigion.server.vanilla.internal;

import com.io7m.eigion.model.EIPermissionSet;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.server.api.EIServerConfiguration;
import com.io7m.eigion.server.api.EIServerConfiguratorType;
import com.io7m.eigion.server.api.EIServerException;
import com.io7m.eigion.server.database.api.EISDatabaseException;
import com.io7m.eigion.server.database.api.EISDatabaseType;
import com.io7m.eigion.server.database.api.EISDatabaseUsersQueriesType;
import com.io7m.eigion.server.service.telemetry.api.EISTelemetryNoOp;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;

import java.util.Objects;
import java.util.UUID;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.SERVER_STARTUP_ERROR;
import static com.io7m.eigion.server.database.api.EISDatabaseRole.EIGION;

/**
 * The default server.
 */

public final class EIServerConfigurator implements EIServerConfiguratorType
{
  private final CloseableCollectionType<EIServerException> resources;
  private final EISDatabaseType database;

  /**
   * Construct a new server.
   *
   * @param inResources The resources
   * @param inDatabase  The database
   */

  private EIServerConfigurator(
    final CloseableCollectionType<EIServerException> inResources,
    final EISDatabaseType inDatabase)
  {
    this.resources =
      Objects.requireNonNull(inResources, "resources");
    this.database =
      Objects.requireNonNull(inDatabase, "database");
  }

  /**
   * Create a server configurator.
   *
   * @param configuration The configuration
   *
   * @return A configurator
   *
   * @throws EIServerException On errors
   */

  public static EIServerConfiguratorType create(
    final EIServerConfiguration configuration)
    throws EIServerException
  {
    try {
      final var resources =
        CloseableCollection.create(() -> {
          return new EIServerException(
            SERVER_STARTUP_ERROR,
            "Server failed to start."
          );
        });

      final var telemetry =
        EISTelemetryNoOp.noop();

      final var database =
        resources.add(
          configuration.databases()
            .open(
              configuration.databaseConfiguration(),
              telemetry.openTelemetry(),
              statement -> {

              })
        );

      return new EIServerConfigurator(resources, database);
    } catch (final EISDatabaseException e) {
      throw new EIServerException(SERVER_STARTUP_ERROR, e);
    }
  }

  @Override
  public EISDatabaseType database()
  {
    return this.database;
  }

  @Override
  public void userSetPermissions(
    final UUID userId,
    final EIPermissionSet permissions)
    throws EIServerException
  {
    try (var c = this.database.openConnection(EIGION)) {
      try (var t = c.openTransaction()) {
        final var u =
          t.queries(EISDatabaseUsersQueriesType.class);

        final var originalUser =
          u.userGet(userId)
            .orElseGet(() -> new EIUser(userId, permissions));

        final var newUser = new EIUser(originalUser.id(), permissions);
        u.userPut(newUser);
        t.commit();
      }
    } catch (final EISDatabaseException e) {
      throw new EIServerException(e.errorCode(), e);
    }
  }

  @Override
  public void close()
    throws EIServerException
  {
    this.resources.close();
  }
}
