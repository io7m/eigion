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
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAdminCreate;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAdminGet;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAdminGetByEmail;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAdminGetByName;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAdminSearch;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAuditGet;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandGroupInviteSetStatus;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandGroupInvites;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandServicesList;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandType;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserBan;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserCreate;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserGet;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserGetByEmail;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserGetByName;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserSearch;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserUnban;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserUpdate;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseType;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.security.EISecurityException;
import com.io7m.eigion.server.vanilla.internal.EIHTTPErrorStatusException;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandExecutionResult;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandExecutorType;

/**
 * A general executor for any command.
 */

public final class EIACommandExecutor
  implements EICommandExecutorType<
  EIACommandContext,
  EISA1CommandType,
  EISA1ResponseType>
{
  /**
   * A general executor for any command.
   */

  public EIACommandExecutor()
  {

  }

  @Override
  public EICommandExecutionResult<EISA1ResponseType> execute(
    final EIACommandContext context,
    final EISA1CommandType command)
    throws
    EIServerDatabaseException,
    EISecurityException,
    EIHTTPErrorStatusException,
    EIPasswordException
  {
    if (command instanceof EISA1CommandAuditGet c) {
      return new EIACmdAuditGetByTime().execute(context, c);
    }
    if (command instanceof EISA1CommandUserCreate c) {
      return new EIACmdUserCreate().execute(context, c);
    }
    if (command instanceof EISA1CommandUserGet c) {
      return new EIACmdUserGet().execute(context, c);
    }
    if (command instanceof EISA1CommandUserGetByName c) {
      return new EIACmdUserGetByName().execute(context, c);
    }
    if (command instanceof EISA1CommandUserSearch c) {
      return new EIACmdUserSearch().execute(context, c);
    }
    if (command instanceof EISA1CommandUserGetByEmail c) {
      return new EIACmdUserGetByEmail().execute(context, c);
    }
    if (command instanceof EISA1CommandServicesList c) {
      return new EIACmdServicesList().execute(context, c);
    }
    if (command instanceof EISA1CommandAdminCreate c) {
      return new EIACmdAdminCreate().execute(context, c);
    }
    if (command instanceof EISA1CommandAdminGet c) {
      return new EIACmdAdminGet().execute(context, c);
    }
    if (command instanceof EISA1CommandAdminGetByName c) {
      return new EIACmdAdminGetByName().execute(context, c);
    }
    if (command instanceof EISA1CommandAdminSearch c) {
      return new EIACmdAdminSearch().execute(context, c);
    }
    if (command instanceof EISA1CommandAdminGetByEmail c) {
      return new EIACmdAdminGetByEmail().execute(context, c);
    }
    if (command instanceof EISA1CommandGroupInvites c) {
      return new EIACmdGroupInvites().execute(context, c);
    }
    if (command instanceof EISA1CommandGroupInviteSetStatus c) {
      return new EIACmdGroupInviteSetStatus().execute(context, c);
    }
    if (command instanceof EISA1CommandUserBan c) {
      return new EIACmdUserBan().execute(context, c);
    }
    if (command instanceof EISA1CommandUserUnban c) {
      return new EIACmdUserUnban().execute(context, c);
    }
    if (command instanceof EISA1CommandUserUpdate c) {
      return new EIACmdUserUpdate().execute(context, c);
    }

    throw new IllegalStateException();
  }
}
