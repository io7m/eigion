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
import com.io7m.eigion.model.EIAdmin;
import com.io7m.eigion.model.EIAdminPermission;
import com.io7m.eigion.model.EIAdminSummary;
import com.io7m.eigion.model.EIAuditEvent;
import com.io7m.eigion.model.EIGroupInvite;
import com.io7m.eigion.model.EIGroupInviteStatus;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.eigion.model.EIPasswordException;
import com.io7m.eigion.model.EIService;
import com.io7m.eigion.model.EISubsetMatch;
import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.model.EIUserSummary;
import com.io7m.eigion.protocol.admin_api.v1.EISA1AdminPermission;
import com.io7m.eigion.protocol.admin_api.v1.EISA1AdminSummary;
import com.io7m.eigion.protocol.admin_api.v1.EISA1AuditEvent;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAdminCreate;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAdminGet;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAdminGetByEmail;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAdminGetByName;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAdminSearch;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAuditGet;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandGroupInviteSetStatus;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandGroupInvites;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandLogin;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandServicesList;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserCreate;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserGet;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserGetByEmail;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserGetByName;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserSearch;
import com.io7m.eigion.protocol.admin_api.v1.EISA1GroupInvite;
import com.io7m.eigion.protocol.admin_api.v1.EISA1GroupInviteStatus;
import com.io7m.eigion.protocol.admin_api.v1.EISA1MessageType;
import com.io7m.eigion.protocol.admin_api.v1.EISA1Messages;
import com.io7m.eigion.protocol.admin_api.v1.EISA1Password;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseAdminCreate;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseAdminGet;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseAdminList;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseAuditGet;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseError;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseGroupInviteSetStatus;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseGroupInvites;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseLogin;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseServiceList;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseType;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseUserCreate;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseUserGet;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseUserList;
import com.io7m.eigion.protocol.admin_api.v1.EISA1SubsetMatch;
import com.io7m.eigion.protocol.admin_api.v1.EISA1UserSummary;
import com.io7m.eigion.protocol.api.EIProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.net.http.HttpResponse.BodyHandlers;

/**
 * The version 1 protocol handler.
 */

