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

import com.io7m.eigion.model.EIGroupCreationChallenge;
import com.io7m.eigion.model.EIGroupCreationRequest;
import com.io7m.eigion.model.EIGroupMembership;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIPage;
import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.pike.api.EIPClientException;
import com.io7m.eigion.pike.api.EIPClientPagedType;
import com.io7m.eigion.protocol.api.EIProtocolException;
import com.io7m.eigion.protocol.pike.EIPCommandGroupCreateBegin;
import com.io7m.eigion.protocol.pike.EIPCommandGroupCreateCancel;
import com.io7m.eigion.protocol.pike.EIPCommandGroupCreateReady;
import com.io7m.eigion.protocol.pike.EIPCommandGroupCreateRequestsBegin;
import com.io7m.eigion.protocol.pike.EIPCommandGroupCreateRequestsNext;
import com.io7m.eigion.protocol.pike.EIPCommandGroupCreateRequestsPrevious;
import com.io7m.eigion.protocol.pike.EIPCommandGroupsBegin;
import com.io7m.eigion.protocol.pike.EIPCommandGroupsNext;
import com.io7m.eigion.protocol.pike.EIPCommandGroupsPrevious;
import com.io7m.eigion.protocol.pike.EIPCommandLogin;
import com.io7m.eigion.protocol.pike.EIPCommandType;
import com.io7m.eigion.protocol.pike.EIPResponseError;
import com.io7m.eigion.protocol.pike.EIPResponseGroupCreateBegin;
import com.io7m.eigion.protocol.pike.EIPResponseGroupCreateCancel;
import com.io7m.eigion.protocol.pike.EIPResponseGroupCreateReady;
import com.io7m.eigion.protocol.pike.EIPResponseGroupCreateRequests;
import com.io7m.eigion.protocol.pike.EIPResponseGroups;
import com.io7m.eigion.protocol.pike.EIPResponseLogin;
import com.io7m.eigion.protocol.pike.EIPResponseType;
import com.io7m.eigion.protocol.pike.cb.EIPCB1Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Objects;
import java.util.function.Function;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.IO_ERROR;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.eigion.pike.internal.EIPCompression.decompressResponse;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.AUTHENTICATION_ERROR;
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
  private final EIPCB1Messages messages;
  private final URI loginURI;
  private EIPCommandLogin mostRecentLogin;

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
      new EIPCB1Messages();

    this.loginURI =
      inBase.resolve("login")
        .normalize();
    this.commandURI =
      inBase.resolve("command")
        .normalize();
  }

  @Override
  public EIPNewHandler login(
    final String admin,
    final String password,
    final URI base)
    throws EIPClientException, InterruptedException
  {
    this.mostRecentLogin = new EIPCommandLogin(admin, password);
    final var result = this.sendLogin(this.mostRecentLogin).user();
    return new EIPNewHandler(result, this);
  }

  private EIPResponseLogin sendLogin(
    final EIPCommandLogin message)
    throws InterruptedException, EIPClientException
  {
    return this.send(1, this.loginURI, EIPResponseLogin.class, true, message);
  }

  private <T extends EIPResponseType> T sendCommand(
    final Class<T> responseClass,
    final EIPCommandType<T> message)
    throws InterruptedException, EIPClientException
  {
    return this.send(1, this.commandURI, responseClass, false, message);
  }

  private <T extends EIPResponseType> T send(
    final int attempt,
    final URI uri,
    final Class<T> responseClass,
    final boolean isLoggingIn,
    final EIPCommandType<T> message)
    throws InterruptedException, EIPClientException
  {
    try {
      final var commandType = message.getClass().getSimpleName();
      LOG.debug("sending {} to {}", commandType, uri);

      final var sendBytes =
        this.messages.serialize(message);

      final var request =
        HttpRequest.newBuilder(uri)
          .header("User-Agent", userAgent())
          .POST(HttpRequest.BodyPublishers.ofByteArray(sendBytes))
          .build();

      final var response =
        this.httpClient()
          .send(request, BodyHandlers.ofByteArray());

      LOG.debug("server: status {}", response.statusCode());

      final var responseHeaders =
        response.headers();

      final var contentType =
        responseHeaders.firstValue("content-type")
          .orElse("application/octet-stream");

      final var expectedContentType = EIPCB1Messages.contentType();
      if (!contentType.equals(expectedContentType)) {
        throw new EIPClientException(
          PROTOCOL_ERROR,
          this.strings()
            .format(
              "errorContentType",
              commandType,
              expectedContentType,
              contentType)
        );
      }

      final var responseMessage =
        this.messages.parse(decompressResponse(response, responseHeaders));

      if (!(responseMessage instanceof final EIPResponseType responseActual)) {
        throw new EIPClientException(
          PROTOCOL_ERROR,
          this.strings()
            .format(
              "errorResponseType",
              "(unavailable)",
              commandType,
              EIPResponseType.class,
              responseMessage.getClass())
        );
      }

      if (responseActual instanceof EIPResponseError error) {
        if (attempt < 3) {
          if (isAuthenticationError(error) && !isLoggingIn) {
            LOG.debug("attempting re-login");
            this.sendLogin(this.mostRecentLogin);
            return this.send(
              attempt + 1,
              uri,
              responseClass,
              false,
              message
            );
          }
        }

        throw new EIPClientException(
          error.errorCode(),
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
          PROTOCOL_ERROR,
          this.strings()
            .format(
              "errorResponseType",
              responseActual.requestId(),
              commandType,
              responseClass,
              responseMessage.getClass())
        );
      }

      return responseClass.cast(responseMessage);
    } catch (final EIProtocolException e) {
      throw new EIPClientException(PROTOCOL_ERROR, e);
    } catch (final IOException e) {
      throw new EIPClientException(IO_ERROR, e);
    }
  }

  private static boolean isAuthenticationError(
    final EIPResponseError error)
  {
    return Objects.equals(error.errorCode(), AUTHENTICATION_ERROR.id());
  }

  private static String userAgent()
  {
    final String version;
    final var pack = EIPClientProtocolHandler1.class.getPackage();
    if (pack != null) {
      version = pack.getImplementationVersion();
    } else {
      version = "0.0.0";
    }
    return "com.io7m.eigion.pike/%s".formatted(version);
  }

  @Override
  public EIGroupCreationChallenge groupCreateBegin(
    final EIGroupName groupName)
    throws EIPClientException, InterruptedException
  {
    final var response =
      this.sendCommand(
        EIPResponseGroupCreateBegin.class,
        new EIPCommandGroupCreateBegin(groupName)
      );

    return new EIGroupCreationChallenge(
      response.groupName(),
      response.token(),
      response.location()
    );
  }

  @Override
  public void groupCreateReady(
    final EIToken token)
    throws EIPClientException, InterruptedException
  {
    this.sendCommand(
      EIPResponseGroupCreateReady.class,
      new EIPCommandGroupCreateReady(token)
    );
  }

  @Override
  public void groupCreateCancel(
    final EIToken token)
    throws EIPClientException, InterruptedException
  {
    this.sendCommand(
      EIPResponseGroupCreateCancel.class,
      new EIPCommandGroupCreateCancel(token)
    );
  }

  @Override
  public EIPClientPagedType<EIGroupMembership> groups()
  {
    return new GenericPaged<>(
      this,
      EIPResponseGroups.class,
      new EIPCommandGroupsBegin(1000L),
      new EIPCommandGroupsNext(),
      new EIPCommandGroupsPrevious(),
      EIPResponseGroups::groups
    );
  }

  @Override
  public EIPClientPagedType<EIGroupCreationRequest> groupCreateRequests()
  {
    return new GenericPaged<>(
      this,
      EIPResponseGroupCreateRequests.class,
      new EIPCommandGroupCreateRequestsBegin(1000L),
      new EIPCommandGroupCreateRequestsNext(),
      new EIPCommandGroupCreateRequestsPrevious(),
      EIPResponseGroupCreateRequests::requests
    );
  }

  private static final class GenericPaged<
    T,
    R extends EIPResponseType,
    CC extends EIPCommandType<R>,
    CN extends EIPCommandType<R>,
    CP extends EIPCommandType<R>>
    implements EIPClientPagedType<T>
  {
    private final Class<R> responseClass;
    private final CC cmdCurrent;
    private final CN cmdNext;
    private final CP cmdPrevious;
    private final Function<R, EIPage<T>> extractor;
    private final EIPClientProtocolHandler1 handler;

    private GenericPaged(
      final EIPClientProtocolHandler1 inHandler,
      final Class<R> inResponseClass,
      final CC inCmdCurrent,
      final CN inCmdNext,
      final CP inCmdPrevious,
      final Function<R, EIPage<T>> inExtractor)
    {
      this.handler =
        Objects.requireNonNull(inHandler, "handler");
      this.responseClass =
        Objects.requireNonNull(inResponseClass, "responseClass");
      this.cmdCurrent =
        Objects.requireNonNull(inCmdCurrent, "cmdCurrent");
      this.cmdNext =
        Objects.requireNonNull(inCmdNext, "cmdNext");
      this.cmdPrevious =
        Objects.requireNonNull(inCmdPrevious, "cmdPrevious");
      this.extractor =
        Objects.requireNonNull(inExtractor, "extractor");
    }

    @Override
    public EIPage<T> current()
      throws EIPClientException, InterruptedException
    {
      return this.extractor.apply(
        this.handler.sendCommand(this.responseClass, this.cmdCurrent)
      );
    }

    @Override
    public EIPage<T> next()
      throws EIPClientException, InterruptedException
    {
      return this.extractor.apply(
        this.handler.sendCommand(this.responseClass, this.cmdNext)
      );
    }

    @Override
    public EIPage<T> previous()
      throws EIPClientException, InterruptedException
    {
      return this.extractor.apply(
        this.handler.sendCommand(this.responseClass, this.cmdPrevious)
      );
    }
  }
}
