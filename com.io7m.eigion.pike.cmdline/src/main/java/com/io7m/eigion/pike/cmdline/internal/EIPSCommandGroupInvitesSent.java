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

package com.io7m.eigion.pike.cmdline.internal;

import com.io7m.eigion.pike.api.EIPClientException;
import org.jline.terminal.Terminal;

import static com.io7m.eigion.pike.cmdline.internal.EIPSCommandResult.SUCCESS;

/**
 * List group invites sent.
 */

public final class EIPSCommandGroupInvitesSent
  extends EIPSAbstractCommand<EIPSCommandGroupInvitesSent.Parameters>
{
  /**
   * List groups.
   *
   * @param inController The controller
   * @param inStrings    The string resources
   */

  public EIPSCommandGroupInvitesSent(
    final EIPSController inController,
    final EIPStrings inStrings)
  {
    super(inController, inStrings, "group-invites-sent");
  }

  @Override
  protected Parameters createEmptyParameters()
  {
    return new Parameters();
  }

  @Override
  protected EIPSCommandResult runActual(
    final Terminal terminal,
    final Parameters parameters)
    throws EIPClientException, InterruptedException
  {
    final var invites =
      this.controller()
        .client()
        .groupInvitesSent();

    final var writer = terminal.writer();
    final var str = this.strings();

    for (final var invite : invites) {
      writer.println(
        str.format("groupInvite.group", invite.group())
      );
      writer.println(
        str.format("groupInvite.token", invite.token())
      );
      writer.println(
        str.format("groupInvite.userInviting", invite.userInviting())
      );
      writer.println(
        str.format("groupInvite.userInvitingName", invite.userInvitingName())
      );
      writer.println(
        str.format("groupInvite.userBeingInvited", invite.userBeingInvited())
      );
      writer.println(
        str.format("groupInvite.userBeingInvitedName", invite.userBeingInvitedName())
      );
      writer.println(
        str.format("groupInvite.status", invite.status())
      );
      writer.println(
        str.format("groupInvite.created", invite.timeStarted())
      );
      invite.timeCompleted().ifPresent(time -> {
        writer.println(
          str.format("groupInvite.completed", time)
        );
      });
      writer.println();
    }
    return SUCCESS;
  }

  protected static final class Parameters
    implements EIPSParameterHolderType
  {
    Parameters()
    {

    }
  }
}
