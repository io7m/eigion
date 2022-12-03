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

package com.io7m.eigion.tests.extensions;

import com.io7m.eigion.tests.EIFakeClock;
import com.io7m.idstore.database.api.IdDatabaseConfiguration;
import com.io7m.idstore.database.api.IdDatabaseCreate;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.database.api.IdDatabaseUpgrade;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.database.postgres.IdDatabases;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.server.api.IdServerBrandingConfiguration;
import com.io7m.idstore.server.api.IdServerConfiguration;
import com.io7m.idstore.server.api.IdServerHTTPServiceConfiguration;
import com.io7m.idstore.server.api.IdServerHistoryConfiguration;
import com.io7m.idstore.server.api.IdServerMailConfiguration;
import com.io7m.idstore.server.api.IdServerMailTransportSMTP;
import com.io7m.idstore.server.api.IdServerRateLimitConfiguration;
import com.io7m.idstore.server.api.IdServerSessionConfiguration;
import com.io7m.idstore.server.api.IdServerType;
import com.io7m.idstore.server.vanilla.IdServers;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jmulticlose.core.ClosingResourceFailedException;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

public final class EIIdStoreExtension
  implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback,
  ExtensionContext.Store.CloseableResource,
  ParameterResolver
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIIdStoreExtension.class);

  private static final PostgreSQLContainer<?> CONTAINER =
    new PostgreSQLContainer<>(
      DockerImageName.parse("postgres")
        .withTag("14.4"))
      .withDatabaseName("idstore")
      .withUsername("postgres")
      .withPassword("12345678");

  public static final URI IDSTORE_USER_API =
    URI.create("http://localhost:50000/");

  public static final URI IDSTORE_USER_VIEW =
    URI.create("http://localhost:50001/");

  public static final URI IDSTORE_ADMIN_API =
    URI.create("http://localhost:51000/");

  private EIFakeClock idstoreClock;
  private CloseableCollectionType<ClosingResourceFailedException> globalResources;
  private CloseableCollectionType<ClosingResourceFailedException> perTestResources;
  private AtomicBoolean started;
  private IdServerType idstore;
  private UUID adminUUID;
  private EIIdStoreType idstoreAPI;
  private IdServerConfiguration idstoreConfiguration;
  private IdDatabaseType idstoreDatabase;

  public EIIdStoreExtension()
  {
    this.globalResources =
      CloseableCollection.create();
    this.perTestResources =
      CloseableCollection.create();
    this.idstoreClock =
      new EIFakeClock();
    this.started =
      new AtomicBoolean(false);
  }

  @Override
  public void beforeAll(
    final ExtensionContext context)
    throws Exception
  {
    if (this.started.compareAndSet(false, true)) {
      context.getRoot()
        .getStore(GLOBAL)
        .put("com.io7m.eigion.tests.extensions.EIIdStoreExtension", this);

      CONTAINER.start();
      CONTAINER.addEnv("PGPASSWORD", "12345678");

      this.globalResources.add(() -> {
        CONTAINER.execInContainer(
          "dropdb",
          "-w",
          "-U",
          "postgres",
          "idstore"
        );
        CONTAINER.stop();
      });
    }
  }

  @Override
  public void close()
    throws Throwable
  {
    LOG.debug("tearing down idstore container");
    this.perTestResources.close();
    this.globalResources.close();
  }

  @Override
  public void beforeEach(
    final ExtensionContext context)
    throws Exception
  {
    LOG.debug("setting up idstore");

    final var r0 =
      CONTAINER.execInContainer(
        "dropdb",
        "-w",
        "-U",
        "postgres",
        "idstore"
      );
    LOG.debug("stderr: {}", r0.getStderr());

    final var r1 =
      CONTAINER.execInContainer(
        "createdb",
        "-w",
        "-U",
        "postgres",
        "idstore"
      );

    LOG.debug("stderr: {}", r0.getStderr());
    assertEquals(0, r1.getExitCode());

    this.perTestResources =
      CloseableCollection.create();
    this.idstoreClock =
      new EIFakeClock();
    this.idstore =
      this.perTestResources.add(this.createIdstoreServer());

    this.adminUUID = UUID.randomUUID();
    this.idstore.setup(
      Optional.of(this.adminUUID),
      new IdName("admin"),
      new IdEmail("admin@example.com"),
      new IdRealName("An Admin"),
      "12345678"
    );

    this.idstore.start();
    this.idstoreDatabase = this.idstore.database();
    this.idstoreAPI = new EIIdStore(this, this.idstoreDatabase);
  }

  private IdServerType createIdstoreServer()
  {
    LOG.debug("creating idstore server");

    final var databaseConfiguration =
      new IdDatabaseConfiguration(
        "postgres",
        "12345678",
        CONTAINER.getHost(),
        CONTAINER.getFirstMappedPort().intValue(),
        "idstore",
        IdDatabaseCreate.CREATE_DATABASE,
        IdDatabaseUpgrade.UPGRADE_DATABASE,
        this.idstoreClock
      );

    final var mailService =
      new IdServerMailConfiguration(
        new IdServerMailTransportSMTP("localhost", 25000),
        Optional.empty(),
        "no-reply@example.com",
        Duration.ofDays(1L)
      );

    final var userApiService =
      new IdServerHTTPServiceConfiguration(
        "localhost",
        50000,
        IDSTORE_USER_API
      );
    final var userViewService =
      new IdServerHTTPServiceConfiguration(
        "localhost",
        50001,
        IDSTORE_USER_VIEW
      );
    final var adminApiService =
      new IdServerHTTPServiceConfiguration(
        "localhost",
        51000,
        IDSTORE_ADMIN_API
      );

    final var branding =
      new IdServerBrandingConfiguration(
        "idstore",
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
      );

    final var history =
      new IdServerHistoryConfiguration(100, 100);

    final var rateLimit =
      new IdServerRateLimitConfiguration(
        Duration.of(10L, ChronoUnit.MINUTES),
        Duration.of(10L, ChronoUnit.MINUTES)
      );

    final var sessions =
      new IdServerSessionConfiguration(
        Duration.of(10L, ChronoUnit.MINUTES),
        Duration.of(10L, ChronoUnit.MINUTES)
      );

    this.idstoreConfiguration =
      new IdServerConfiguration(
        Locale.getDefault(),
        this.idstoreClock,
        new IdDatabases(),
        databaseConfiguration,
        mailService,
        userApiService,
        userViewService,
        adminApiService,
        sessions,
        branding,
        history,
        rateLimit,
        Optional.empty()
      );

    return new IdServers().createServer(this.idstoreConfiguration);
  }

  @Override
  public void afterEach(
    final ExtensionContext context)
    throws Exception
  {
    LOG.debug("tearing down idstore");
    this.perTestResources.close();
    LOG.debug("tore down idstore");
  }

  @Override
  public boolean supportsParameter(
    final ParameterContext parameterContext,
    final ExtensionContext extensionContext)
    throws ParameterResolutionException
  {
    final Class<?> requiredType =
      parameterContext.getParameter().getType();

    return Objects.equals(requiredType, IdServerType.class)
           || Objects.equals(requiredType, EIIdStoreType.class);
  }

  @Override
  public Object resolveParameter(
    final ParameterContext parameterContext,
    final ExtensionContext extensionContext)
    throws ParameterResolutionException
  {
    final Class<?> requiredType =
      parameterContext.getParameter().getType();

    if (Objects.equals(requiredType, IdServerType.class)) {
      return this.idstore;
    }

    if (Objects.equals(requiredType, EIIdStoreType.class)) {
      return this.idstoreAPI;
    }

    throw new IllegalStateException(
      "Unrecognized requested parameter type: %s".formatted(requiredType)
    );
  }

  private static final class EIIdStore implements EIIdStoreType
  {
    private final EIIdStoreExtension extension;
    private final IdDatabaseType database;

    EIIdStore(
      final EIIdStoreExtension inExtension,
      final IdDatabaseType inDatabase)
    {
      this.extension =
        Objects.requireNonNull(inExtension, "inExtension");
      this.database =
        Objects.requireNonNull(inDatabase, "database");
    }

    @Override
    public UUID createUser(
      final String name,
      final String password)
      throws Exception
    {
      try (var c = this.database.openConnection(IDSTORE)) {
        try (var t = c.openTransaction()) {
          t.adminIdSet(this.extension.adminUUID);
          final var u =
            t.queries(IdDatabaseUsersQueriesType.class);

          final var user =
            u.userCreate(
              new IdName(name),
              new IdRealName(name),
              new IdEmail("%s@example.org".formatted(name)),
              IdPasswordAlgorithmPBKDF2HmacSHA256.create().createHashed(password)
            );
          t.commit();
          return user.id();
        }
      }
    }

    @Override
    public UUID admin()
    {
      return this.extension.adminUUID;
    }

    @Override
    public URI baseUserAPI()
    {
      return this.extension.idstoreConfiguration.userApiAddress()
        .externalAddress();
    }

    @Override
    public URI baseUserView()
    {
      return this.extension.idstoreConfiguration.userViewAddress()
        .externalAddress();
    }

    @Override
    public URI baseAdminAPI()
    {
      return this.extension.idstoreConfiguration.adminApiAddress()
        .externalAddress();
    }
  }
}
