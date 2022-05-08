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

package com.io7m.eigion.client.vanilla.v1;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.io7m.dixmont.core.DmJsonRestrictedDeserializers;
import com.io7m.eigion.client.api.EIClientConfiguration;
import com.io7m.eigion.client.api.EIClientLoginFailed;
import com.io7m.eigion.client.api.EIClientLoginStatusType;
import com.io7m.eigion.client.api.EIClientNewsItem;
import com.io7m.eigion.client.api.EIClientOnline;
import com.io7m.eigion.client.api.EIClientType;
import com.io7m.eigion.client.vanilla.internal.EIClientStrings;
import com.io7m.eigion.client.vanilla.v1.EIV1ClientMessagesType.EIV1LoginResponse;
import com.io7m.eigion.client.vanilla.v1.EIV1ClientMessagesType.EIV1News;
import com.io7m.eigion.client.vanilla.v1.EIV1ClientMessagesType.EIV1NewsItem;
import com.io7m.eigion.taskrecorder.EITask;
import com.io7m.jattribute.core.AttributeReadableType;
import com.io7m.jattribute.core.AttributeType;
import com.io7m.jattribute.core.Attributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;

import static com.io7m.eigion.client.api.EIClientLoggedIn.CLIENT_LOGGED_IN;
import static com.io7m.eigion.client.api.EIClientLoggedOut.CLIENT_LOGGED_OUT;
import static com.io7m.eigion.client.api.EIClientLoginInProcess.CLIENT_LOGIN_IN_PROCESS;
import static com.io7m.eigion.client.api.EIClientLoginNotRequired.CLIENT_LOGIN_NOT_REQUIRED;
import static com.io7m.eigion.client.api.EIClientLoginWentOffline.CLIENT_LOGIN_WENT_OFFLINE;
import static com.io7m.eigion.client.api.EIClientOnline.CLIENT_ONLINE;
import static java.net.http.HttpRequest.BodyPublishers.ofByteArray;

/**
 * The default client implementation.
 */

