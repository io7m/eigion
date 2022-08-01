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

import com.io7m.eigion.model.EIAdminPermission;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIGroupPrefix;
import com.io7m.eigion.model.EIGroupRole;
import com.io7m.eigion.model.EIPassword;
import com.io7m.eigion.model.EIPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.eigion.model.EIPasswordException;
import com.io7m.eigion.model.EIUserDisplayName;
import com.io7m.eigion.model.EIUserEmail;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandLogin;
import com.io7m.eigion.protocol.admin_api.v1.EISA1Messages;
import com.io7m.eigion.protocol.api.EIProtocolException;
import com.io7m.eigion.protocol.public_api.v1.EISP1Messages;
import com.io7m.eigion.protocol.versions.EISVMessages;
import com.io7m.eigion.server.api.EIServerClosed;
import com.io7m.eigion.server.api.EIServerConfiguration;
import com.io7m.eigion.server.api.EIServerEventType;
import com.io7m.eigion.server.api.EIServerException;
import com.io7m.eigion.server.api.EIServerRequestProcessed;
import com.io7m.eigion.server.api.EIServerStarted;
import com.io7m.eigion.server.api.EIServerStarting;
import com.io7m.eigion.server.api.EIServerType;
import com.io7m.eigion.server.api.EIServerUserLoggedIn;
import com.io7m.eigion.server.database.api.EIServerDatabaseAdminsQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseConfiguration;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseGroupsQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseUsersQueriesType;
import com.io7m.eigion.server.database.postgres.EIServerDatabases;
import com.io7m.eigion.server.vanilla.EIServers;
import com.io7m.eigion.storage.api.EIStorageParameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.CookieManager;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.io7m.eigion.model.EIGroupRole.FOUNDER;
import static com.io7m.eigion.server.database.api.EIServerDatabaseCreate.CREATE_DATABASE;
import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.EIGION;
import static com.io7m.eigion.server.database.api.EIServerDatabaseUpgrade.UPGRADE_DATABASE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public abstract class EIServerContract
  implements EIServerPublicMessengerType, EIServerAdminMessengerType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIServerContract.class);

  @Container
  private final PostgreSQLContainer<?> container =
    new PostgreSQLContainer<>("postgres")
      .withDatabaseName("postgres")
      .withUsername("postgres")
      .withPassword("12345678");

  private ArrayList<String> events;
  private CookieManager cookies;
  private EICapturingDatabases databases;
  private EIFakeStorageFactory storage;
  private EISA1Messages messagesAdminV1;
  private EISP1Messages messagesPublicV1;
  private EISVMessages messagesV;
  private EIServerType server;
  private EIServers servers;
  private HttpClient httpClient;
  private Path directory;
  private EIFakeDomainCheckers domainCheckers;
  private AtomicBoolean started;

  private static String describeEvent(
    final EIServerEventType item)
  {
    if (item instanceof EIServerStarted) {
      return "STARTED";
    }
    if (item instanceof EIServerStarting starting) {
      return "STARTING: " + starting.message();
    }
    if (item instanceof EIServerClosed) {
      return "CLOSED";
    }
    if (item instanceof EIServerUserLoggedIn userLoggedIn) {
      return "USER_LOGGED_IN " + userLoggedIn.userName();
    }
    if (item instanceof EIServerRequestProcessed requestProcessed) {
      return "REQUEST_PROCESSED " + requestProcessed.requestLine() + " " + requestProcessed.status();
    }

    throw new IllegalStateException("Unrecognized event: " + item);
  }

  protected static EIPassword createBadPassword()
    throws EIPasswordException
  {
    return EIPasswordAlgorithmPBKDF2HmacSHA256.create()
      .createHashed("12345678");
  }

  protected static OffsetDateTime timeNow()
  {
    return OffsetDateTime.now(Clock.systemUTC()).withNano(0);
  }

  @Override
  public final Logger msgLogger()
  {
    return LOG;
  }

  @Override
  public final HttpClient msgHttpClient()
  {
    return this.httpClient;
  }

  @Override
  public final EISP1Messages msgPublic()
  {
    return this.messagesPublicV1;
  }

  protected final EIFakeStorageFactory storage()
  {
    return this.storage;
  }

  protected final EICapturingDatabases databases()
  {
    return this.databases;
  }

  protected final HttpClient httpClient()
  {
    return this.httpClient;
  }

  protected final EISVMessages messagesV()
  {
    return this.messagesV;
  }

  protected final EIFakeDomainCheckers domainCheckers()
  {
    return this.domainCheckers;
  }

  protected final UUID createUserSomeone(
    final UUID adminId)
    throws EIServerDatabaseException, EIPasswordException
  {
    return this.createUser(adminId, "someone");
  }

  protected final UUID createUser(
    final UUID adminId,
    final String name)
    throws EIServerDatabaseException, EIPasswordException
  {
    final var database = this.databases.mostRecent();
    try (var connection = database.openConnection(EIGION)) {
      try (var transaction = connection.openTransaction()) {
        transaction.adminIdSet(adminId);

        final var users =
          transaction.queries(EIServerDatabaseUsersQueriesType.class);
        final var userId = UUID.randomUUID();
        users.userCreate(
          userId,
          new EIUserDisplayName(name),
          new EIUserEmail(name + "@example.com"),
          timeNow(),
          createBadPassword()
        );
        transaction.commit();
        return userId;
      }
    }
  }

  protected final EIGroupName createGroup(
    final UUID userFounder,
    final String name)
    throws EIServerDatabaseException, EIPasswordException
  {
    final var database = this.databases.mostRecent();
    try (var connection = database.openConnection(EIGION)) {
      try (var transaction = connection.openTransaction()) {
        final var groups =
          transaction.queries(EIServerDatabaseGroupsQueriesType.class);

        final var groupName = new EIGroupName(name);
        groups.groupCreate(groupName, userFounder);
        groups.groupMembershipSet(groupName, userFounder, Set.of(FOUNDER));
        transaction.commit();
        return groupName;
      }
    }
  }

  @Override
  public EISA1Messages msgAdmin()
  {
    return this.messagesAdminV1;
  }

  public final CookieManager cookies()
  {
    return this.cookies;
  }

  public final PostgreSQLContainer<?> container()
  {
    return this.container;
  }

  public final EIServerType server()
  {
    return this.server;
  }

  public final EISP1Messages messagesPublicV1()
  {
    return this.messagesPublicV1;
  }

  public final EISA1Messages messagesAdminV1()
  {
    return this.messagesAdminV1;
  }

  public final Path directory()
  {
    return this.directory;
  }

  @BeforeEach
  public final void setup()
    throws Exception
  {
    this.started =
      new AtomicBoolean(false);
    this.directory =
      EITestDirectories.createTempDirectory();
    this.servers =
      new EIServers();
    this.databases =
      new EICapturingDatabases(new EIServerDatabases());
    this.storage =
      new EIFakeStorageFactory();
    this.domainCheckers =
      new EIFakeDomainCheckers();

    this.server =
      this.createServer();
    this.events =
      new ArrayList<String>();
    this.messagesPublicV1 =
      new EISP1Messages();
    this.messagesAdminV1 =
      new EISA1Messages();
    this.messagesV =
      new EISVMessages();

    this.server.events()
      .subscribe(new Flow.Subscriber<>()
      {
        private Flow.Subscription sub;

        @Override
        public void onSubscribe(
          final Flow.Subscription subscription)
        {
          this.sub = subscription;
          subscription.request(1L);
        }

        @Override
        public void onNext(
          final EIServerEventType item)
        {
          final var text = describeEvent(item);
          LOG.debug("event: {}", text);
          EIServerContract.this.events.add(text);
          this.sub.request(1L);
        }

        @Override
        public void onError(
          final Throwable throwable)
        {

        }

        @Override
        public void onComplete()
        {

        }
      });

    this.cookies =
      new CookieManager();

    this.httpClient =
      HttpClient.newBuilder()
        .cookieHandler(this.cookies)
        .build();
  }

  @AfterEach
  public final void tearDown()
    throws Exception
  {
    this.server.close();
  }

  protected final EIServerType createServer()
  {
    LOG.debug("creating server");

    final var databaseConfiguration =
      new EIServerDatabaseConfiguration(
        "postgres",
        "12345678",
        this.container.getContainerIpAddress(),
        this.container.getFirstMappedPort().intValue(),
        "postgres",
        CREATE_DATABASE,
        UPGRADE_DATABASE,
        Clock.systemUTC()
      );

    return this.servers.createServer(
      new EIServerConfiguration(
        Locale.getDefault(),
        Clock.systemUTC(),
        this.domainCheckers,
        this.databases,
        databaseConfiguration,
        this.storage,
        new EIStorageParameters(Map.of()),
        new InetSocketAddress("localhost", 40000),
        new InetSocketAddress("localhost", 40001),
        this.directory,
        new EIGroupPrefix("com.eigion.users.")
      )
    );
  }

  protected final <T> T parseV(
    final HttpResponse<byte[]> response,
    final Class<T> clazz)
    throws EIProtocolException
  {
    final var bodyText = response.body();
    LOG.debug("received: {}", new String(bodyText, UTF_8));
    return clazz.cast(this.messagesV().parse(bodyText));
  }

  protected final void doLoginAdmin(
    final String user,
    final String pass)
    throws Exception
  {
    final var r =
      this.msgSendAdminBytes(
        "/admin/1/0/login",
        this.messagesAdminV1().serialize(
          new EISA1CommandLogin(user, pass))
      );
    assertEquals(200, r.statusCode());
  }

  protected final UUID createAdminInitial(
    final String user,
    final String pass)
    throws Exception
  {
    this.serverStartIfNecessary();

    final var database = this.databases.mostRecent();
    try (var c = database.openConnection(EIGION)) {
      try (var t = c.openTransaction()) {
        final var q =
          t.queries(EIServerDatabaseAdminsQueriesType.class);

        final var password =
          EIPasswordAlgorithmPBKDF2HmacSHA256.create()
            .createHashed(pass);

        final var adminId = UUID.randomUUID();
        q.adminCreateInitial(
          adminId,
          user,
          UUID.randomUUID() + "@example.com",
          OffsetDateTime.now(),
          password
        );
        t.commit();
        return adminId;
      }
    }
  }

  protected final UUID createAdmin(
    final UUID owner,
    final String user,
    final String pass,
    final Set<EIAdminPermission> permissions)
    throws Exception
  {
    this.serverStartIfNecessary();

    final var database = this.databases.mostRecent();
    try (var c = database.openConnection(EIGION)) {
      try (var t = c.openTransaction()) {
        t.adminIdSet(owner);

        final var q =
          t.queries(EIServerDatabaseAdminsQueriesType.class);

        final var password =
          EIPasswordAlgorithmPBKDF2HmacSHA256.create()
            .createHashed(pass);

        final var newId = UUID.randomUUID();
        q.adminCreate(
          newId,
          user,
          newId + "@example.com",
          OffsetDateTime.now(),
          password,
          permissions
        );
        t.commit();
        return newId;
      }
    }
  }

  protected final void serverStartIfNecessary()
  {
    assertTrue(
      this.container().isRunning(),
      "Container must be running!"
    );

    if (this.started.compareAndSet(false, true)) {
      try {
        this.server.start();
      } catch (final EIServerException e) {
        this.started.set(false);
        throw new IllegalStateException(e);
      }
    }
  }

  protected final void setGroupRoles(
    final UUID user,
    final String groupName,
    final Set<EIGroupRole> roles)
    throws Exception
  {
    this.serverStartIfNecessary();

    final var database = this.databases.mostRecent();
    try (var c = database.openConnection(EIGION)) {
      try (var t = c.openTransaction()) {
        final var q =
          t.queries(EIServerDatabaseGroupsQueriesType.class);
        q.groupMembershipSet(new EIGroupName(groupName), user, roles);
        t.commit();
      }
    }
  }
}
