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
import com.io7m.eigion.amberjack.cmdline.EISExitException;
import org.jline.reader.Completer;
import org.jline.terminal.Terminal;

import java.util.Collection;
import java.util.List;

/**
 * The interface exposed by commands.
 */

public interface EISCommandType
{
  /**
   * @return The name of the command
   */

  String name();

  /**
   * @param commands The available commands
   *
   * @return A completer for the command
   */

  List<Completer> argumentCompleters(
    Collection<EISCommandType> commands);

  /**
   * @return The help text for the command
   */

  String help();

  /**
   * Execute the command.
   *
   * @param terminal  The terminal
   * @param arguments The arguments (not including the command name)
   *
   * @return The result
   *
   * @throws EISExitException     On exit
   * @throws EIAClientException   On client errors
   * @throws InterruptedException On interruption
   */

  EISCommandResult run(
    Terminal terminal,
    List<String> arguments)
    throws EISExitException, EIAClientException, InterruptedException;
}
