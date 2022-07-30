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

import com.beust.jcommander.Parameter;
import org.jline.terminal.Terminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.io7m.eigion.pike.cmdline.internal.EIPSCommandResult.FAILURE;
import static com.io7m.eigion.pike.cmdline.internal.EIPSCommandResult.SUCCESS;

/**
 * Show help for a command.
 */

public final class EIPSCommandHelp
  extends EIPSAbstractCommand<EIPSCommandHelp.Parameters>
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIPSCommandHelp.class);

  /**
   * Show help for a command.
   *
   * @param inController The controller
   * @param inStrings    The string resources
   */

  public EIPSCommandHelp(
    final EIPSController inController,
    final EIPStrings inStrings)
  {
    super(inController, inStrings, "help");
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
  {
    final var writer = terminal.writer();

    if (parameters.commandNames.isEmpty()) {
      writer.println(this.help());
      return SUCCESS;
    }

    final var commandName =
      parameters.commandNames.get(0);
    final var command =
      this.commands().get(commandName);

    if (command == null) {
      final var s = this.strings();
      LOG.error("{}", s.format("noSuchCommand", commandName));
      writer.println(s.format("helpCommands", this.listCommands()));
      return FAILURE;
    }

    writer.println(command.help());
    return SUCCESS;
  }

  private String listCommands()
  {
    return this.commands().keySet()
      .stream()
      .sorted()
      .collect(Collectors.joining(", ", "{ ", " }"));
  }

  protected static final class Parameters
    implements EIPSParameterHolderType
  {
    @Parameter
    private List<String> commandNames = new ArrayList<>();

    Parameters()
    {

    }
  }
}
