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

import com.io7m.eigion.amberjack.api.EIAJClientException;
import com.io7m.eigion.amberjack.api.EIAJClientPagedType;
import com.io7m.eigion.model.EIAuditEvent;
import com.io7m.eigion.model.EIAuditSearchParameters;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIPage;
import com.io7m.eigion.protocol.amberjack.EIAJCommandAuditSearchBegin;
import com.io7m.eigion.protocol.amberjack.EIAJCommandAuditSearchNext;
import com.io7m.eigion.protocol.amberjack.EIAJCommandAuditSearchPrevious;
import com.io7m.eigion.protocol.amberjack.EIAJCommandGroupCreate;
import com.io7m.eigion.protocol.amberjack.EIAJCommandLogin;
import com.io7m.eigion.protocol.amberjack.EIAJCommandType;
import com.io7m.eigion.protocol.amberjack.EIAJResponseAuditSearch;
import com.io7m.eigion.protocol.amberjack.EIAJResponseError;
import com.io7m.eigion.protocol.amberjack.EIAJResponseGroupCreate;
import com.io7m.eigion.protocol.amberjack.EIAJResponseLogin;
import com.io7m.eigion.protocol.amberjack.EIAJResponseType;
import com.io7m.eigion.protocol.amberjack.cb.EIAJCB1Messages;
import com.io7m.eigion.protocol.api.EIProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Objects;
import java.util.function.Function;

import static com.io7m.eigion.amberjack.internal.EIAJCompression.decompressResponse;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.IO_ERROR;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.AUTHENTICATION_ERROR;
import static java.net.http.HttpResponse.BodyHandlers;

/**
 * The version 1 protocol handler.
 */

public final class EIAJClientProtocolHandler1
  extends EIAJClientProtocolHandlerAbstract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIAJClientProtocolHandler1.class);

  private final URI commandURI;
  private final EIAJCB1Messages messages;
  private final URI loginURI;
  private EIAJCommandLogin mostRecentLogin;

  /**
   * The version 1 protocol handler.
   *
   * @param inHttpClient The HTTP client
   * @param inStrings    The string resources
   * @param inBase       The base URI
   */

  public EIAJClientProtocolHandler1(
    final HttpClient inHttpClient,
    final EIAJStrings inStrings,
    final URI inBase)
  {
    super(inHttpClient, inStrings, inBase);

    this.messages =
      new EIAJCB1Messages();

    this.loginURI =
      inBase.resolve("login")
        .normalize();
    this.commandURI =
      inBase.resolve("command")
        .normalize();
  }

  @Override
  public EIAJNewHandler login(
    final String admin,
    final String password,
    final URI base)
    throws EIAJClientException, InterruptedException
  {
    this.mostRecentLogin = new EIAJCommandLogin(admin, password);
    final var result = this.sendLogin(this.mostRecentLogin).user();
    return new EIAJNewHandler(result, this);
  }

  private EIAJResponseLogin sendLogin(
    final EIAJCommandLogin message)
    throws InterruptedException, EIAJClientException
  {
    return this.send(1, this.loginURI, EIAJResponseLogin.class, true, message);
  }

  private <T extends EIAJResponseType> T sendCommand(
    final Class<T> responseClass,
    final EIAJCommandType<T> message)
    throws InterruptedException, EIAJClientException
  {
    return this.send(1, this.commandURI, responseClass, false, message);
  }

  private <T extends EIAJResponseType> T send(
    final int attempt,
    final URI uri,
    final Class<T> responseClass,
    final boolean isLoggingIn,
    final EIAJCommandType<T> message)
    throws InterruptedException, EIAJClientException
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

      final var expectedContentType = EIAJCB1Messages.contentType();
      if (!contentType.equals(expectedContentType)) {
        throw new EIAJClientException(
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

      if (!(responseMessage instanceof final EIAJResponseType responseActual)) {
        throw new EIAJClientException(
          PROTOCOL_ERROR,
          this.strings()
            .format(
              "errorResponseType",
              "(unavailable)",
              commandType,
              EIAJResponseType.class,
              responseMessage.getClass())
        );
      }

      if (responseActual instanceof EIAJResponseError error) {
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

        throw new EIAJClientException(
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
        throw new EIAJClientException(
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
      throw new EIAJClientException(PROTOCOL_ERROR, e);
    } catch (final IOException e) {
      throw new EIAJClientException(IO_ERROR, e);
    }
  }

  private static boolean isAuthenticationError(
    final EIAJResponseError error)
  {
    return Objects.equals(error.errorCode(), AUTHENTICATION_ERROR.id());
  }

  private static String userAgent()
  {
    final String version;
    final var pack = EIAJClientProtocolHandler1.class.getPackage();
    if (pack != null) {
      version = pack.getImplementationVersion();
    } else {
      version = "0.0.0";
    }
    return "com.io7m.eigion.amberjack/%s".formatted(version);
  }

  @Override
  public void groupCreate(
    final EIGroupName name)
    throws EIAJClientException, InterruptedException
  {
    this.sendCommand(
      EIAJResponseGroupCreate.class,
      new EIAJCommandGroupCreate(name)
    );
  }

  @Override
  public EIAJClientPagedType<EIAuditEvent> auditSearch(
    final EIAuditSearchParameters parameters)
  {
    return new GenericPaged<>(
      this,
      EIAJResponseAuditSearch.class,
      new EIAJCommandAuditSearchBegin(parameters),
      new EIAJCommandAuditSearchNext(),
      new EIAJCommandAuditSearchPrevious(),
      EIAJResponseAuditSearch::page
    );
  }

  private static final class GenericPaged<
    T,
    R extends EIAJResponseType,
    CC extends EIAJCommandType<R>,
    CN extends EIAJCommandType<R>,
    CP extends EIAJCommandType<R>>
    implements EIAJClientPagedType<T>
  {
    private final Class<R> responseClass;
    private final CC cmdCurrent;
    private final CN cmdNext;
    private final CP cmdPrevious;
    private final Function<R, EIPage<T>> extractor;
    private final EIAJClientProtocolHandler1 handler;

    private GenericPaged(
      final EIAJClientProtocolHandler1 inHandler,
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
      throws EIAJClientException, InterruptedException
    {
      return this.extractor.apply(
        this.handler.sendCommand(this.responseClass, this.cmdCurrent)
      );
    }

    @Override
    public EIPage<T> next()
      throws EIAJClientException, InterruptedException
    {
      return this.extractor.apply(
        this.handler.sendCommand(this.responseClass, this.cmdNext)
      );
    }

    @Override
    public EIPage<T> previous()
      throws EIAJClientException, InterruptedException
    {
      return this.extractor.apply(
        this.handler.sendCommand(this.responseClass, this.cmdPrevious)
      );
    }
  }
}
