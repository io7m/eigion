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

import com.io7m.eigion.pike.api.EIPClientException;
import com.io7m.eigion.pike.api.EIPClientType;
import com.io7m.eigion.pike.cmdline.EIPSExitException;
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

import static com.io7m.eigion.pike.cmdline.internal.EIPSCommandResult.FAILURE;
import static com.io7m.eigion.pike.cmdline.internal.EIPSCommandResult.SUCCESS;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;

/**
 * The shell controller, responsible for dispatching commands.
 */

public final class EIPSController
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIPSController.class);

  private final EIPStrings strings;
  private final EIPClientType client;
  private final EnumSet<EIPSControllerFlag> flags;
  private volatile Map<String, EIPSCommandType> commands;
  private volatile Completer rootCompleter;

  private EIPSController(
    final EIPStrings inStrings,
    final EIPClientType inClient)
  {
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.client =
      Objects.requireNonNull(inClient, "client");
    this.flags =
      EnumSet.noneOf(EIPSControllerFlag.class);
  }

  /**
   * Create a shell controller.
   *
   * @param locale The locale for messages
   * @param client The client
   *
   * @return A shell controller
   */

  public static EIPSController create(
    final Locale locale,
    final EIPClientType client)
  {
    Objects.requireNonNull(locale, "locale");
    Objects.requireNonNull(client, "client");

    final EIPStrings strings;
    try {
      strings = new EIPStrings(locale);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }

    final var controller = new EIPSController(strings, client);
    final Map<String, EIPSCommandType> commandMap =
      Stream.of(
        new EIPSCommandExit(controller, strings),
        new EIPSCommandGroupCreationBegin(controller, strings),
        new EIPSCommandGroupInvitesSent(controller, strings),
        new EIPSCommandGroupInvitesReceived(controller, strings),
        new EIPSCommandGroupCreationCancel(controller, strings),
        new EIPSCommandGroupCreationReady(controller, strings),
        new EIPSCommandGroupCreationRequests(controller, strings),
        new EIPSCommandGroupInvite(controller, strings),
        new EIPSCommandGroups(controller, strings),
        new EIPSCommandHelp(controller, strings),
        new EIPSCommandLogin(controller, strings),
        new EIPSCommandSet(controller, strings),
        new EIPSCommandVersion(controller, strings)
      ).collect(toUnmodifiableMap(EIPSCommandType::name, identity()));

    final var completers = new ArrayList<Completer>();
    for (final var command : commandMap.values()) {
      completers.add(new StringsCompleter(command.name()));
    }

    final var rootCompleter = new AggregateCompleter(completers);
    controller.initialize(commandMap, rootCompleter);
    return controller;
  }

  /**
   * @return The underlying pike client
   */

  public EIPClientType client()
  {
    return this.client;
  }

  private void initialize(
    final Map<String, EIPSCommandType> inCommandMap,
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
   * @throws EIPSExitException     On exit
   * @throws InterruptedException On interruption
   */

  public EIPSCommandResult execute(
    final Terminal terminal,
    final List<String> words)
    throws EIPSExitException, InterruptedException
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
    } catch (final EIPClientException e) {
      LOG.error("{}", e.getMessage());
      return FAILURE;
    }
  }

  /**
   * @return The available commands
   */

  public Map<String, EIPSCommandType> commands()
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
    final EIPSControllerFlag flag,
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
    final EIPSControllerFlag flag)
  {
    return this.flags.contains(flag);
  }
}
