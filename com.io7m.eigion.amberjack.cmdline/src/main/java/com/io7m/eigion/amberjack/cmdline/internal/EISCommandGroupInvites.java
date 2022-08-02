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

package com.io7m.eigion.amberjack.cmdline.internal;

import com.beust.jcommander.Parameter;
import com.io7m.eigion.amberjack.api.EIAClientException;
import com.io7m.eigion.amberjack.cmdline.EIAExitException;
import com.io7m.eigion.model.EIGroupInviteStatus;
import com.io7m.eigion.model.EIGroupName;
import org.jline.terminal.Terminal;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.eigion.amberjack.cmdline.internal.EISCommandResult.SUCCESS;

/**
 * List group invites.
 */

public final class EISCommandGroupInvites
  extends EISAbstractCommand<EISCommandGroupInvites.Parameters>
{
  /**
   * List group invites.
   *
   * @param inController The controller
   * @param inStrings    The string resources
   */

  public EISCommandGroupInvites(
    final EISController inController,
    final EISStrings inStrings)
  {
    super(inController, inStrings, "group-invites");
  }

  @Override
  protected Parameters createEmptyParameters()
  {
    return new Parameters();
  }

  @Override
  protected EISCommandResult runActual(
    final Terminal terminal,
    final Parameters parameters)
    throws EIAExitException, EIAClientException, InterruptedException
  {
    final var invites =
      this.controller()
        .client()
        .groupInvites(
          parameters.since,
          Optional.ofNullable(parameters.groupName).map(EIGroupName::new),
          Optional.ofNullable(parameters.userInviter),
          Optional.ofNullable(parameters.userBeingInvited),
          Optional.ofNullable(parameters.status)
        );

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
        str.format(
          "groupInvite.userBeingInvitedName",
          invite.userBeingInvitedName())
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
    implements EISParameterHolderType
  {
    @Parameter(
      description = "Only list invites created since this time.",
      required = true,
      converter = EISOffsetDateTimeConverter.class,
      names = "--since")
    private OffsetDateTime since;

    @Parameter(
      description = "Only list invites for this group.",
      required = false,
      names = "--group")
    private String groupName;

    @Parameter(
      description = "Only list invites created by this user.",
      required = false,
      names = "--user-inviter")
    private UUID userInviter;

    @Parameter(
      description = "Only list invites targeted at this user.",
      required = false,
      names = "--user-being-invited")
    private UUID userBeingInvited;

    @Parameter(
      description = "Only list invites targeted at this user.",
      required = false,
      names = "--status")
    private EIGroupInviteStatus status;

    Parameters()
    {

    }
  }
}
