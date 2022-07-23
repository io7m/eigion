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

import com.io7m.eigion.amberjack.api.EIAClientType;
import com.io7m.eigion.amberjack.cmdline.EISExitException;
import com.io7m.eigion.amberjack.cmdline.EIShellType;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

/**
 * A shell.
 */

public final class EIShell implements EIShellType
{
  private final DefaultParser parser;
  private final Terminal terminal;
  private final LineReader reader;
  private final EISController commands;

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
    this.commands =
      Objects.requireNonNull(inCommands, "commands");
  }

  /**
   * Create a shell.
   *
   * @param locale The locale for error messages
   * @param client The client
   *
   * @return The shell
   *
   * @throws IOException On errors
   */

  public static EIShell create(
    final Locale locale,
    final EIAClientType client)
    throws IOException
  {
    Objects.requireNonNull(locale, "locale");
    Objects.requireNonNull(client, "client");

    final var parser =
      new DefaultParser();

    final var terminal =
      TerminalBuilder.builder()
        .system(true)
        .color(true)
        .build();

    final var commands =
      EISController.create(locale, client);

    final var reader =
      LineReaderBuilder.builder()
        .terminal(terminal)
        .completer(commands.completer())
        .parser(parser)
        .build();

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
        final var text =
          this.reader.readLine("amberjack# ").trim();

        if (text.isBlank()) {
          continue;
        }

        final var line =
          this.reader.getParsedLine();
        final var words =
          line.words();

        if (words.isEmpty()) {
          continue;
        }

        this.commands.execute(this.terminal, words);
      } catch (final UserInterruptException e) {
        continue;
      } catch (final EndOfFileException e) {
        return;
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public void close()
    throws IOException
  {
    this.terminal.close();
  }
}
