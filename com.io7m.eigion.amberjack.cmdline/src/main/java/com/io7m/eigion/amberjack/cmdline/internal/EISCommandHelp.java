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

import org.jline.reader.Completer;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;

import java.util.Collection;
import java.util.List;

import static com.io7m.eigion.amberjack.cmdline.internal.EISCommandResult.FAILURE;
import static com.io7m.eigion.amberjack.cmdline.internal.EISCommandResult.SUCCESS;

/**
 * Show help for a command.
 */

public final class EISCommandHelp
  extends EISAbstractCommand
{
  /**
   * Show help for a command.
   *
   * @param inController The controller
   * @param inStrings    The string resources
   */

  public EISCommandHelp(
    final EISController inController,
    final EISStrings inStrings)
  {
    super(inController, inStrings, "help");
  }

  @Override
  public List<Completer> argumentCompleters(
    final Collection<EISCommandType> values)
  {
    final var candidates =
      values.stream()
        .map(EISCommandType::name)
        .map(StringsCompleter::new)
        .map(c -> (Completer) c)
        .toList();

    return List.of(new AggregateCompleter(candidates));
  }

  @Override
  public EISCommandResult run(
    final Terminal terminal,
    final List<String> arguments)
  {
    if (arguments.isEmpty()) {
      terminal.writer().println(this.help());
      return SUCCESS;
    }

    final var commandName =
      arguments.get(0);
    final var command =
      this.commands().get(commandName);

    if (command == null) {
      terminal.writer()
        .println(this.strings().format("noSuchCommand", commandName));
      return FAILURE;
    }

    terminal.writer().println(command.help());
    return SUCCESS;
  }
}
