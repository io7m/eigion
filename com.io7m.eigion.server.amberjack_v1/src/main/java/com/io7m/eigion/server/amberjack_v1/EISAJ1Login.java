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

import com.io7m.eigion.error_codes.EIStandardErrorCodes;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.protocol.amberjack.EIAJCommandLogin;
import com.io7m.eigion.protocol.amberjack.EIAJResponseLogin;
import com.io7m.eigion.protocol.amberjack.cb.EIAJCB1Messages;
import com.io7m.eigion.protocol.api.EIProtocolException;
import com.io7m.eigion.server.controller.EISStrings;
import com.io7m.eigion.server.controller.command_exec.EISCommandExecutionFailure;
import com.io7m.eigion.server.controller.login.EILoginServiceType;
import com.io7m.eigion.server.database.api.EISDatabaseException;
import com.io7m.eigion.server.database.api.EISDatabaseType;
import com.io7m.eigion.server.http.EICommonInstrumentedServlet;
import com.io7m.eigion.server.http.EIHTTPErrorStatusException;
import com.io7m.eigion.server.http.EISRequestUniqueIDs;
import com.io7m.eigion.server.http.EISRequestUserAgents;
import com.io7m.eigion.server.service.limits.EIRequestLimitExceeded;
import com.io7m.eigion.server.service.limits.EIRequestLimitsType;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.io7m.eigion.model.EIPermission.AMBERJACK_ACCESS;
import static com.io7m.eigion.server.database.api.EISDatabaseRole.EIGION;
import static com.io7m.idstore.model.IdLoginMetadataStandard.remoteHost;
import static com.io7m.idstore.model.IdLoginMetadataStandard.remoteHostProxied;
import static com.io7m.idstore.model.IdLoginMetadataStandard.userAgent;

/**
 * A servlet that handles user logins.
 */

public final class EISAJ1Login extends EICommonInstrumentedServlet
{
  private final EISDatabaseType database;
  private final EIAJCB1Messages messages;
  private final EISStrings strings;
  private final EISAJ1Sends sends;
  private final EILoginServiceType login;
  private final EIRequestLimitsType limits;

  /**
   * A servlet that handles user logins.
   *
   * @param inServices The service directory
   */

  public EISAJ1Login(
    final EIServiceDirectoryType inServices)
  {
    super(Objects.requireNonNull(inServices, "services"));

    this.messages =
      inServices.requireService(EIAJCB1Messages.class);
    this.login =
      inServices.requireService(EILoginServiceType.class);
    this.strings =
      inServices.requireService(EISStrings.class);
    this.sends =
      inServices.requireService(EISAJ1Sends.class);
    this.limits =
      inServices.requireService(EIRequestLimitsType.class);
    this.database =
      inServices.requireService(EISDatabaseType.class);
  }

  @Override
  protected void service(
    final HttpServletRequest request,
    final HttpServletResponse response)
    throws IOException
  {
    final var requestId =
      EISRequestUniqueIDs.requestIdFor(request);

    try {
      final var command =
        this.readLoginCommand(request);
      final var metadata =
        readRequestMetadata(request);

      try (var connection = this.database.openConnection(EIGION)) {
        try (var transaction = connection.openTransaction()) {
          final var session =
            this.login.userLogin(
              transaction,
              requestId,
              command.userName(),
              command.password(),
              metadata,
              Set.of(AMBERJACK_ACCESS)
            );

          transaction.commit();
          final var httpSession = request.getSession(true);
          httpSession.setAttribute("ID", session.id());
          this.sendLoginResponse(request, response, session.user());
        }
      }
    } catch (final EIHTTPErrorStatusException e) {
      this.sends.sendError(
        response,
        requestId,
        e.statusCode(),
        e.errorCode(),
        e.getMessage()
      );
    } catch (final EISDatabaseException e) {
      this.sends.sendError(
        response,
        requestId,
        500,
        e.errorCode(),
        e.getMessage()
      );
    } catch (final EISCommandExecutionFailure e) {
      this.sends.sendError(
        response,
        requestId,
        e.httpStatusCode(),
        e.errorCode(),
        e.getMessage()
      );
    } catch (final InterruptedException e) {
      throw new IOException(e.getMessage(), e);
    }
  }

  private static Map<String, String> readRequestMetadata(
    final HttpServletRequest request)
  {
    final var metadata = new HashMap<String, String>(3);
    metadata.put(
      userAgent(),
      EISRequestUserAgents.requestUserAgent(request)
    );
    metadata.put(
      remoteHost(),
      request.getRemoteAddr()
    );
    metadata.put(
      remoteHostProxied(),
      Optional.ofNullable(request.getHeader("X-Forwarded-For"))
        .orElse("")
    );
    return Map.copyOf(metadata);
  }

  private void sendLoginResponse(
    final HttpServletRequest request,
    final HttpServletResponse response,
    final EIUser user)
    throws IOException
  {
    response.setStatus(200);
    response.setContentType(EIAJCB1Messages.contentType());

    try {
      final var data =
        this.messages.serialize(
          new EIAJResponseLogin(EISRequestUniqueIDs.requestIdFor(request), user)
        );
      response.setContentLength(data.length);
      try (var output = response.getOutputStream()) {
        output.write(data);
      }
    } catch (final EIProtocolException e) {
      throw new IOException(e);
    }
  }

  private EIAJCommandLogin readLoginCommand(
    final HttpServletRequest request)
    throws EIHTTPErrorStatusException, IOException
  {
    try (var input = this.limits.boundedMaximumInput(request, 1024)) {
      final var data = input.readAllBytes();
      final var message = this.messages.parse(data);
      if (message instanceof EIAJCommandLogin loginCommand) {
        return loginCommand;
      }
    } catch (final EIProtocolException | EIRequestLimitExceeded e) {
      throw new EIHTTPErrorStatusException(
        400,
        EIStandardErrorCodes.PROTOCOL_ERROR,
        e.getMessage(),
        e
      );
    }

    throw new EIHTTPErrorStatusException(
      400,
      EIStandardErrorCodes.PROTOCOL_ERROR,
      this.strings.format("expectedCommand", "CommandLogin")
    );
  }
}
