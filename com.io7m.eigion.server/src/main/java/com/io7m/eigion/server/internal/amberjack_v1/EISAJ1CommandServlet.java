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

import com.io7m.eigion.protocol.amberjack.EIAJCommandType;
import com.io7m.eigion.protocol.amberjack.EIAJResponseError;
import com.io7m.eigion.protocol.amberjack.EIAJResponseType;
import com.io7m.eigion.protocol.amberjack.cb.EIAJCB1Messages;
import com.io7m.eigion.protocol.api.EIProtocolException;
import com.io7m.eigion.server.database.api.EISDatabaseException;
import com.io7m.eigion.server.database.api.EISDatabaseTransactionType;
import com.io7m.eigion.server.database.api.EISDatabaseType;
import com.io7m.eigion.server.internal.EIHTTPErrorStatusException;
import com.io7m.eigion.server.internal.EISRequestDecoration;
import com.io7m.eigion.server.internal.EISRequestLimits;
import com.io7m.eigion.server.internal.amberjack.EISAJCommandContext;
import com.io7m.eigion.server.internal.amberjack.EISAJCommandExecutor;
import com.io7m.eigion.server.internal.command_exec.EISCommandExecutionFailure;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.eigion.server.database.api.EISDatabaseRole.EIGION;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;

/**
 * A servlet for executing a single command.
 */

public final class EISAJ1CommandServlet extends EISAJ1AuthenticatedServlet
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EISAJ1CommandServlet.class);

  private final EISDatabaseType database;
  private final EISRequestLimits limits;
  private final EIAJCB1Messages messages;
  private final EISAJCommandExecutor executor;
  private final EIServiceDirectoryType services;

  /**
   * A servlet for executing a single command.
   *
   * @param inServices The service directory
   */

  public EISAJ1CommandServlet(
    final EIServiceDirectoryType inServices)
  {
    super(inServices);

    this.services =
      Objects.requireNonNull(inServices, "inServices");
    this.database =
      inServices.requireService(EISDatabaseType.class);
    this.limits =
      inServices.requireService(EISRequestLimits.class);
    this.messages =
      inServices.requireService(EIAJCB1Messages.class);
    this.executor =
      new EISAJCommandExecutor();
  }

  @Override
  protected Logger logger()
  {
    return LOG;
  }

  @Override
  protected void serviceAuthenticated(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final HttpSession session)
    throws Exception
  {
    final var requestId =
      EISRequestDecoration.requestIdFor(request);

    try (var input = this.limits.boundedMaximumInput(request, 1048576)) {
      final var data = input.readAllBytes();
      final var message = this.messages.parse(data);
      if (message instanceof EIAJCommandType<?> command) {
        this.executeCommand(request, servletResponse, command);
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
      this.strings().format("expectedCommand", "EIAJCommandType")
    );
  }

  private void executeCommand(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final EIAJCommandType<?> command)
    throws EISDatabaseException, IOException, InterruptedException
  {
    try (var connection = this.database.openConnection(EIGION)) {
      try (var transaction = connection.openTransaction()) {
        this.executeCommandInTransaction(
          request,
          servletResponse,
          command,
          transaction
        );
      }
    }
  }

  private void executeCommandInTransaction(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final EIAJCommandType<?> command,
    final EISDatabaseTransactionType transaction)
    throws IOException, EISDatabaseException
  {
    final var context =
      EISAJCommandContext.create(
        this.services,
        transaction,
        request,
        this.userSession()
      );

    final var sends =
      this.sends();

    try {
      final EIAJResponseType result = this.executor.execute(context, command);
      sends.send(servletResponse, 200, result);
      if (result instanceof EIAJResponseError error) {
        Span.current().setAttribute("eigion.errorCode", error.errorCode().id());
      } else {
        transaction.commit();
      }
    } catch (final EISCommandExecutionFailure e) {
      Span.current().setAttribute("eigion.errorCode", e.errorCode().id());
      sends.send(
        servletResponse,
        e.httpStatusCode(),
        new EIAJResponseError(
          e.requestId(),
          e.errorCode(),
          e.getMessage()
        ));
    }
  }
}
