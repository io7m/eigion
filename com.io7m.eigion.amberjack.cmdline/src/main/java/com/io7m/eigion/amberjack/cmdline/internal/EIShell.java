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

import com.io7m.eigion.amberjack.cmdline.EISExitException;
import com.io7m.eigion.amberjack.cmdline.EIShellConfiguration;
import com.io7m.eigion.amberjack.cmdline.EIShellType;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.Objects;

import static com.io7m.eigion.amberjack.cmdline.internal.EIControllerFlag.EXIT_ON_FAILED_COMMAND;
import static com.io7m.eigion.amberjack.cmdline.internal.EISCommandResult.FAILURE;

/**
 * A shell.
 */

public final class EIShell implements EIShellType
{
  private final DefaultParser parser;
  private final Terminal terminal;
  private final LineReader reader;
  private final EISController controller;

  private EIShell(
    final DefaultParser inParser,
    final Terminal inTerminal,
    final LineReader inReader,
    final EISController inCommands)
  {
    this.parser =
      Objects.requireNonNull(inParser, "parser");
    this.terminal =
      Objects.requireNonNull(inTerminal, "terminal");
    this.reader =
      Objects.requireNonNull(inReader, "reader");
    this.controller =
      Objects.requireNonNull(inCommands, "commands");
  }

  /**
   * Create a shell.
   *
   * @param configuration The configuration
   *
   * @return The shell
   *
   * @throws IOException On errors
   */

  public static EIShellType create(
    final EIShellConfiguration configuration)
    throws IOException
  {
    Objects.requireNonNull(configuration, "configuration");

    final var parser =
      new DefaultParser();

    final var terminalBuilder =
      TerminalBuilder.builder();

    terminalBuilder.system(true);
    terminalBuilder.color(true);

    final var terminal =
      terminalBuilder.build();

    final var commands =
      EISController.create(
        configuration.locale(),
        configuration.client()
      );

    final var reader =
      LineReaderBuilder.builder()
        .terminal(terminal)
        .completer(commands.completer())
        .parser(parser)
        .build();

    final var width0 =
      terminal.getWidth() == 0;
    final var height0 =
      terminal.getHeight() == 0;

    if (width0 || height0) {
      terminal.setSize(
        new Size(
          Math.max(terminal.getWidth(), 80),
          Math.max(terminal.getHeight(), 25)
        )
      );
    }

    return new EIShell(
      parser,
      terminal,
      reader,
      commands
    );
  }

  @Override
  public void run()
    throws EISExitException
  {
    while (true) {
      try {
        final var text = this.readLine();
        if (text.isBlank()) {
          continue;
        }
        if (text.startsWith("#")) {
          continue;
        }

        final var line =
          this.reader.getParsedLine();
        final var words =
          line.words();

        if (words.isEmpty()) {
          continue;
        }

        final var result =
          this.controller.execute(this.terminal, words);

        if (result == FAILURE
            && this.controller.isFlagSet(EXIT_ON_FAILED_COMMAND)) {
          throw new EISExitException(1);
        }
      } catch (final UserInterruptException e) {
        continue;
      } catch (final EndOfFileException e) {
        return;
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private String readLine()
  {
    return this.reader.readLine("> ").trim();
  }

  @Override
  public void close()
    throws IOException
  {
    this.terminal.close();
  }
}
