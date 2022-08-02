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

package com.io7m.eigion.server.vanilla.internal.public_api;

import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupCreateBegin;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupCreateCancel;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupCreateReady;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupCreateRequests;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupInvite;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupInviteByName;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupInviteCancel;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupInviteRespond;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupInvitesReceived;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupInvitesSent;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroups;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandType;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseType;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.security.EISecurityException;
import com.io7m.eigion.server.vanilla.internal.EIHTTPErrorStatusException;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandExecutionResult;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandExecutorType;

/**
 * A command executor for public commands.
 */

public final class EIPCommandExecutor
  implements EICommandExecutorType<
  EIPCommandContext,
  EISP1CommandType,
  EISP1ResponseType>
{
  /**
   * A command executor for public commands.
   */

  public EIPCommandExecutor()
  {

  }

  @Override
  public EICommandExecutionResult<EISP1ResponseType> execute(
    final EIPCommandContext context,
    final EISP1CommandType command)
    throws
    EIServerDatabaseException,
    EIHTTPErrorStatusException,
    EISecurityException
  {
    if (command instanceof EISP1CommandGroupCreateBegin c) {
      return new EIPCmdGroupCreateBegin().execute(context, c);
    }
    if (command instanceof EISP1CommandGroupCreateRequests c) {
      return new EIPCmdGroupCreateRequests().execute(context, c);
    }
    if (command instanceof EISP1CommandGroupCreateCancel c) {
      return new EIPCmdGroupCreateCancel().execute(context, c);
    }
    if (command instanceof EISP1CommandGroupCreateReady c) {
      return new EIPCmdGroupCreateReady().execute(context, c);
    }
    if (command instanceof EISP1CommandGroups c) {
      return new EIPCmdGroups().execute(context, c);
    }
    if (command instanceof EISP1CommandGroupInvite c) {
      return new EIPCmdGroupInvite().execute(context, c);
    }
    if (command instanceof EISP1CommandGroupInviteByName c) {
      return new EIPCmdGroupInviteByName().execute(context, c);
    }
    if (command instanceof EISP1CommandGroupInvitesSent c) {
      return new EIPCmdGroupInvitesSent().execute(context, c);
    }
    if (command instanceof EISP1CommandGroupInvitesReceived c) {
      return new EIPCmdGroupInvitesReceived().execute(context, c);
    }
    if (command instanceof EISP1CommandGroupInviteCancel c) {
      return new EIPCmdGroupInviteCancel().execute(context, c);
    }
    if (command instanceof EISP1CommandGroupInviteRespond c) {
      return new EIPCmdGroupInviteRespond().execute(context, c);
    }

    throw new IllegalStateException();
  }
}
