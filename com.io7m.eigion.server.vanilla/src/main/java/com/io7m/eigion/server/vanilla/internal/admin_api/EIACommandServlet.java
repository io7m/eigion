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


package com.io7m.eigion.server.vanilla.internal.admin_api;

import com.io7m.eigion.model.EIPasswordException;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandType;
import com.io7m.eigion.protocol.admin_api.v1.EISA1Messages;
import com.io7m.eigion.protocol.api.EIProtocolException;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseTransactionType;
import com.io7m.eigion.server.database.api.EIServerDatabaseType;
import com.io7m.eigion.server.security.EISecurityException;
import com.io7m.eigion.server.vanilla.internal.EIHTTPErrorStatusException;
import com.io7m.eigion.server.vanilla.internal.EIRequestLimits;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.EIGION;
import static com.io7m.eigion.server.vanilla.internal.EIServerRequestDecoration.requestIdFor;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;

/**
 * A servlet for executing a single command.
 */

public final class EIACommandServlet extends EIAAuthenticatedServlet
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIACommandServlet.class);

  private final EIServerDatabaseType database;
  private final EIRequestLimits limits;
  private final EISA1Messages messages;
  private final EIACommandExecutor executor;
  private final EIServiceDirectoryType services;

  /**
   * A servlet for executing a single command.
   *
   * @param inServices The service directory
   */

  public EIACommandServlet(
    final EIServiceDirectoryType inServices)
  {
    super(inServices);

    this.services =
      Objects.requireNonNull(inServices, "inServices");
    this.database =
      inServices.requireService(EIServerDatabaseType.class);
    this.limits =
      inServices.requireService(EIRequestLimits.class);
    this.messages =
      inServices.requireService(EISA1Messages.class);
    this.executor =
      new EIACommandExecutor();
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
      requestIdFor(request);

    try (var input = this.limits.boundedMaximumInput(request, 1048576)) {
      final var data = input.readAllBytes();
      final var message = this.messages.parse(data);
      if (message instanceof EISA1CommandType command) {
        this.executeCommand(servletResponse, requestId, command);
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
      this.strings().format("expectedCommand", "EISA1CommandType")
    );
  }

  private void executeCommand(
    final HttpServletResponse servletResponse,
    final UUID requestId,
    final EISA1CommandType command)
    throws
    EIServerDatabaseException,
    IOException,
    EISecurityException,
    EIHTTPErrorStatusException,
    EIPasswordException
  {
    try (var connection = this.database.openConnection(EIGION)) {
      try (var transaction = connection.openTransaction()) {
        this.executeCommandInTransaction(
          servletResponse,
          requestId,
          command,
          transaction
        );
      }
    }
  }

  private void executeCommandInTransaction(
    final HttpServletResponse servletResponse,
    final UUID requestId,
    final EISA1CommandType command,
    final EIServerDatabaseTransactionType transaction)
    throws
    EIServerDatabaseException,
    IOException,
    EISecurityException,
    EIHTTPErrorStatusException,
    EIPasswordException
  {
    final var context =
      new EIACommandContext(
        this.services,
        this.strings(),
        requestId,
        transaction,
        this.clock(),
        this.admin()
      );

    final var result =
      this.executor.execute(context, command);

    this.sends()
      .send(servletResponse, result.httpStatus(), result.response());

    transaction.commit();
  }
}
