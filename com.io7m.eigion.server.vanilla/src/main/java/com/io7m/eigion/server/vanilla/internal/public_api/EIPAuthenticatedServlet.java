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
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.protocol.public_api.v1.EISP1Messages;
import com.io7m.eigion.server.vanilla.internal.EIHTTPErrorStatusException;
import com.io7m.eigion.server.vanilla.internal.EIServerClock;
import com.io7m.eigion.server.vanilla.internal.EIServerEventBus;
import com.io7m.eigion.server.vanilla.internal.EIServerStrings;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import static com.io7m.eigion.server.vanilla.internal.EIServerRequestDecoration.requestIdFor;
import static com.io7m.eigion.server.vanilla.logging.EIServerMDCRequestProcessor.mdcForRequest;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;

/**
 * A servlet that checks that a user is authenticated before delegating
 * execution to a subclass.
 */

public abstract class EIPAuthenticatedServlet extends HttpServlet
{
  private final EIPSends sends;
  private final EIServerClock clock;
  private final EIServerEventBus events;
  private final EIServerStrings strings;
  private final EISP1Messages messages;
  private EIPUser user;

  /**
   * A servlet that checks that a user is authenticated before delegating
   * execution to a subclass.
   *
   * @param services The service directory
   */

  protected EIPAuthenticatedServlet(
    final EIServiceDirectoryType services)
  {
    Objects.requireNonNull(services, "services");

    this.messages =
      services.requireService(EISP1Messages.class);
    this.strings =
      services.requireService(EIServerStrings.class);
    this.events =
      services.requireService(EIServerEventBus.class);
    this.clock =
      services.requireService(EIServerClock.class);
    this.sends =
      services.requireService(EIPSends.class);
  }

  /**
   * @return The authenticated user
   */

  protected final EIPUser user()
  {
    return this.user;
  }

  protected final UUID userId()
  {
    return this.user().id();
  }

  protected final EIPSends sends()
  {
    return this.sends;
  }

  protected final EIServerClock clock()
  {
    return this.clock;
  }

  protected final EIServerEventBus events()
  {
    return this.events;
  }

  protected final EIServerStrings strings()
  {
    return this.strings;
  }

  protected final EISP1Messages messages()
  {
    return this.messages;
  }

  protected abstract Logger logger();

  protected abstract void serviceAuthenticated(
    HttpServletRequest request,
    HttpServletResponse servletResponse,
    HttpSession session)
    throws Exception;

  @Override
  protected final void service(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse)
    throws ServletException, IOException
  {
    try (var ignored0 = mdcForRequest((Request) request)) {

      try {
        final var session = request.getSession(false);
        if (session != null) {
          final var userId = (UUID) session.getAttribute("UserID");
          this.user = new EIPUser(userId);
          this.serviceAuthenticated(request, servletResponse, session);
          return;
        }

        this.logger().debug("unauthenticated!");
        this.sends.sendError(
          servletResponse,
          requestIdFor(request),
          HttpStatus.UNAUTHORIZED_401,
          "unauthorized",
          this.strings.format("unauthorized")
        );
      } catch (final EIHTTPErrorStatusException e) {
        this.sends.sendError(
          servletResponse,
          requestIdFor(request),
          e.statusCode(),
          e.errorCode(),
          e.getMessage()
        );
      } catch (final EIPasswordException e) {
        this.logger().debug("password: ", e);
        this.sends.sendError(
          servletResponse,
          requestIdFor(request),
          INTERNAL_SERVER_ERROR_500,
          "password",
          e.getMessage()
        );
      } catch (final EIServerDatabaseException e) {
        this.logger().debug("database: ", e);
        this.sends.sendError(
          servletResponse,
          requestIdFor(request),
          INTERNAL_SERVER_ERROR_500,
          "database",
          e.getMessage()
        );
      } catch (final Exception e) {
        this.logger().trace("exception: ", e);
        throw new IOException(e);
      }
    }
  }
}