public final class EIAClientProtocolHandler1
  extends EIAClientProtocolHandlerAbstract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIAClientProtocolHandler1.class);

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
      return mapPartial(
        this.sendCommandOptional(
          EISA1ResponseUserGet.class,
          new EISA1CommandUserGet(UUID.fromString(id))),
        message -> message.user().toUser()
      );
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
      return mapPartial(
        this.sendCommandOptional(
          EISA1ResponseUserGet.class,
          new EISA1CommandUserGetByName(name)),
        message -> message.user().toUser()
      );
    } catch (final EIPasswordException e) {
      throw new EIAClientException(e);
    }
  }

  @Override
  public Optional<EIUser> userByEmail(
    final String email)
    throws EIAClientException, InterruptedException
  {
    try {
      return mapPartial(
        this.sendCommandOptional(
          EISA1ResponseUserGet.class,
          new EISA1CommandUserGetByEmail(email)),
        message -> message.user().toUser()
      );
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
  public List<EIAuditEvent> auditGet(
    final OffsetDateTime dateLower,
    final OffsetDateTime dateUpper,
    final EISubsetMatch<String> owner,
    final EISubsetMatch<String> type,
    final EISubsetMatch<String> message)
    throws EIAClientException, InterruptedException
  {
    final var response =
      this.sendCommand(
        EISA1ResponseAuditGet.class,
        new EISA1CommandAuditGet(
          dateLower,
          dateUpper,
          EISA1SubsetMatch.ofSubsetMatch(owner),
          EISA1SubsetMatch.ofSubsetMatch(type),
          EISA1SubsetMatch.ofSubsetMatch(message)));

    return response.events()
      .stream()
      .map(EISA1AuditEvent::toAuditEvent)
      .toList();
  }

  @Override
  public EIUser userCreate(
    final String name,
    final String email,
    final String password)
    throws EIAClientException, InterruptedException
  {
    try {
      final var hashedPassword =
        EIPasswordAlgorithmPBKDF2HmacSHA256.create()
          .createHashed(password);

      final var v1Password =
        new EISA1Password(
          hashedPassword.algorithm().identifier(),
          hashedPassword.hash(),
          hashedPassword.salt()
        );

      final var message =
        this.sendCommand(
          EISA1ResponseUserCreate.class,
          new EISA1CommandUserCreate(name, email, v1Password));

      return message.user().toUser();
    } catch (final EIPasswordException e) {
      throw new EIAClientException(e);
    }
  }

  private <T extends EISA1ResponseLogin> T sendLogin(
    final Class<T> responseClass,
    final EISA1CommandLogin message)
    throws InterruptedException, EIAClientException
  {
    return this.send(this.loginURI, responseClass, message, false)
      .orElseThrow(() -> new IllegalStateException("send() returned empty"));
  }

  private <T extends EISA1ResponseType> T sendCommand(
    final Class<T> responseClass,
    final EISA1MessageType message)
    throws InterruptedException, EIAClientException
  {
    return this.send(this.commandURI, responseClass, message, false)
      .orElseThrow(() -> new IllegalStateException("send() returned empty"));
  }

  private <T extends EISA1ResponseType> Optional<T> sendCommandOptional(
    final Class<T> responseClass,
    final EISA1MessageType message)
    throws InterruptedException, EIAClientException
  {
    return this.send(this.commandURI, responseClass, message, true);
  }

  private <T extends EISA1ResponseType> Optional<T> send(
    final URI uri,
    final Class<T> responseClass,
    final EISA1MessageType message,
    final boolean allowNotFound)
    throws InterruptedException, EIAClientException
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

      if (!contentType.equals(EISA1Messages.contentType())) {
        throw new EIAClientException(
          this.strings()
            .format(
              "errorContentType",
              commandType,
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
              commandType,
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
              commandType,
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
              commandType,
              responseClass,
              responseMessage.getClass())
        );
      }

      return Optional.of(responseClass.cast(responseMessage));
    } catch (final EIProtocolException | IOException e) {
      throw new EIAClientException(e);
    }
  }

  @Override
  public Optional<EIAdmin> adminById(
    final String id)
    throws EIAClientException, InterruptedException
  {
    try {
      return mapPartial(
        this.sendCommandOptional(
          EISA1ResponseAdminGet.class,
          new EISA1CommandAdminGet(UUID.fromString(id))),
        message -> message.admin().toAdmin()
      );
    } catch (final EIPasswordException e) {
      throw new EIAClientException(e);
    } catch (final IllegalArgumentException e) {
      throw new EIAClientException(
        this.strings().format("errorMalformedParameter", e.getMessage())
      );
    }
  }

  @Override
  public Optional<EIAdmin> adminByName(
    final String name)
    throws EIAClientException, InterruptedException
  {
    try {
      return mapPartial(
        this.sendCommandOptional(
          EISA1ResponseAdminGet.class,
          new EISA1CommandAdminGetByName(name)),
        message -> message.admin().toAdmin()
      );
    } catch (final EIPasswordException e) {
      throw new EIAClientException(e);
    }
  }

  @Override
  public Optional<EIAdmin> adminByEmail(
    final String email)
    throws EIAClientException, InterruptedException
  {
    try {
      return mapPartial(
        this.sendCommandOptional(
          EISA1ResponseAdminGet.class,
          new EISA1CommandAdminGetByEmail(email)),
        message -> message.admin().toAdmin());
    } catch (final EIPasswordException e) {
      throw new EIAClientException(e);
    }
  }

  @Override
  public List<EIAdminSummary> adminSearch(
    final String query)
    throws EIAClientException, InterruptedException
  {
    final var message =
      this.sendCommand(
        EISA1ResponseAdminList.class,
        new EISA1CommandAdminSearch(query));

    return message.admins()
      .stream()
      .map(EISA1AdminSummary::toAdminSummary)
      .toList();
  }

  @Override
  public EIAdmin adminCreate(
    final String name,
    final String email,
    final String password,
    final Set<EIAdminPermission> permissions)
    throws EIAClientException, InterruptedException
  {
    try {
      final var hashedPassword =
        EIPasswordAlgorithmPBKDF2HmacSHA256.create()
          .createHashed(password);

      final var v1Password =
        new EISA1Password(
          hashedPassword.algorithm().identifier(),
          hashedPassword.hash(),
          hashedPassword.salt()
        );

      final var v1Permissions =
        permissions.stream()
          .map(EISA1AdminPermission::ofAdmin)
          .collect(Collectors.toUnmodifiableSet());

      final var message =
        this.sendCommand(
          EISA1ResponseAdminCreate.class,
          new EISA1CommandAdminCreate(name, email, v1Password, v1Permissions));

      return message.admin().toAdmin();
    } catch (final EIPasswordException e) {
      throw new EIAClientException(e);
    }
  }

  @Override
  public List<EIGroupInvite> groupInvites(
    final OffsetDateTime since,
    final Optional<EIGroupName> withGroupName,
    final Optional<UUID> withUserInviter,
    final Optional<UUID> withUserBeingInvited,
    final Optional<EIGroupInviteStatus> withStatus)
    throws EIAClientException, InterruptedException
  {
    final var response =
      this.sendCommand(
        EISA1ResponseGroupInvites.class,
        new EISA1CommandGroupInvites(
          since,
          withUserInviter,
          withUserBeingInvited,
          withGroupName.map(EIGroupName::value),
          withStatus.map(EISA1GroupInviteStatus::ofStatus)
        )
      );

    return response.invites()
      .stream()
      .map(EISA1GroupInvite::toInvite)
      .toList();
  }

  @Override
  public void groupInviteSetStatus(
    final EIToken token,
    final EIGroupInviteStatus status)
    throws EIAClientException, InterruptedException
  {
    this.sendCommand(
      EISA1ResponseGroupInviteSetStatus.class,
      new EISA1CommandGroupInviteSetStatus(
        token.value(),
        EISA1GroupInviteStatus.ofStatus(status)
      )
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
