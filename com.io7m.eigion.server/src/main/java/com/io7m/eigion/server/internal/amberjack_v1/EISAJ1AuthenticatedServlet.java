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

package com.io7m.eigion.server.internal.amberjack_v1;

import com.io7m.eigion.protocol.amberjack.cb.EIAJCB1Messages;
import com.io7m.eigion.server.database.api.EISDatabaseException;
import com.io7m.eigion.server.database.api.EISDatabaseType;
import com.io7m.eigion.server.internal.EIHTTPErrorStatusException;
import com.io7m.eigion.server.internal.EISClock;
import com.io7m.eigion.server.internal.EISRequestDecoration;
import com.io7m.eigion.server.internal.EISStrings;
import com.io7m.eigion.server.internal.common.EICommonInstrumentedServlet;
import com.io7m.eigion.server.internal.sessions.EISUserSession;
import com.io7m.eigion.server.internal.sessions.EISUserSessionService;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.SQL_ERROR;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;

/**
 * A servlet that checks that a user is authenticated before delegating
 * execution to a subclass.
 */

public abstract class EISAJ1AuthenticatedServlet
  extends EICommonInstrumentedServlet
{
  private final EISAJ1Sends sends;
  private final EISClock clock;
  private final EISStrings strings;
  private final EIAJCB1Messages messages;
  private final EISDatabaseType database;
  private final EISUserSessionService userSessions;
  private EISUserSession userSession;

  /**
   * A servlet that checks that a user is authenticated before delegating
   * execution to a subclass.
   *
   * @param services The service directory
   */

  protected EISAJ1AuthenticatedServlet(
    final EIServiceDirectoryType services)
  {
    super(Objects.requireNonNull(services, "services"));

    this.messages =
      services.requireService(EIAJCB1Messages.class);
    this.strings =
      services.requireService(EISStrings.class);
    this.clock =
      services.requireService(EISClock.class);
    this.sends =
      services.requireService(EISAJ1Sends.class);
    this.database =
      services.requireService(EISDatabaseType.class);
    this.userSessions =
      services.requireService(EISUserSessionService.class);
  }

  protected final EISUserSession userSession()
  {
    return this.userSession;
  }

  protected final EISAJ1Sends sends()
  {
    return this.sends;
  }

  protected final EISClock clock()
  {
    return this.clock;
  }

  protected final EISStrings strings()
  {
    return this.strings;
  }

  protected final EIAJCB1Messages messages()
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
    try {
      final var session = request.getSession(false);
      if (session != null) {
        final var userId = (UUID) session.getAttribute("UserID");
        if (userId != null) {
          final var userSessionNow =
            this.userSessions.find(userId, session.getId());
          if (userSessionNow.isPresent()) {
            this.userSession = userSessionNow.get();
            this.serviceAuthenticated(request, servletResponse, session);
            return;
          }
        }
      }

      this.sends.sendError(
        servletResponse,
        EISRequestDecoration.requestIdFor(request),
        HttpStatus.UNAUTHORIZED_401,
        AUTHENTICATION_ERROR,
        this.strings.format("unauthorized")
      );
    } catch (final EIHTTPErrorStatusException e) {
      this.sends.sendError(
        servletResponse,
        EISRequestDecoration.requestIdFor(request),
        e.statusCode(),
        e.errorCode(),
        e.getMessage()
      );
    } catch (final EISDatabaseException e) {
      this.sends.sendError(
        servletResponse,
        EISRequestDecoration.requestIdFor(request),
        INTERNAL_SERVER_ERROR_500,
        SQL_ERROR,
        e.getMessage()
      );
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }
}
