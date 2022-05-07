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
import com.io7m.eigion.client.api.EIClientNewsItem;
import com.io7m.eigion.client.api.EIClientStatusType;
import com.io7m.eigion.client.api.EIClientStatusType.EIClientStatusLoginFailed;
import com.io7m.eigion.client.api.EIClientType;
import com.io7m.eigion.client.vanilla.internal.EIClientStrings;
import com.io7m.eigion.client.vanilla.v1.EIV1ClientMessagesType.EIV1LoginResponse;
import com.io7m.eigion.client.vanilla.v1.EIV1ClientMessagesType.EIV1News;
import com.io7m.eigion.client.vanilla.v1.EIV1ClientMessagesType.EIV1NewsItem;
import com.io7m.eigion.taskrecorder.EITask;
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
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

import static com.io7m.eigion.client.api.EIClientStatusType.EIClientStatusInitial.CLIENT_STATUS_INITIAL;
import static com.io7m.eigion.client.api.EIClientStatusType.EIClientStatusLoggedIn.CLIENT_STATUS_LOGGED_IN;
import static com.io7m.eigion.client.api.EIClientStatusType.EIClientStatusLoggingIn.CLIENT_STATUS_LOGGING_IN;
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
  private final SubmissionPublisher<EIClientStatusType> statusEvents;
  private volatile EIClientStatusType statusNow;

  /**
   * The default client implementation.
   *
   * @param inStrings The client strings
   * @param inConfiguration The client configuration
   * @param inClient The HTTP client
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
    this.statusNow =
      CLIENT_STATUS_INITIAL;
    this.statusEvents =
      new SubmissionPublisher<>();

    this.serializers =
      DmJsonRestrictedDeserializers.builder()
        .allowClass(String.class)
        .allowClass(boolean.class)
        .allowClass(EIV1LoginResponse.class)
        .allowClass(EIV1News.class)
        .allowClass(EIV1NewsItem.class)
        .allowClassName("java.util.List<com.io7m.eigion.client.vanilla.v1.EIV1ClientMessagesType$EIV1NewsItem>")
        .build();

    this.mapper =
      JsonMapper.builder()
        .build();

    final var simpleModule = new SimpleModule();
    simpleModule.setDeserializers(this.serializers);
    this.mapper.registerModule(simpleModule);

    this.setStatus(CLIENT_STATUS_INITIAL);
  }

  private void setStatus(
    final EIClientStatusType status)
  {
    this.statusNow = status;
    this.statusEvents.submit(status);
  }

  @Override
  public Flow.Publisher<EIClientStatusType> status()
  {
    return this.statusEvents;
  }

  @Override
  public EIClientStatusType statusNow()
  {
    return this.statusNow;
  }

  @Override
  public EITask<Void> login(
    final String username,
    final String password)
  {
    Objects.requireNonNull(username, "username");
    Objects.requireNonNull(password, "password");

    final var task =
      EITask.<Void>create(LOG, this.strings.format("loginTask"));

    final var loginURI =
      this.configuration.baseURI().resolve("/v1/login");

    task.beginStep(this.strings.format("loginConnectingTo", loginURI));
    this.setStatus(CLIENT_STATUS_LOGGING_IN);

    final var message =
      new EIV1ClientMessagesType.EIV1Login("login", username, password);


    try {
      final var request =
        HttpRequest.newBuilder(loginURI)
          .POST(ofByteArray(this.mapper.writeValueAsBytes(message)))
          .build();

      final var response =
        this.client.send(request, HttpResponse.BodyHandlers.ofInputStream());
      final var responseMessage =
        this.mapper.readValue(response.body(), EIV1LoginResponse.class);

      if (response.statusCode() >= 400) {
        task.setFailed(this.strings.format("login.serverFailed", response.statusCode()));
        this.setStatus(new EIClientStatusLoginFailed(task));
      } else {
        task.setSucceeded();
        this.setStatus(CLIENT_STATUS_LOGGED_IN);
      }
    } catch (final ConnectException e) {
      final var em = this.strings.format("login.connectFailed");
      task.setFailed(em, e);
      this.setStatus(new EIClientStatusLoginFailed(task));
    } catch (IOException | InterruptedException e) {
      final var em = e.getMessage();
      task.setFailed(em, e);
      this.setStatus(new EIClientStatusLoginFailed(task));
    }

    return task;
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
        task.setFailed(this.strings.format("news.fetchFailed", response.statusCode()));
      } else {
        task.setSucceeded(this.strings.format("news.received", responseMessage.items.size()));
        task.setResult(mapNews(responseMessage));
      }
    } catch (IOException | InterruptedException e) {
      task.setFailed(this.strings.format("news.fetchIOFailed", e.getMessage()));
    }

    return task;
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
  public void close()
  {

  }
}
