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

package com.io7m.eigion.server.pike_v1;

import com.io7m.eigion.protocol.api.EIProtocolException;
import com.io7m.eigion.protocol.pike.EIPCommandType;
import com.io7m.eigion.protocol.pike.EIPResponseError;
import com.io7m.eigion.protocol.pike.EIPResponseType;
import com.io7m.eigion.protocol.pike.cb.EIPCB1Messages;
import com.io7m.eigion.server.controller.command_exec.EISCommandExecutionFailure;
import com.io7m.eigion.server.controller.pike.EISPCommandContext;
import com.io7m.eigion.server.controller.pike.EISPCommandExecutor;
import com.io7m.eigion.server.database.api.EISDatabaseException;
import com.io7m.eigion.server.database.api.EISDatabaseTransactionType;
import com.io7m.eigion.server.database.api.EISDatabaseType;
import com.io7m.eigion.server.http.EIHTTPErrorStatusException;
import com.io7m.eigion.server.http.EISRequestUniqueIDs;
import com.io7m.eigion.server.service.limits.EIRequestLimitsType;
import com.io7m.eigion.server.service.sessions.EISession;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.IO_ERROR;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.eigion.server.database.api.EISDatabaseRole.EIGION;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;

/**
 * A servlet for executing a single command.
 */

public final class EISP1CommandServlet extends EISP1AuthenticatedServlet
{
  private final EISDatabaseType database;
  private final EIRequestLimitsType limits;
  private final EIPCB1Messages messages;
  private final EISPCommandExecutor executor;
  private final EIServiceDirectoryType services;

  /**
   * A servlet for executing a single command.
   *
   * @param inServices The service directory
   */

  public EISP1CommandServlet(
    final EIServiceDirectoryType inServices)
  {
    super(inServices);

    this.services =
      Objects.requireNonNull(inServices, "inServices");
    this.database =
      inServices.requireService(EISDatabaseType.class);
    this.limits =
      inServices.requireService(EIRequestLimitsType.class);
    this.messages =
      inServices.requireService(EIPCB1Messages.class);
    this.executor =
      new EISPCommandExecutor();
  }

  @Override
  protected void serviceAuthenticated(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final EISession session)
    throws Exception
  {
    try (var input = this.limits.boundedMaximumInput(request, 1048576)) {
      final var data = input.readAllBytes();
      final var message = this.messages.parse(data);
      if (message instanceof EIPCommandType<?> command) {
        this.executeCommand(request, servletResponse, session, command);
        return;
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
      this.strings().format("expectedCommand", "EIPCommandType")
    );
  }

  private void executeCommand(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final EISession session,
    final EIPCommandType<?> command)
    throws EISDatabaseException, IOException
  {
    try (var connection = this.database.openConnection(EIGION)) {
      try (var transaction = connection.openTransaction()) {
        this.executeCommandInTransaction(
          request,
          servletResponse,
          session,
          command,
          transaction
        );
      }
    }
  }

  private void executeCommandInTransaction(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final EISession session,
    final EIPCommandType<?> command,
    final EISDatabaseTransactionType transaction)
    throws IOException
  {
    final var requestId =
      EISRequestUniqueIDs.requestIdFor(request);

    final var context =
      new EISPCommandContext(
        this.services,
        requestId,
        transaction,
        session,
        request.getRemoteHost(),
        Optional.ofNullable(request.getHeader("User-Agent"))
          .orElse("<unavailable>")
      );

    final var sends = this.sends();

    try {
      final EIPResponseType result = this.executor.execute(context, command);
      sends.send(servletResponse, 200, result);
      if (result instanceof EIPResponseError error) {
        Span.current()
          .setAttribute("eigion.errorCode", error.errorCode().id());
      } else {
        transaction.commit();
      }
    } catch (final EISCommandExecutionFailure e) {
      sends.send(
        servletResponse,
        e.httpStatusCode(),
        new EIPResponseError(e.requestId(), e.errorCode(), e.getMessage())
      );
    } catch (final Exception e) {
      sends.send(
        servletResponse,
        500,
        new EIPResponseError(requestId, IO_ERROR, e.getMessage())
      );
    }
  }
}
