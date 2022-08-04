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

import com.io7m.eigion.error_codes.EIErrorCode;
import com.io7m.eigion.model.EIAdmin;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseError;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseType;
import com.io7m.eigion.server.database.api.EIServerDatabaseTransactionType;
import com.io7m.eigion.server.vanilla.internal.EIServerClock;
import com.io7m.eigion.server.vanilla.internal.EIServerStrings;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandContext;
import com.io7m.eigion.services.api.EIServiceDirectoryType;

import java.util.Objects;
import java.util.UUID;

/**
 * The context for execution of a command (or set of commands in a
 * transaction).
 */

public final class EIACommandContext extends EICommandContext<EISA1ResponseType>
{
  private final EIAdmin admin;

  /**
   * @return The user executing the command.
   */

  public EIAdmin admin()
  {
    return this.admin;
  }

  /**
   * The context for execution of a command (or set of commands in a
   * transaction).
   *
   * @param inServices    The service directory
   * @param inStrings     The string resources
   * @param inRequestId   The request ID
   * @param inTransaction The transaction
   * @param inClock       The clock
   * @param inAdmin       The admin executing the command
   */

  public EIACommandContext(
    final EIServiceDirectoryType inServices,
    final EIServerStrings inStrings,
    final UUID inRequestId,
    final EIServerDatabaseTransactionType inTransaction,
    final EIServerClock inClock,
    final EIAdmin inAdmin)
  {
    super(inServices, inStrings, inRequestId, inTransaction, inClock);
    this.admin = Objects.requireNonNull(inAdmin, "inAdmin");
  }

  @Override
  protected EISA1ResponseError error(
    final UUID id,
    final EIErrorCode errorCode,
    final String message)
  {
    return new EISA1ResponseError(id, errorCode.id(), message);
  }
}
