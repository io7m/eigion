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
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.terminal.Terminal;

import java.util.Collection;
import java.util.List;

/**
 * Exit the shell.
 */

public final class EISCommandExit
  extends EISAbstractCommand
{
  /**
   * Exit the shell.
   *
   * @param inController The controller
   * @param inStrings    The string resources
   */

  public EISCommandExit(
    final EISController inController,
    final EISStrings inStrings)
  {
    super(inController, inStrings, "exit");
  }

  @Override
  public List<Completer> argumentCompleters(
    final Collection<EISCommandType> values)
  {
    return List.of(new NullCompleter());
  }

  @Override
  public EISCommandResult run(
    final Terminal terminal,
    final List<String> arguments)
    throws EISExitException
  {
    throw new EISExitException();
  }
}
