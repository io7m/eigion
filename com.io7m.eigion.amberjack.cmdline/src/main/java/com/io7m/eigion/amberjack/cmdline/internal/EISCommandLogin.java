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
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.terminal.Terminal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;

import static com.io7m.eigion.amberjack.cmdline.internal.EISCommandResult.FAILURE;
import static com.io7m.eigion.amberjack.cmdline.internal.EISCommandResult.SUCCESS;

/**
 * Log in!
 */

public final class EISCommandLogin
  extends EISAbstractCommand
{
  /**
   * Log in!
   *
   * @param inController The controller
   * @param inStrings    The string resources
   */

  public EISCommandLogin(
    final EISController inController,
    final EISStrings inStrings)
  {
    super(inController, inStrings, "login");
  }

  @Override
  public EISCommandResult run(
    final Terminal terminal,
    final List<String> arguments)
    throws EIAClientException, InterruptedException
  {
    final var writer = terminal.writer();
    if (arguments.size() < 3) {
      writer.println(this.strings().format("login.missingParameters"));
      return FAILURE;
    }

    final var username = arguments.get(0);
    final var password = arguments.get(1);
    final var uri = arguments.get(2);

    try {
      this.controller()
        .client()
        .login(username, password, new URI(uri));
    } catch (final URISyntaxException e) {
      writer.write(this.strings().format("error", e.getMessage()));
      return FAILURE;
    }

    return SUCCESS;
  }

  @Override
  public List<Completer> argumentCompleters(
    final Collection<EISCommandType> values)
  {
    return List.of(new NullCompleter());
  }
}
