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


package com.io7m.eigion.server.internal.command_exec;

import com.io7m.eigion.error_codes.EIErrorCode;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.model.EIValidityException;
import com.io7m.eigion.protocol.api.EIProtocolException;
import com.io7m.eigion.protocol.api.EIProtocolMessageType;
import com.io7m.eigion.server.database.api.EISDatabaseException;
import com.io7m.eigion.server.database.api.EISDatabaseTransactionType;
import com.io7m.eigion.server.internal.EISClock;
import com.io7m.eigion.server.internal.EISStrings;
import com.io7m.eigion.server.internal.EISTelemetryService;
import com.io7m.eigion.server.internal.security.EISecurityException;
import com.io7m.eigion.server.internal.sessions.EISUserSession;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import io.opentelemetry.api.trace.Tracer;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.HTTP_PARAMETER_INVALID;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.SECURITY_POLICY_DENIED;

/**
 * The context for execution of a command (or set of commands in a
 * transaction).
 *
 * @param <E> The type of error messages
 */

public abstract class EISCommandContext<E extends EIProtocolMessageType>
{
  private final EIServiceDirectoryType services;
  private final UUID requestId;
  private final EISDatabaseTransactionType transaction;
  private final EISClock clock;
  private final EISStrings strings;
  private final EISUserSession userSession;
  private final String remoteHost;
  private final String remoteUserAgent;
  private final Tracer tracer;

  /**
   * The context for execution of a command (or set of commands in a
   * transaction).
   *
   * @param inServices        The service directory
   * @param inStrings         The string resources
   * @param inRequestEI       The request ID
   * @param inTransaction     The transaction
   * @param inClock           The clock
   * @param inSession         The user session
   * @param inRemoteHost      The remote host
   * @param inRemoteUserAgent The remote user agent
   */

  public EISCommandContext(
    final EIServiceDirectoryType inServices,
    final EISStrings inStrings,
    final UUID inRequestEI,
    final EISDatabaseTransactionType inTransaction,
    final EISClock inClock,
    final EISUserSession inSession,
    final String inRemoteHost,
    final String inRemoteUserAgent)
  {
    this.services =
      Objects.requireNonNull(inServices, "services");
    this.requestId =
      Objects.requireNonNull(inRequestEI, "requestId");
    this.transaction =
      Objects.requireNonNull(inTransaction, "transaction");
    this.clock =
      Objects.requireNonNull(inClock, "clock");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.userSession =
      Objects.requireNonNull(inSession, "inSession");
    this.remoteHost =
      Objects.requireNonNull(inRemoteHost, "remoteHost");
    this.remoteUserAgent =
      Objects.requireNonNull(inRemoteUserAgent, "remoteUserAgent");
    this.tracer =
      inServices.requireService(EISTelemetryService.class)
        .tracer();
  }

  /**
   * @return The user session
   */

  public final EISUserSession userSession()
  {
    return this.userSession;
  }

  /**
   * @return The remote host
   */

  public final String remoteHost()
  {
    return this.remoteHost;
  }

  /**
   * @return The remote user agent
   */

  public final String remoteUserAgent()
  {
    return this.remoteUserAgent;
  }

  /**
   * @return The service directory used during execution
   */

  public final EIServiceDirectoryType services()
  {
    return this.services;
  }

  /**
   * @return The ID of the incoming request
   */

  public final UUID requestId()
  {
    return this.requestId;
  }

  /**
   * @return The database transaction
   */

  public final EISDatabaseTransactionType transaction()
  {
    return this.transaction;
  }

  /**
   * @return The OpenTelemetry tracer
   */

  public final Tracer tracer()
  {
    return this.tracer;
  }

  /**
   * @return The current time
   */

  public final OffsetDateTime now()
  {
    return this.clock.now();
  }

  /**
   * Produce an exception indicating an error, with a formatted error message.
   *
   * @param statusCode The HTTP status code
   * @param errorCode  The error code
   * @param messageId  The string resource message ID
   * @param args       The string resource format arguments
   *
   * @return An execution failure
   */

  public final EISCommandExecutionFailure failFormatted(
    final int statusCode,
    final EIErrorCode errorCode,
    final String messageId,
    final Object... args)
  {
    return this.fail(
      statusCode,
      errorCode,
      this.strings.format(messageId, args)
    );
  }

  /**
   * Produce an exception indicating an error, with a string constant message.
   *
   * @param statusCode The HTTP status code
   * @param errorCode  The error code
   * @param message    The string message
   *
   * @return An execution failure
   */

  public final EISCommandExecutionFailure fail(
    final int statusCode,
    final EIErrorCode errorCode,
    final String message)
  {
    return new EISCommandExecutionFailure(
      message,
      this.requestId,
      statusCode,
      errorCode
    );
  }

  protected abstract E error(
    UUID id,
    EIErrorCode errorCode,
    String message
  );

  /**
   * Produce an exception indicating a database error.
   *
   * @param e The database exception
   *
   * @return An execution failure
   */

  public final EISCommandExecutionFailure failDatabase(
    final EISDatabaseException e)
  {
    return new EISCommandExecutionFailure(
      e.getMessage(),
      e,
      this.requestId,
      500,
      e.errorCode()
    );
  }

  /**
   * Produce an exception indicating a validation error.
   *
   * @param e The exception
   *
   * @return An execution failure
   */

  public EISCommandExecutionFailure failValidity(
    final EIValidityException e)
  {
    return new EISCommandExecutionFailure(
      e.getMessage(),
      e,
      this.requestId,
      400,
      HTTP_PARAMETER_INVALID
    );
  }

  /**
   * Produce an exception indicating a protocol error.
   *
   * @param e The exception
   *
   * @return An execution failure
   */

  public EISCommandExecutionFailure failProtocol(
    final EIProtocolException e)
  {
    return new EISCommandExecutionFailure(
      e.getMessage(),
      e,
      this.requestId,
      400,
      PROTOCOL_ERROR
    );
  }

  /**
   * Produce an exception indicating a security policy error.
   *
   * @param e The security exception
   *
   * @return An execution failure
   */

  public EISCommandExecutionFailure failSecurity(
    final EISecurityException e)
  {
    return new EISCommandExecutionFailure(
      e.getMessage(),
      e,
      this.requestId,
      403,
      SECURITY_POLICY_DENIED
    );
  }

  /**
   * @return The current user session's user
   */

  public final EIUser user()
  {
    return this.userSession.user();
  }
}
