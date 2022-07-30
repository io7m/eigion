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


package com.io7m.eigion.server.vanilla.internal.public_api;

import com.io7m.eigion.model.EIPasswordException;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandLogin;
import com.io7m.eigion.server.api.EIServerUserLoggedIn;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseType;
import com.io7m.eigion.server.database.api.EIServerDatabaseUsersQueriesType;
import com.io7m.eigion.protocol.api.EIProtocolException;
import com.io7m.eigion.protocol.public_api.v1.EISP1Messages;
import com.io7m.eigion.server.vanilla.internal.EIHTTPErrorStatusException;
import com.io7m.eigion.server.vanilla.internal.EIRequestLimits;
import com.io7m.eigion.server.vanilla.internal.EIServerClock;
import com.io7m.eigion.server.vanilla.internal.EIServerEventBus;
import com.io7m.eigion.server.vanilla.internal.EIServerStrings;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.EIGION;
import static com.io7m.eigion.server.vanilla.internal.EIServerRequestDecoration.requestIdFor;
import static com.io7m.eigion.server.vanilla.logging.EIServerMDCRequestProcessor.mdcForRequest;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.METHOD_NOT_ALLOWED_405;
import static org.eclipse.jetty.http.HttpStatus.UNAUTHORIZED_401;

/**
 * A servlet that handles user logins.
 */

public final class EIPLogin extends HttpServlet
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIPLogin.class);

  private final EIServerDatabaseType database;
  private final EISP1Messages messages;
  private final EIServerStrings strings;
  private final EIServerEventBus events;
  private final EIServerClock clock;
  private final EIPSends errors;
  private final EIRequestLimits limits;

  /**
   * A servlet that handles user logins.
   *
   * @param inServices The service directory
   */

  public EIPLogin(
    final EIServiceDirectoryType inServices)
  {
    Objects.requireNonNull(inServices, "inServices");

    this.database =
      inServices.requireService(EIServerDatabaseType.class);
    this.messages =
      inServices.requireService(EISP1Messages.class);
    this.strings =
      inServices.requireService(EIServerStrings.class);
    this.events =
      inServices.requireService(EIServerEventBus.class);
    this.clock =
      inServices.requireService(EIServerClock.class);
    this.errors =
      inServices.requireService(EIPSends.class);
    this.limits =
      inServices.requireService(EIRequestLimits.class);
  }

  @Override
  protected void service(
    final HttpServletRequest request,
    final HttpServletResponse response)
    throws IOException
  {
    try (var ignored0 = mdcForRequest(request)) {
      try {
        if (!Objects.equals(request.getMethod(), "POST")) {
          throw new EIHTTPErrorStatusException(
            METHOD_NOT_ALLOWED_405,
            "http",
            this.strings.format("methodNotAllowed")
          );
        }

        final var login =
          this.readLoginCommand(request);

        try (var connection = this.database.openConnection(EIGION)) {
          try (var transaction = connection.openTransaction()) {
            final var users =
              transaction.queries(EIServerDatabaseUsersQueriesType.class);
            this.tryLogin(request, response, users, login);
            transaction.commit();
          }
        }

      } catch (final EIHTTPErrorStatusException e) {
        this.errors.sendError(
          response,
          requestIdFor(request),
          e.statusCode(),
          e.errorCode(),
          e.getMessage()
        );
      } catch (final EIPasswordException e) {
        LOG.debug("password: ", e);
        this.errors.sendError(
          response,
          requestIdFor(request),
          INTERNAL_SERVER_ERROR_500,
          "password",
          e.getMessage()
        );
      } catch (final EIServerDatabaseException e) {
        LOG.debug("database: ", e);
        this.errors.sendError(
          response,
          requestIdFor(request),
          INTERNAL_SERVER_ERROR_500,
          "database",
          e.getMessage()
        );
      }
    }
  }

  private void tryLogin(
    final HttpServletRequest request,
    final HttpServletResponse response,
    final EIServerDatabaseUsersQueriesType users,
    final EISP1CommandLogin login)
    throws
    EIHTTPErrorStatusException,
    EIServerDatabaseException,
    EIPasswordException
  {
    final var userOpt =
      users.userGetForName(login.userName());

    if (userOpt.isEmpty()) {
      throw new EIHTTPErrorStatusException(
        UNAUTHORIZED_401,
        "authentication",
        this.strings.format("loginFailed")
      );
    }

    final var user =
      userOpt.get();
    final var ok =
      user.password().check(login.password());

    if (!ok) {
      throw new EIHTTPErrorStatusException(
        UNAUTHORIZED_401,
        "authentication",
        this.strings.format("loginFailed")
      );
    }

    LOG.info("user '{}' logged in", login.userName());
    final var session = request.getSession();
    session.setAttribute("UserID", user.id());
    response.setStatus(200);

    users.userLogin(user.id(), request.getRemoteAddr());

    this.events.publish(
      new EIServerUserLoggedIn(
        this.clock.now(),
        requestIdFor(request),
        user.id(),
        user.name(),
        request.getRemoteAddr())
    );
  }

  private EISP1CommandLogin readLoginCommand(
    final HttpServletRequest request)
    throws EIHTTPErrorStatusException, IOException
  {
    try (var input = this.limits.boundedMaximumInput(request, 1024)) {
      final var data = input.readAllBytes();
      final var message = this.messages.parse(data);
      if (message instanceof EISP1CommandLogin login) {
        return login;
      }
    } catch (final EIProtocolException e) {
      throw new EIHTTPErrorStatusException(
        BAD_REQUEST_400,
        "protocol",
        e.getMessage(),
        e
      );
    }

    throw new EIHTTPErrorStatusException(
      BAD_REQUEST_400,
      "protocol",
      this.strings.format("expectedCommand", "CommandLogin")
    );
  }
}
