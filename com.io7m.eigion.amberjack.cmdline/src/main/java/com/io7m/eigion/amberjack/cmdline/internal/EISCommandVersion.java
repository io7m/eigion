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
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.terminal.Terminal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

/**
 * Show the application version.
 */

public final class EISCommandVersion
  extends EISAbstractCommand
{
  private static final String VERSION_TXT =
    "/com/io7m/eigion/amberjack/cmdline/internal/version.txt";

  /**
   * Show the application version.
   *
   * @param inController The controller
   * @param inStrings    The string resources
   */

  public EISCommandVersion(
    final EISController inController,
    final EISStrings inStrings)
  {
    super(inController, inStrings, "version");
  }

  @Override
  public EISCommandResult run(
    final Terminal terminal,
    final List<String> arguments)
  {
    try {
      final var c = EISCommandVersion.class;
      try (var stream = c.getResourceAsStream(VERSION_TXT)) {
        final var text =
          new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        terminal.writer().println(text);
      }
    } catch (final IOException e) {
      terminal.writer().println("error: " + e);
    }

    return EISCommandResult.SUCCESS;
  }

  @Override
  public List<Completer> argumentCompleters(
    final Collection<EISCommandType> values)
  {
    return List.of(new NullCompleter());
  }
}
