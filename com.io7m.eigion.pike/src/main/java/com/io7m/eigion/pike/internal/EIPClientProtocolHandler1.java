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


package com.io7m.eigion.pike.internal;

import com.io7m.eigion.model.EIGroupCreationRequest;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.pike.api.EIPClientException;
import com.io7m.eigion.pike.api.EIPGroupCreationChallenge;
import com.io7m.eigion.protocol.api.EIProtocolException;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupCreateBegin;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupCreateCancel;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupCreateReady;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupCreateRequests;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandLogin;
import com.io7m.eigion.protocol.public_api.v1.EISP1GroupCreationRequest;
import com.io7m.eigion.protocol.public_api.v1.EISP1MessageType;
import com.io7m.eigion.protocol.public_api.v1.EISP1Messages;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseError;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroupCreateBegin;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroupCreateCancel;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroupCreateReady;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroupCreateRequests;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseLogin;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.net.http.HttpResponse.BodyHandlers;

/**
 * The version 1 protocol handler.
 */

public final class EIPClientProtocolHandler1
  extends EIPClientProtocolHandlerAbstract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIPClientProtocolHandler1.class);

  private final URI commandURI;
  private final URI transactionURI;
  private final EISP1Messages messages;
  private final URI loginURI;

  /**
   * The version 1 protocol handler.
   *
   * @param inHttpClient The HTTP client
   * @param inStrings    The string resources
   * @param inBase       The base URI
   */

  public EIPClientProtocolHandler1(
    final HttpClient inHttpClient,
    final EIPStrings inStrings,
    final URI inBase)
  {
    super(inHttpClient, inStrings, inBase);

    this.messages =
      new EISP1Messages();

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

  private static <A, B, E extends Exception> Optional<B> mapPartial(
    final Optional<A> o,
    final FunctionType<A, B, E> f)
    throws E
  {
    if (o.isPresent()) {
      return Optional.of(f.apply(o.get()));
    }
    return Optional.empty();
  }

  @Override
  public EIPClientProtocolHandlerType login(
    final String user,
    final String password,
    final URI base)
    throws EIPClientException, InterruptedException
  {
    this.sendLogin(
      EISP1ResponseLogin.class,
      new EISP1CommandLogin(user, password)
    );
    return this;
  }

  private <T extends EISP1ResponseLogin> T sendLogin(
    final Class<T> responseClass,
    final EISP1CommandLogin message)
    throws InterruptedException, EIPClientException
  {
    return this.send(this.loginURI, responseClass, message, false)
      .orElseThrow(() -> new IllegalStateException("send() returned empty"));
  }

  private <T extends EISP1ResponseType> T sendCommand(
    final Class<T> responseClass,
    final EISP1MessageType message)
    throws InterruptedException, EIPClientException
  {
    return this.send(this.commandURI, responseClass, message, false)
      .orElseThrow(() -> new IllegalStateException("send() returned empty"));
  }

  private <T extends EISP1ResponseType> Optional<T> sendCommandOptional(
    final Class<T> responseClass,
    final EISP1MessageType message)
    throws InterruptedException, EIPClientException
  {
    return this.send(this.commandURI, responseClass, message, true);
  }

  private <T extends EISP1ResponseType> Optional<T> send(
    final URI uri,
    final Class<T> responseClass,
    final EISP1MessageType message,
    final boolean allowNotFound)
    throws InterruptedException, EIPClientException
  {
    try {
      final var commandType = message.getClass().getSimpleName();
      LOG.debug("sending {} to {}", commandType, uri);

      final var sendBytes =
        this.messages.serialize(message);

      final var request =
        HttpRequest.newBuilder(uri)
          .POST(HttpRequest.BodyPublishers.ofByteArray(sendBytes))
          .build();

      final var response =
        this.httpClient()
          .send(request, BodyHandlers.ofByteArray());

      LOG.debug("server: status {}", response.statusCode());

      if (response.statusCode() == 404 && allowNotFound) {
        return Optional.empty();
      }

      final var responseHeaders =
        response.headers();

      final var contentType =
        responseHeaders.firstValue("content-type")
          .orElse("application/octet-stream");

      if (!contentType.equals(EISP1Messages.contentType())) {
        throw new EIPClientException(
          this.strings()
            .format(
              "errorContentType",
              commandType,
              EISP1Messages.contentType(),
              contentType)
        );
      }

      final var responseMessage =
        this.messages.parse(response.body());

      if (!(responseMessage instanceof EISP1ResponseType)) {
        throw new EIPClientException(
          this.strings()
            .format(
              "errorResponseType",
              "(unavailable)",
              commandType,
              EISP1ResponseType.class,
              responseMessage.getClass())
        );
      }

      final var responseActual = (EISP1ResponseType) responseMessage;
      if (responseActual instanceof EISP1ResponseError error) {
        throw new EIPClientException(
          this.strings()
            .format(
              "errorResponse",
              error.requestId(),
              commandType,
              Integer.valueOf(response.statusCode()),
              error.errorCode(),
              error.message())
        );
      }

      if (!Objects.equals(responseActual.getClass(), responseClass)) {
        throw new EIPClientException(
          this.strings()
            .format(
              "errorResponseType",
              responseActual.requestId(),
              commandType,
              responseClass,
              responseMessage.getClass())
        );
      }

      return Optional.of(responseClass.cast(responseMessage));
    } catch (final EIProtocolException | IOException e) {
      throw new EIPClientException(e);
    }
  }

  @Override
  public EIPGroupCreationChallenge groupCreationBegin(
    final EIGroupName name)
    throws EIPClientException, InterruptedException
  {
    final var response =
      this.sendCommand(
        EISP1ResponseGroupCreateBegin.class,
        new EISP1CommandGroupCreateBegin(name.value())
      );

    return new EIPGroupCreationChallenge(
      new EIToken(response.token()),
      response.location()
    );
  }

  @Override
  public List<EIGroupCreationRequest> groupCreationRequests()
    throws EIPClientException, InterruptedException
  {
    final var response =
      this.sendCommand(
        EISP1ResponseGroupCreateRequests.class,
        new EISP1CommandGroupCreateRequests()
      );

    return response.requests()
      .stream()
      .map(EISP1GroupCreationRequest::toRequest)
      .toList();
  }

  @Override
  public void groupCreationCancel(
    final EIToken token)
    throws EIPClientException, InterruptedException
  {
    this.sendCommand(
      EISP1ResponseGroupCreateCancel.class,
      new EISP1CommandGroupCreateCancel(token.value())
    );
  }

  @Override
  public void groupCreationReady(
    final EIToken token)
    throws EIPClientException, InterruptedException
  {
    this.sendCommand(
      EISP1ResponseGroupCreateReady.class,
      new EISP1CommandGroupCreateReady(token.value())
    );
  }

  interface FunctionType<A, B, E extends Exception>
  {
    B apply(A x)
      throws E;
  }

  private static final class NotFoundException extends Exception
  {
    NotFoundException()
    {

    }
  }
}
