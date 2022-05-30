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
import com.io7m.eigion.server.protocol.public_api.v1.EISP1Messages;
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
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;

import static com.io7m.eigion.server.vanilla.internal.EIServerRequestIDs.requestIdFor;
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
  private URI clientURI;
  private HttpServletResponse response;
  private UUID userId;

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

  private static String clientOf(
    final HttpServletRequest servletRequest,
    final UUID user)
  {
    return new StringBuilder(64)
      .append('[')
      .append(servletRequest.getRemoteAddr())
      .append(':')
      .append(servletRequest.getRemotePort())
      .append(' ')
      .append(user)
      .append(']')
      .toString();
  }

  private static String clientOf(
    final HttpServletRequest servletRequest)
  {
    return new StringBuilder(64)
      .append('[')
      .append(servletRequest.getRemoteAddr())
      .append(':')
      .append(servletRequest.getRemotePort())
      .append(']')
      .toString();
  }

  private static URI makeClientURI(
    final HttpServletRequest servletRequest)
  {
    return URI.create(
      new StringBuilder(64)
        .append("client:")
        .append(servletRequest.getRemoteAddr())
        .append(":")
        .append(servletRequest.getRemotePort())
        .toString()
    );
  }

  private static URI makeClientURI(
    final HttpServletRequest servletRequest,
    final UUID user)
  {
    return URI.create(
      new StringBuilder(64)
        .append("client:")
        .append(servletRequest.getRemoteAddr())
        .append(":")
        .append(servletRequest.getRemotePort())
        .append(":")
        .append(user)
        .toString()
    );
  }

  protected final UUID userId()
  {
    return this.userId;
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

  /**
   * @return The current client URI
   */

  public final URI clientURI()
  {
    return this.clientURI;
  }

  @Override
  protected final void service(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse)
    throws ServletException, IOException
  {
    MDC.put("client", clientOf(request));
    this.clientURI = makeClientURI(request);
    this.response = servletResponse;

    try {
      final var session = request.getSession(false);
      if (session != null) {
        this.userId = (UUID) session.getAttribute("UserID");
        MDC.put("client", clientOf(request, this.userId));
        this.clientURI = makeClientURI(request, this.userId);
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
    } finally {
      MDC.remove("client");
    }
  }
}
