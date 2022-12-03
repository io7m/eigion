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

package com.io7m.eigion.server.amberjack_v1;

import com.io7m.eigion.server.controller.EISStrings;
import com.io7m.eigion.server.database.api.EISDatabaseException;
import com.io7m.eigion.server.http.EICommonInstrumentedServlet;
import com.io7m.eigion.server.http.EIHTTPErrorStatusException;
import com.io7m.eigion.server.http.EISRequestUniqueIDs;
import com.io7m.eigion.server.service.sessions.EISession;
import com.io7m.eigion.server.service.sessions.EISessionSecretIdentifier;
import com.io7m.eigion.server.service.sessions.EISessionServiceType;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Objects;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.AUTHENTICATION_ERROR;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.UNAUTHORIZED_401;

/**
 * A servlet that checks that a user is authenticated before delegating
 * execution to a subclass.
 */

public abstract class EISAJ1AuthenticatedServlet
  extends EICommonInstrumentedServlet
{
  private final EISAJ1Sends sends;
  private final EISStrings strings;
  private final EISessionServiceType sessions;

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

    this.strings =
      services.requireService(EISStrings.class);
    this.sends =
      services.requireService(EISAJ1Sends.class);
    this.sessions =
      services.requireService(EISessionServiceType.class);
  }

  protected final EISAJ1Sends sends()
  {
    return this.sends;
  }

  protected final EISStrings strings()
  {
    return this.strings;
  }

  protected abstract void serviceAuthenticated(
    HttpServletRequest request,
    HttpServletResponse servletResponse,
    EISession session)
    throws Exception;

  @Override
  protected final void service(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse)
    throws IOException
  {
    final var requestId =
      EISRequestUniqueIDs.requestIdFor(request);

    try {
      final var session = request.getSession(false);
      if (session != null) {
        final var sessionId =
          (EISessionSecretIdentifier) session.getAttribute("ID");

        if (sessionId != null) {
          final var userSessionOpt =
            this.sessions.findSession(sessionId);
          if (userSessionOpt.isPresent()) {
            this.serviceAuthenticated(
              request, servletResponse, userSessionOpt.get());
            return;
          }
        }
      }

      this.sends.sendError(
        servletResponse,
        requestId,
        UNAUTHORIZED_401,
        AUTHENTICATION_ERROR,
        this.strings.format("unauthorized")
      );
    } catch (final EIHTTPErrorStatusException e) {
      this.sends.sendError(
        servletResponse,
        requestId,
        e.statusCode(),
        e.errorCode(),
        e.getMessage()
      );
    } catch (final EISDatabaseException e) {
      this.sends.sendError(
        servletResponse,
        requestId,
        INTERNAL_SERVER_ERROR_500,
        e.errorCode(),
        e.getMessage()
      );
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }
}