public final class EIV1Client implements EIClientType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIV1Client.class);

  private final EIClientStrings strings;
  private final EIClientConfiguration configuration;
  private final HttpClient client;
  private final JsonMapper mapper;
  private final SimpleDeserializers serializers;
  private final AttributeType<EIClientOnline> online;
  private final AttributeType<EIClientLoginStatusType> loginStatus;
  private final Semaphore loginSemaphore;

  /**
   * The default client implementation.
   *
   * @param inStrings       The client strings
   * @param inConfiguration The client configuration
   * @param inClient        The HTTP client
   */

  public EIV1Client(
    final EIClientStrings inStrings,
    final EIClientConfiguration inConfiguration,
    final HttpClient inClient)
  {
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.client =
      Objects.requireNonNull(inClient, "inClient");

    final var attributes =
      Attributes.create(e -> LOG.error("listener raised exception: ", e));

    this.online =
      attributes.create(CLIENT_ONLINE);
    this.loginStatus =
      attributes.create(
        inConfiguration.isLoginRequired()
          ? CLIENT_LOGGED_OUT
          : CLIENT_LOGIN_NOT_REQUIRED
      );

    this.serializers =
      DmJsonRestrictedDeserializers.builder()
        .allowClass(String.class)
        .allowClass(boolean.class)
        .allowClass(EIV1LoginResponse.class)
        .allowClass(EIV1News.class)
        .allowClass(EIV1NewsItem.class)
        .allowClassName(
          "java.util.List<com.io7m.eigion.client.vanilla.v1.EIV1ClientMessagesType$EIV1NewsItem>")
        .build();

    this.mapper =
      JsonMapper.builder()
        .build();

    final var simpleModule = new SimpleModule();
    simpleModule.setDeserializers(this.serializers);
    this.mapper.registerModule(simpleModule);

    this.loginSemaphore =
      new Semaphore(1);
  }

  private static List<EIClientNewsItem> mapNews(
    final EIV1News responseMessage)
  {
    return responseMessage.items.stream().map(item -> {
      return new EIClientNewsItem(
        LocalDateTime.parse(item.date, DateTimeFormatter.ISO_OFFSET_DATE_TIME),
        item.format,
        item.title,
        item.text
      );
    }).toList();
  }

  @Override
  public AttributeReadableType<EIClientLoginStatusType> loginStatus()
  {
    return this.loginStatus;
  }

  @Override
  public AttributeReadableType<EIClientOnline> onlineStatus()
  {
    return this.online;
  }

  @Override
  public void onlineSet(
    final EIClientOnline mode)
  {
    this.online.set(Objects.requireNonNull(mode, "online"));

    this.loginStatus.set(
      switch (mode) {
        case CLIENT_ONLINE -> {
          if (this.configuration.isLoginRequired()) {
            yield CLIENT_LOGGED_OUT;
          }
          yield CLIENT_LOGIN_NOT_REQUIRED;
        }
        case CLIENT_OFFLINE -> CLIENT_LOGIN_WENT_OFFLINE;
      }
    );
  }

  @Override
  public EITask<Void> login(
    final String username,
    final String password)
  {
    Objects.requireNonNull(username, "username");
    Objects.requireNonNull(password, "password");

    try {
      final var task =
        EITask.<Void>create(LOG, this.strings.format("loginTask"));

      try {
        this.loginSemaphore.acquire();

        final var loginURI =
          this.configuration.baseURI().resolve("/v1/login");

        task.beginStep(this.strings.format("loginConnectingTo", loginURI));
        this.loginStatus.set(CLIENT_LOGIN_IN_PROCESS);

        final var message =
          new EIV1ClientMessagesType.EIV1Login("login", username, password);


        final var request =
          HttpRequest.newBuilder(loginURI)
            .POST(ofByteArray(this.mapper.writeValueAsBytes(message)))
            .build();

        final var response =
          this.client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        final var responseMessage =
          this.mapper.readValue(response.body(), EIV1LoginResponse.class);

        if (response.statusCode() >= 400) {
          task.setFailed(this.strings.format(
            "login.serverFailed",
            response.statusCode()));
          this.loginStatus.set(new EIClientLoginFailed(task));
        } else {
          task.setSucceeded();
          this.loginStatus.set(CLIENT_LOGGED_IN);
        }
      } catch (final ConnectException e) {
        final var em = this.strings.format("login.connectFailed");
        task.setFailed(em, e);
        this.loginStatus.set(new EIClientLoginFailed(task));
      } catch (IOException | InterruptedException e) {
        final var em = e.getMessage();
        task.setFailed(em, e);
        this.loginStatus.set(new EIClientLoginFailed(task));
      }

      return task;
    } finally {
      this.loginSemaphore.release();
    }
  }

  @Override
  public EITask<List<EIClientNewsItem>> news()
  {
    final var task =
      EITask.<List<EIClientNewsItem>>create(
        LOG, this.strings.format("news.fetching"));

    final var newsURI =
      this.configuration.baseURI().resolve("/v1/news");

    task.beginStep(this.strings.format("news.fetchingURI", newsURI));

    try {
      final var request =
        HttpRequest.newBuilder(newsURI)
          .GET()
          .build();

      final var response =
        this.client.send(request, HttpResponse.BodyHandlers.ofInputStream());
      final var responseMessage =
        this.mapper.readValue(response.body(), EIV1News.class);

      if (response.statusCode() >= 400) {
        task.setFailed(this.strings.format(
          "news.fetchFailed",
          response.statusCode()));
      } else {
        task.setSucceeded(this.strings.format(
          "news.received",
          responseMessage.items.size()));
        task.setResult(mapNews(responseMessage));
      }
    } catch (IOException | InterruptedException e) {
      task.setFailed(this.strings.format("news.fetchIOFailed", e.getMessage()));
    }

    return task;
  }

  @Override
  public void close()
  {

  }
}
