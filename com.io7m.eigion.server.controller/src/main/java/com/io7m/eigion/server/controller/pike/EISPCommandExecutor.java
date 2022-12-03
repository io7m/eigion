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

package com.io7m.eigion.server.controller.pike;

import com.io7m.eigion.protocol.pike.EIPCommandGroupCreateBegin;
import com.io7m.eigion.protocol.pike.EIPCommandGroupCreateCancel;
import com.io7m.eigion.protocol.pike.EIPCommandGroupCreateReady;
import com.io7m.eigion.protocol.pike.EIPCommandGroupCreateRequestsBegin;
import com.io7m.eigion.protocol.pike.EIPCommandGroupCreateRequestsNext;
import com.io7m.eigion.protocol.pike.EIPCommandGroupCreateRequestsPrevious;
import com.io7m.eigion.protocol.pike.EIPCommandGroupsBegin;
import com.io7m.eigion.protocol.pike.EIPCommandGroupsNext;
import com.io7m.eigion.protocol.pike.EIPCommandGroupsPrevious;
import com.io7m.eigion.protocol.pike.EIPCommandType;
import com.io7m.eigion.protocol.pike.EIPResponseType;
import com.io7m.eigion.server.controller.command_exec.EISCommandExecutionFailure;
import com.io7m.eigion.server.controller.command_exec.EISCommandExecutorType;

import java.io.IOException;

/**
 * A command executor for Tickets commands.
 */

public final class EISPCommandExecutor
  implements EISCommandExecutorType<
    EISPCommandContext,
    EIPCommandType<? extends EIPResponseType>,
      EIPResponseType>
{
  /**
   * A command executor for Tickets commands.
   */

  public EISPCommandExecutor()
  {

  }

  @Override
  public EIPResponseType execute(
    final EISPCommandContext context,
    final EIPCommandType<? extends EIPResponseType> command)
    throws IOException, EISCommandExecutionFailure, InterruptedException
  {
    final var span =
      context.tracer()
        .spanBuilder(command.getClass().getSimpleName())
        .startSpan();

    try (var ignored = span.makeCurrent()) {
      return executeCommand(context, command);
    } catch (final Throwable e) {
      span.recordException(e);
      throw e;
    } finally {
      span.end();
    }
  }

  private static EIPResponseType executeCommand(
    final EISPCommandContext context,
    final EIPCommandType<? extends EIPResponseType> command)
    throws IOException, EISCommandExecutionFailure, InterruptedException
  {
    if (command instanceof EIPCommandGroupCreateBegin c) {
      return new EISPCmdGroupCreateBegin().execute(context, c);
    }
    if (command instanceof EIPCommandGroupCreateReady c) {
      return new EISPCmdGroupCreateReady().execute(context, c);
    }
    if (command instanceof EIPCommandGroupCreateCancel c) {
      return new EISPCmdGroupCreateCancel().execute(context, c);
    }
    if (command instanceof EIPCommandGroupsBegin c) {
      return new EISPCmdGroupsBegin().execute(context, c);
    }
    if (command instanceof EIPCommandGroupsNext c) {
      return new EISPCmdGroupsNext().execute(context, c);
    }
    if (command instanceof EIPCommandGroupsPrevious c) {
      return new EISPCmdGroupsPrevious().execute(context, c);
    }
    if (command instanceof EIPCommandGroupCreateRequestsBegin c) {
      return new EISPCmdGroupCreateRequestsBegin().execute(context, c);
    }
    if (command instanceof EIPCommandGroupCreateRequestsNext c) {
      return new EISPCmdGroupCreateRequestsNext().execute(context, c);
    }
    if (command instanceof EIPCommandGroupCreateRequestsPrevious c) {
      return new EISPCmdGroupCreateRequestsPrevious().execute(context, c);
    }

    throw new IllegalStateException(
      "Unrecognized command: %s".formatted(command.getClass())
    );
  }
}
