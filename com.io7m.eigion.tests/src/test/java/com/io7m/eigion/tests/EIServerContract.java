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

import com.io7m.eigion.model.EIPassword;
import com.io7m.eigion.model.EIPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.eigion.model.EIPasswordException;
import com.io7m.eigion.server.api.EIServerClosed;
import com.io7m.eigion.server.api.EIServerConfiguration;
import com.io7m.eigion.server.api.EIServerEventType;
import com.io7m.eigion.server.api.EIServerRequestProcessed;
import com.io7m.eigion.server.api.EIServerStarted;
import com.io7m.eigion.server.api.EIServerStarting;
import com.io7m.eigion.server.api.EIServerType;
import com.io7m.eigion.server.api.EIServerUserLoggedIn;
import com.io7m.eigion.server.database.api.EIServerDatabaseConfiguration;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseUsersQueriesType;
import com.io7m.eigion.server.database.postgres.EIServerDatabases;
import com.io7m.eigion.server.protocol.api.EIServerProtocolException;
import com.io7m.eigion.server.protocol.public_api.v1.EISP1MessageType;
import com.io7m.eigion.server.protocol.public_api.v1.EISP1Messages;
import com.io7m.eigion.server.protocol.versions.EISVMessages;
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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Flow;

import static com.io7m.eigion.server.database.api.EIServerDatabaseCreate.CREATE_DATABASE;
import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.EIGION;
import static com.io7m.eigion.server.database.api.EIServerDatabaseUpgrade.UPGRADE_DATABASE;
import static java.nio.charset.StandardCharsets.UTF_8;

@Testcontainers(disabledWithoutDocker = true)
public abstract class EIServerContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIServerContract.class);

  @Container
  private final PostgreSQLContainer<?> container =
    new PostgreSQLContainer<>("postgres")
      .withDatabaseName("postgres")
      .withUsername("postgres")
      .withPassword("12345678");

  private EIServers servers;
  private EIServerType server;
  private EICapturingDatabases databases;
  private HttpClient httpClient;
  private ArrayList<String> events;
  private EISP1Messages messagesV1;
  private Path directory;
  private CookieManager cookies;
  private EISVMessages messagesV;
  private EIFakeStorageFactory storage;

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

  protected final UUID createUserSomeone()
    throws EIServerDatabaseException, EIPasswordException
  {
    final var database = this.databases.mostRecent();
    try (var connection = database.openConnection(EIGION)) {
      try (var transaction = connection.openTransaction()) {
        final var users =
          transaction.queries(EIServerDatabaseUsersQueriesType.class);
        final var userId = UUID.randomUUID();
        users.userCreate(
          userId,
          "someone",
          "someone@example.com",
          timeNow(),
          createBadPassword()
        );
        transaction.commit();
        return userId;
      }
    }
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

  public final EISP1Messages messagesV1()
  {
    return this.messagesV1;
  }

  public final Path directory()
  {
    return this.directory;
  }

  @BeforeEach
  public final void setup()
    throws Exception
  {
    this.directory =
      EITestDirectories.createTempDirectory();
    this.servers =
      new EIServers();
    this.databases =
      new EICapturingDatabases(new EIServerDatabases());
    this.storage =
      new EIFakeStorageFactory();

    this.server =
      this.createServer();
    this.events =
      new ArrayList<String>();
    this.messagesV1 =
      new EISP1Messages();
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
        this.databases,
        databaseConfiguration,
        this.storage,
        new EIStorageParameters(Map.of()),
        new InetSocketAddress("localhost", 40000),
        new InetSocketAddress("localhost", 40001),
        this.directory,
        Locale.getDefault(),
        Clock.systemUTC()
      )
    );
  }

  protected final HttpResponse<byte[]> postPublicText(
    final String endpoint,
    final String text)
    throws Exception
  {
    return this.postPublicBytes(
      endpoint, text.getBytes(UTF_8)
    );
  }

  protected final HttpResponse<byte[]> postPublicBytes(
    final String endpoint,
    final byte[] text)
    throws Exception
  {
    final var request =
      HttpRequest.newBuilder()
        .POST(HttpRequest.BodyPublishers.ofByteArray(text))
        .uri(URI.create("http://localhost:40001" + endpoint))
        .build();

    return this.httpClient.send(
      request,
      HttpResponse.BodyHandlers.ofByteArray()
    );
  }

  protected final HttpResponse<byte[]> getPublic(
    final String endpoint)
    throws Exception
  {
    final var request =
      HttpRequest.newBuilder()
        .GET()
        .uri(URI.create("http://localhost:40001" + endpoint))
        .build();

    return this.httpClient.send(
      request,
      HttpResponse.BodyHandlers.ofByteArray()
    );
  }

  protected final HttpResponse<byte[]> getAdmin(
    final String endpoint)
    throws Exception
  {
    final var request =
      HttpRequest.newBuilder()
        .GET()
        .uri(URI.create("http://localhost:40000" + endpoint))
        .build();

    return this.httpClient.send(
      request,
      HttpResponse.BodyHandlers.ofByteArray()
    );
  }

  protected final <T extends EISP1MessageType> T parsePublic(
    final HttpResponse<byte[]> response,
    final Class<T> clazz)
    throws EIServerProtocolException
  {
    final var bodyText = response.body();
    LOG.debug("received: {}", bodyText);
    return clazz.cast(this.messagesV1().parse(bodyText));
  }
}
