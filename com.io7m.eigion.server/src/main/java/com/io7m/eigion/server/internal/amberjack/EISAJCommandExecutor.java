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

package com.io7m.eigion.server.internal.amberjack;

import com.io7m.eigion.protocol.amberjack.EIAJCommandType;
import com.io7m.eigion.protocol.amberjack.EIAJResponseType;
import com.io7m.eigion.server.internal.command_exec.EISCommandExecutionFailure;
import com.io7m.eigion.server.internal.command_exec.EISCommandExecutorType;

import java.io.IOException;

/**
 * A command executor for Tickets commands.
 */

public final class EISAJCommandExecutor
  implements EISCommandExecutorType<
    EISAJCommandContext,
    EIAJCommandType<? extends EIAJResponseType>,
    EIAJResponseType>
{
  /**
   * A command executor for Tickets commands.
   */

  public EISAJCommandExecutor()
  {

  }

  @Override
  public EIAJResponseType execute(
    final EISAJCommandContext context,
    final EIAJCommandType<? extends EIAJResponseType> command)
    throws IOException, EISCommandExecutionFailure
  {
    final var span =
      context.tracer()
        .spanBuilder(command.getClass().getSimpleName())
        .startSpan();

    try (var ignored = span.makeCurrent()) {
      return this.executeCommand(context, command);
    } catch (final Throwable e) {
      span.recordException(e);
      throw e;
    } finally {
      span.end();
    }
  }

  private EIAJResponseType executeCommand(
    final EISAJCommandContext context,
    final EIAJCommandType<? extends EIAJResponseType> command)
    throws IOException
  {
    throw new IOException("Unimplemented!");
  }
}
