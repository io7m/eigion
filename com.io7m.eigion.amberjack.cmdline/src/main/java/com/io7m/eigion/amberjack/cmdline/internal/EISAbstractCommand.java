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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.io7m.eigion.amberjack.api.EIAClientException;
import com.io7m.eigion.amberjack.cmdline.EIAExitException;
import org.jline.terminal.Terminal;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.io7m.eigion.amberjack.cmdline.internal.EISCommandResult.FAILURE;

/**
 * An abstract base class for commands.
 *
 * @param <T> The type of command-line parameters
 */

public abstract class EISAbstractCommand<T extends EISParameterHolderType>
  implements EISCommandType
{
  private final String name;
  private final EISController controller;
  private final EISStrings strings;

  /**
   * An abstract base class for commands.
   *
   * @param inStrings    The string resources
   * @param inController The controller
   * @param inName       The command name
   */

  public EISAbstractCommand(
    final EISController inController,
    final EISStrings inStrings,
    final String inName)
  {
    this.controller =
      Objects.requireNonNull(inController, "commands");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.name =
      Objects.requireNonNull(inName, "name");
  }

  /**
   * @return The controller commands
   */

  protected final Map<String, EISCommandType> commands()
  {
    return this.controller.commands();
  }

  /**
   * @return The string resources
   */

  protected final EISStrings strings()
  {
    return this.strings;
  }

  @Override
  public final String name()
  {
    return this.name;
  }

  @Override
  public final String help()
  {
    final var parameters =
      this.createEmptyParameters();

    final var commander =
      JCommander.newBuilder()
        .programName(this.name)
        .addObject(parameters)
        .build();

    final var text = new StringBuilder(128);
    commander.getUsageFormatter().usage(text);
    return text.toString();
  }

  /**
   * @return The controller
   */

  protected final EISController controller()
  {
    return this.controller;
  }

  protected abstract T createEmptyParameters();

  protected abstract EISCommandResult runActual(
    Terminal terminal,
    T parameters)
    throws EIAExitException, EIAClientException, InterruptedException;

  @Override
  public final EISCommandResult run(
    final Terminal terminal,
    final List<String> arguments)
    throws EIAExitException, EIAClientException, InterruptedException
  {
    final var parameters =
      this.createEmptyParameters();

    final var commander =
      JCommander.newBuilder()
        .programName(this.name)
        .addObject(parameters)
        .build();

    final var writer = terminal.writer();
    final var args = arguments.toArray(new String[0]);
    try {
      commander.parse(args);
    } catch (final ParameterException e) {
      writer.println(this.help());
      return FAILURE;
    }
    return this.runActual(terminal, parameters);
  }
}
