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

import com.io7m.eigion.amberjack.api.EIAClientException;
import com.io7m.eigion.amberjack.api.EIAClientType;
import com.io7m.eigion.amberjack.cmdline.EISExitException;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static com.io7m.eigion.amberjack.cmdline.internal.EISCommandResult.FAILURE;
import static com.io7m.eigion.amberjack.cmdline.internal.EISCommandResult.SUCCESS;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;

/**
 * The shell controller, responsible for dispatching commands.
 */

public final class EISController
{
  private final EISStrings strings;
  private final EIAClientType client;
  private volatile Map<String, EISCommandType> commands;
  private volatile Completer rootCompleter;

  private EISController(
    final EISStrings inStrings,
    final EIAClientType inClient)
  {
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.client =
      Objects.requireNonNull(inClient, "client");
  }

  /**
   * Create a shell controller.
   *
   * @param locale The locale for messages
   * @param client The client
   *
   * @return A shell controller
   */

  public static EISController create(
    final Locale locale,
    final EIAClientType client)
  {
    Objects.requireNonNull(locale, "locale");
    Objects.requireNonNull(client, "client");

    final EISStrings strings;
    try {
      strings = new EISStrings(locale);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }

    final var controller = new EISController(strings, client);
    final Map<String, EISCommandType> commandMap =
      Stream.of(
        new EISCommandAuditGetByTime(controller, strings),
        new EISCommandExit(controller, strings),
        new EISCommandHelp(controller, strings),
        new EISCommandLogin(controller, strings),
        new EISCommandServices(controller, strings),
        new EISCommandUserByEmail(controller, strings),
        new EISCommandUserById(controller, strings),
        new EISCommandUserByName(controller, strings),
        new EISCommandUserSearch(controller, strings),
        new EISCommandVersion(controller, strings)
      ).collect(toUnmodifiableMap(EISCommandType::name, identity()));

    final var completers = new ArrayList<Completer>();
    for (final var command : commandMap.values()) {
      final var argCompleters =
        new ArrayList<Completer>();
      final var cmdCompleters =
        command.argumentCompleters(commandMap.values());

      argCompleters.add(new StringsCompleter(command.name()));
      argCompleters.addAll(cmdCompleters);
      completers.add(new ArgumentCompleter(argCompleters));
    }

    final var rootCompleter = new AggregateCompleter(completers);
    controller.initialize(commandMap, rootCompleter);
    return controller;
  }

  /**
   * @return The underlying amberjack client
   */

  public EIAClientType client()
  {
    return this.client;
  }

  private void initialize(
    final Map<String, EISCommandType> inCommandMap,
    final AggregateCompleter inRootCompleter)
  {
    this.commands =
      Map.copyOf(Objects.requireNonNull(inCommandMap, "commandMap"));
    this.rootCompleter =
      Objects.requireNonNull(inRootCompleter, "rootCompleter");
  }

  /**
   * @return The shell completer
   */

  public Completer completer()
  {
    return this.rootCompleter;
  }

  /**
   * Execute a shell command.
   *
   * @param terminal The terminal
   * @param words    The words that make up the command
   *
   * @return The command result
   *
   * @throws EISExitException     On exit
   * @throws InterruptedException On interruption
   */

  public EISCommandResult execute(
    final Terminal terminal,
    final List<String> words)
    throws EISExitException, InterruptedException
  {
    Objects.requireNonNull(terminal, "terminal");
    Objects.requireNonNull(words, "words");

    if (words.isEmpty()) {
      return SUCCESS;
    }

    final var commandName =
      words.get(0);
    final var command =
      this.commands.get(commandName);

    if (command == null) {
      terminal.writer().println(
        this.strings.format("noSuchCommand", commandName)
      );
      return FAILURE;
    }

    try {
      if (words.size() > 1) {
        final var arguments = new ArrayList<String>();
        for (int index = 1; index < words.size(); ++index) {
          arguments.add(words.get(index));
        }
        return command.run(terminal, List.copyOf(arguments));
      }
      return command.run(terminal, List.of());
    } catch (final EIAClientException e) {
      terminal.writer()
        .println(this.strings.format("error", e.getMessage()));
      return FAILURE;
    }
  }

  /**
   * @return The available commands
   */

  public Map<String, EISCommandType> commands()
  {
    return this.commands;
  }
}
