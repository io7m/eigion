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


package com.io7m.eigion.server.internal.pike;

import com.io7m.eigion.error_codes.EIErrorCode;
import com.io7m.eigion.protocol.pike.EIPResponseError;
import com.io7m.eigion.protocol.pike.EIPResponseType;
import com.io7m.eigion.server.database.api.EISDatabaseTransactionType;
import com.io7m.eigion.server.internal.EISClock;
import com.io7m.eigion.server.internal.EISRequests;
import com.io7m.eigion.server.internal.EISStrings;
import com.io7m.eigion.server.internal.command_exec.EISCommandContext;
import com.io7m.eigion.server.internal.sessions.EISUserSession;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

import static com.io7m.eigion.server.internal.EISRequestDecoration.requestIdFor;

/**
 * The command context for user API commands.
 */

public final class EISPCommandContext
  extends EISCommandContext<EIPResponseType>
{
  /**
   * The context for execution of a command (or set of commands in a
   * transaction).
   *
   * @param inServices      The service directory
   * @param inStrings       The string resources
   * @param inRequestId     The request ID
   * @param inTransaction   The transaction
   * @param inClock         The clock
   * @param inSession       The user session
   * @param remoteHost      The remote host
   * @param remoteUserAgent The remote user agent
   */

  public EISPCommandContext(
    final EIServiceDirectoryType inServices,
    final EISStrings inStrings,
    final UUID inRequestId,
    final EISDatabaseTransactionType inTransaction,
    final EISClock inClock,
    final EISUserSession inSession,
    final String remoteHost,
    final String remoteUserAgent)
  {
    super(
      inServices,
      inStrings,
      inRequestId,
      inTransaction,
      inClock,
      inSession,
      remoteHost,
      remoteUserAgent
    );
  }

  /**
   * Create a new command context from the given objects.
   *
   * @param services    The service directory
   * @param transaction The database transaction
   * @param request     The request
   * @param userSession The user session
   *
   * @return A context
   */

  public static EISPCommandContext create(
    final EIServiceDirectoryType services,
    final EISDatabaseTransactionType transaction,
    final HttpServletRequest request,
    final EISUserSession userSession)
  {
    return new EISPCommandContext(
      services,
      services.requireService(EISStrings.class),
      requestIdFor(request),
      transaction,
      services.requireService(EISClock.class),
      userSession,
      request.getRemoteHost(),
      EISRequests.requestUserAgent(request)
    );
  }

  @Override
  protected EIPResponseError error(
    final UUID id,
    final EIErrorCode errorCode,
    final String message)
  {
    return new EIPResponseError(id, errorCode, message);
  }
}
