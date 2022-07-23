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
import com.io7m.eigion.model.EIUser;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.terminal.Terminal;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.io7m.eigion.amberjack.cmdline.internal.EISCommandResult.FAILURE;
import static com.io7m.eigion.amberjack.cmdline.internal.EISCommandResult.SUCCESS;
import static com.io7m.eigion.amberjack.cmdline.internal.EISUsers.showUser;

/**
 * Find a user by their email address.
 */

public final class EISCommandUserByEmail
  extends EISAbstractCommand
{
  /**
   * Find a user by their email address.
   *
   * @param inController The controller
   * @param inStrings    The string resources
   */

  public EISCommandUserByEmail(
    final EISController inController,
    final EISStrings inStrings)
  {
    super(inController, inStrings, "user-by-email");
  }

  @Override
  public EISCommandResult run(
    final Terminal terminal,
    final List<String> arguments)
    throws EIAClientException, InterruptedException
  {
    final var writer = terminal.writer();
    if (arguments.isEmpty()) {
      writer.println(this.strings().format("user-by-email.missingEmail"));
      return FAILURE;
    }

    final var email = arguments.get(0);
    final Optional<EIUser> userOpt =
      this.controller()
        .client()
        .userByEmail(email);

    userOpt.ifPresent(user -> showUser(this.strings(), terminal, user));
    return SUCCESS;
  }

  @Override
  public List<Completer> argumentCompleters(
    final Collection<EISCommandType> values)
  {
    return List.of(new NullCompleter());
  }
}
