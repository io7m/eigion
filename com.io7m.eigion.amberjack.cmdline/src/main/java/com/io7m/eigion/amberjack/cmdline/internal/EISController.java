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
import com.io7m.eigion.amberjack.cmdline.EIAExitException;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.EnumSet;
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
  private static final Logger LOG =
    LoggerFactory.getLogger(EISController.class);

  private final EISStrings strings;
  private final EIAClientType client;
  private final EnumSet<EIControllerFlag> flags;
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
    this.flags =
      EnumSet.noneOf(EIControllerFlag.class);
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
        new EISCommandAdminCreate(controller, strings),
        new EISCommandAdminGet(controller, strings),
        new EISCommandAdminSearch(controller, strings),
        new EISCommandAudit(controller, strings),
        new EISCommandExit(controller, strings),
        new EISCommandGroupInvites(controller, strings),
        new EISCommandHelp(controller, strings),
        new EISCommandLogin(controller, strings),
        new EISCommandServices(controller, strings),
        new EISCommandSet(controller, strings),
        new EISCommandUserCreate(controller, strings),
        new EISCommandUserGet(controller, strings),
        new EISCommandUserSearch(controller, strings),
        new EISCommandVersion(controller, strings)
      ).collect(toUnmodifiableMap(EISCommandType::name, identity()));

    final var completers = new ArrayList<Completer>();
    for (final var command : commandMap.values()) {
      completers.add(new StringsCompleter(command.name()));
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
   * @throws EIAExitException     On exit
   * @throws InterruptedException On interruption
   */

  public EISCommandResult execute(
    final Terminal terminal,
    final List<String> words)
    throws EIAExitException, InterruptedException
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
      LOG.error("{}", this.strings.format("noSuchCommand", commandName));
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
      LOG.error("{}", e.getMessage());
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

  /**
   * Enable or disable the given flag.
   *
   * @param flag    The flag
   * @param enabled {@code true} if the flag should be enabled
   */

  public void setFlag(
    final EIControllerFlag flag,
    final boolean enabled)
  {
    if (enabled) {
      this.flags.add(flag);
    } else {
      this.flags.remove(flag);
    }
  }

  /**
   * @param flag The flag
   *
   * @return {@code true} if the given flag is enabled
   */

  public boolean isFlagSet(
    final EIControllerFlag flag)
  {
    return this.flags.contains(flag);
  }
}
