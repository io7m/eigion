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

package com.io7m.eigion.server.internal.pike_v1;

import com.io7m.eigion.error_codes.EIErrorCode;
import com.io7m.eigion.error_codes.EIStandardErrorCodes;
import com.io7m.eigion.model.EIPermissionSet;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.model.EIUserLogin;
import com.io7m.eigion.protocol.api.EIProtocolException;
import com.io7m.eigion.protocol.pike.EIPCommandLogin;
import com.io7m.eigion.protocol.pike.EIPResponseLogin;
import com.io7m.eigion.protocol.pike.cb.EIPCB1Messages;
import com.io7m.eigion.server.database.api.EISDatabaseException;
import com.io7m.eigion.server.database.api.EISDatabaseType;
import com.io7m.eigion.server.database.api.EISDatabaseUsersQueriesType;
import com.io7m.eigion.server.internal.EIHTTPErrorStatusException;
import com.io7m.eigion.server.internal.EISClock;
import com.io7m.eigion.server.internal.EISIdstoreClients;
import com.io7m.eigion.server.internal.EISRequestDecoration;
import com.io7m.eigion.server.internal.EISRequestLimits;
import com.io7m.eigion.server.internal.EISRequests;
import com.io7m.eigion.server.internal.EISStrings;
import com.io7m.eigion.server.internal.common.EICommonInstrumentedServlet;
import com.io7m.eigion.server.internal.sessions.EISUserSessionService;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import com.io7m.idstore.model.IdLoginMetadataStandard;
import com.io7m.idstore.user_client.api.IdUClientException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.HTTP_METHOD_ERROR;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.eigion.server.database.api.EISDatabaseRole.EIGION;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.METHOD_NOT_ALLOWED_405;

/**
 * A servlet that handles user logins.
 */

public final class EISP1Login extends EICommonInstrumentedServlet
{
  private final EISDatabaseType database;
  private final EISRequestLimits limits;
  private final EIPCB1Messages messages;
  private final EISStrings strings;
  private final EISUserSessionService sessions;
  private final EISP1Sends sends;
  private final EISIdstoreClients idClients;
  private final EISClock clock;

  /**
   * A servlet that handles user logins.
   *
   * @param inServices The service directory
   */

  public EISP1Login(
    final EIServiceDirectoryType inServices)
  {
    super(Objects.requireNonNull(inServices, "services"));

    this.messages =
      inServices.requireService(EIPCB1Messages.class);
    this.idClients =
      inServices.requireService(EISIdstoreClients.class);
    this.strings =
      inServices.requireService(EISStrings.class);
    this.sends =
      inServices.requireService(EISP1Sends.class);
    this.limits =
      inServices.requireService(EISRequestLimits.class);
    this.sessions =
      inServices.requireService(EISUserSessionService.class);
    this.database =
      inServices.requireService(EISDatabaseType.class);
    this.clock =
      inServices.requireService(EISClock.class);
  }

  @Override
  protected void service(
    final HttpServletRequest request,
    final HttpServletResponse response)
    throws IOException
  {
    try {
      if (!Objects.equals(request.getMethod(), "POST")) {
        throw new EIHTTPErrorStatusException(
          METHOD_NOT_ALLOWED_405,
          HTTP_METHOD_ERROR,
          this.strings.format("methodNotAllowed")
        );
      }

      final var login =
        this.readLoginCommand(request);

      final var client =
        this.idClients.createClient();

      final var idUser =
        client.login(
          login.userName(),
          login.password(),
          this.idClients.baseURI(),
          Map.ofEntries(
            Map.entry(
              IdLoginMetadataStandard.remoteHostProxied(),
              request.getRemoteAddr()
            )
          )
        );

      final var eiUser =
        this.checkUser(
          idUser.id(),
          request.getRemoteAddr(),
          EISRequests.requestUserAgent(request)
        );

      final var httpSession = request.getSession(true);
      this.sessions.create(eiUser, httpSession, client);
      httpSession.setAttribute("UserID", idUser.id());
      this.sendLoginResponse(request, response, eiUser);

    } catch (final EIHTTPErrorStatusException e) {
      this.sends.sendError(
        response,
        EISRequestDecoration.requestIdFor(request),
        e.statusCode(),
        e.errorCode(),
        e.getMessage()
      );
    } catch (final IdUClientException e) {
      this.sends.sendError(
        response,
        EISRequestDecoration.requestIdFor(request),
        401,
        new EIErrorCode(e.errorCode().id()),
        e.reason()
      );
    } catch (final InterruptedException e) {
      this.sends.sendError(
        response,
        EISRequestDecoration.requestIdFor(request),
        500,
        EIStandardErrorCodes.IO_ERROR,
        e.getMessage()
      );
    } catch (final EISDatabaseException e) {
      this.sends.sendError(
        response,
        EISRequestDecoration.requestIdFor(request),
        500,
        e.errorCode(),
        e.getMessage()
      );
    }
  }

  private EIUser checkUser(
    final UUID userId,
    final String address,
    final String userAgent)
    throws EIHTTPErrorStatusException, EISDatabaseException
  {
    try (var connection = this.database.openConnection(EIGION)) {
      try (var transaction = connection.openTransaction()) {
        final var users =
          transaction.queries(EISDatabaseUsersQueriesType.class);
        final var user =
          users.userGet(userId)
            .orElse(new EIUser(userId, EIPermissionSet.empty()));

        users.userPut(user);
        users.userLogin(
          new EIUserLogin(
            userId,
            this.clock.now(),
            address,
            userAgent
          )
        );

        transaction.commit();
        return user;
      }
    }
  }

  private void sendLoginResponse(
    final HttpServletRequest request,
    final HttpServletResponse response,
    final EIUser user)
    throws IOException
  {
    response.setStatus(200);
    response.setContentType(EIPCB1Messages.contentType());

    try {
      final var data =
        this.messages.serialize(
          new EIPResponseLogin(EISRequestDecoration.requestIdFor(request), user)
        );
      response.setContentLength(data.length);
      try (var output = response.getOutputStream()) {
        output.write(data);
      }
    } catch (final EIProtocolException e) {
      throw new IOException(e);
    }
  }

  private EIPCommandLogin readLoginCommand(
    final HttpServletRequest request)
    throws EIHTTPErrorStatusException, IOException
  {
    try (var input = this.limits.boundedMaximumInput(request, 1024)) {
      final var data = input.readAllBytes();
      final var message = this.messages.parse(data);
      if (message instanceof EIPCommandLogin login) {
        return login;
      }
    } catch (final EIProtocolException e) {
      throw new EIHTTPErrorStatusException(
        BAD_REQUEST_400,
        PROTOCOL_ERROR,
        e.getMessage(),
        e
      );
    }

    throw new EIHTTPErrorStatusException(
      BAD_REQUEST_400,
      PROTOCOL_ERROR,
      this.strings.format("expectedCommand", "CommandLogin")
    );
  }
}
