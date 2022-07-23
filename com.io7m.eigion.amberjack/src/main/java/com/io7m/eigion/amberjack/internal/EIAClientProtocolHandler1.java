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


package com.io7m.eigion.amberjack.internal;

import com.io7m.eigion.amberjack.api.EIAClientException;
import com.io7m.eigion.model.EIAuditEvent;
import com.io7m.eigion.model.EIPasswordException;
import com.io7m.eigion.model.EIService;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.model.EIUserSummary;
import com.io7m.eigion.protocol.admin_api.v1.EISA1AuditEvent;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAuditGetByTime;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandLogin;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandServicesList;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserGet;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserGetByEmail;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserGetByName;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserSearch;
import com.io7m.eigion.protocol.admin_api.v1.EISA1MessageType;
import com.io7m.eigion.protocol.admin_api.v1.EISA1Messages;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseAuditGet;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseError;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseLogin;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseServiceList;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseType;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseUserGet;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseUserList;
import com.io7m.eigion.protocol.admin_api.v1.EISA1UserSummary;
import com.io7m.eigion.protocol.api.EIProtocolException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static java.net.http.HttpResponse.BodyHandlers;

/**
 * The version 1 protocol handler.
 */

public final class EIAClientProtocolHandler1
  extends EIAClientProtocolHandlerAbstract
{
  private final URI commandURI;
  private final URI transactionURI;
  private final EISA1Messages messages;
  private final URI loginURI;

  /**
   * The version 1 protocol handler.
   *
   * @param inHttpClient The HTTP client
   * @param inStrings    The string resources
   * @param inBase       The base URI
   */

  public EIAClientProtocolHandler1(
    final HttpClient inHttpClient,
    final EIAStrings inStrings,
    final URI inBase)
  {
    super(inHttpClient, inStrings, inBase);

    this.messages =
      new EISA1Messages();

    this.loginURI =
      inBase.resolve("login")
        .normalize();
    this.commandURI =
      inBase.resolve("command")
        .normalize();
    this.transactionURI =
      inBase.resolve("transaction")
        .normalize();
  }

  @Override
  public EIAClientProtocolHandlerType login(
    final String user,
    final String password,
    final URI base)
    throws EIAClientException, InterruptedException
  {
    this.sendLogin(
      EISA1ResponseLogin.class,
      new EISA1CommandLogin(user, password)
    );
    return this;
  }

  @Override
  public List<EIService> services()
    throws EIAClientException, InterruptedException
  {
    final var message =
      this.sendCommand(
        EISA1ResponseServiceList.class,
        new EISA1CommandServicesList());

    return message.services()
      .stream()
      .map(s -> new EIService(s.serviceName(), s.serviceDescription()))
      .toList();
  }

  @Override
  public Optional<EIUser> userById(
    final String id)
    throws EIAClientException, InterruptedException
  {
    try {
      final var message =
        this.sendCommand(
          EISA1ResponseUserGet.class,
          new EISA1CommandUserGet(UUID.fromString(id)));

      return Optional.of(message.user().toUser());
    } catch (final EIPasswordException e) {
      throw new EIAClientException(e);
    } catch (final IllegalArgumentException e) {
      throw new EIAClientException(
        this.strings().format("errorMalformedParameter", e.getMessage())
      );
    }
  }

  @Override
  public Optional<EIUser> userByName(
    final String name)
    throws EIAClientException, InterruptedException
  {
    try {
      final var message =
        this.sendCommand(
          EISA1ResponseUserGet.class,
          new EISA1CommandUserGetByName(name));

      return Optional.of(message.user().toUser());
    } catch (final EIPasswordException e) {
      throw new EIAClientException(e);
    }
  }

  @Override
  public Optional<EIUser> userByEmail(final String email)
    throws EIAClientException, InterruptedException
  {
    try {
      final var message =
        this.sendCommand(
          EISA1ResponseUserGet.class,
          new EISA1CommandUserGetByEmail(email));

      return Optional.of(message.user().toUser());
    } catch (final EIPasswordException e) {
      throw new EIAClientException(e);
    }
  }

  @Override
  public List<EIUserSummary> userSearch(
    final String query)
    throws EIAClientException, InterruptedException
  {
    final var message =
      this.sendCommand(
        EISA1ResponseUserList.class,
        new EISA1CommandUserSearch(query));

    return message.users()
      .stream()
      .map(EISA1UserSummary::toUserSummary)
      .toList();
  }

  @Override
  public List<EIAuditEvent> auditGetByTime(
    final OffsetDateTime dateLower,
    final OffsetDateTime dateUpper)
    throws EIAClientException, InterruptedException
  {
    final var message =
      this.sendCommand(
        EISA1ResponseAuditGet.class,
        new EISA1CommandAuditGetByTime(dateLower, dateUpper)
      );

    return message.events()
      .stream()
      .map(EISA1AuditEvent::toAuditEvent)
      .toList();
  }

  private <T extends EISA1ResponseLogin> T sendLogin(
    final Class<T> responseClass,
    final EISA1CommandLogin message)
    throws InterruptedException, EIAClientException
  {
    return this.send(this.loginURI, responseClass, message);
  }

  private <T extends EISA1ResponseType> T sendCommand(
    final Class<T> responseClass,
    final EISA1MessageType message)
    throws InterruptedException, EIAClientException
  {
    return this.send(this.commandURI, responseClass, message);
  }

  private <T extends EISA1ResponseType> T send(
    final URI uri,
    final Class<T> responseClass,
    final EISA1MessageType message)
    throws InterruptedException, EIAClientException
  {
    try {
      final var sendBytes =
        this.messages.serialize(message);

      final var request =
        HttpRequest.newBuilder(uri)
          .POST(HttpRequest.BodyPublishers.ofByteArray(sendBytes))
          .build();

      final var response =
        this.httpClient()
          .send(request, BodyHandlers.ofByteArray());

      final var responseHeaders =
        response.headers();

      final var contentType =
        responseHeaders.firstValue("content-type")
          .orElse("application/octet-stream");

      if (!contentType.equals(EISA1Messages.contentType())) {
        throw new EIAClientException(
          this.strings()
            .format(
              "errorContentType",
              EISA1Messages.contentType(),
              contentType)
        );
      }

      final var responseMessage =
        this.messages.parse(response.body());

      if (!(responseMessage instanceof EISA1ResponseType)) {
        throw new EIAClientException(
          this.strings()
            .format(
              "errorResponseType",
              "(unavailable)",
              EISA1ResponseType.class,
              responseMessage.getClass())
        );
      }

      final var responseActual = (EISA1ResponseType) responseMessage;
      if (responseActual instanceof EISA1ResponseError error) {
        throw new EIAClientException(
          this.strings()
            .format(
              "errorResponse",
              error.requestId(),
              Integer.valueOf(response.statusCode()),
              error.errorCode(),
              error.message())
        );
      }

      if (!Objects.equals(responseActual.getClass(), responseClass)) {
        throw new EIAClientException(
          this.strings()
            .format(
              "errorResponseType",
              responseActual.requestId(),
              responseClass,
              responseMessage.getClass())
        );
      }

      return responseClass.cast(responseMessage);
    } catch (final EIProtocolException | IOException e) {
      throw new EIAClientException(e);
    }
  }
}
