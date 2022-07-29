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

import com.io7m.eigion.model.EIAdmin;
import org.jline.terminal.Terminal;

import java.util.Objects;

/**
 * Functions over admins.
 */

public final class EISAdmins
{
  private EISAdmins()
  {

  }

  /**
   * Show the given admin on the terminal.
   *
   * @param s        The string resources
   * @param terminal The terminal output
   * @param u        The admin
   */

  public static void showAdmin(
    final EISStrings s,
    final Terminal terminal,
    final EIAdmin u)
  {
    Objects.requireNonNull(s, "strings");
    Objects.requireNonNull(terminal, "terminal");
    Objects.requireNonNull(u, "admin");

    final var w = terminal.writer();
    w.println(s.format("admin.show.id", u.id()));
    w.println(s.format("admin.show.name", u.name()));
    w.println(s.format("admin.show.email", u.email()));
    w.println(s.format("admin.show.created", u.created()));
    w.println(s.format("admin.show.lastLogin", u.lastLoginTime()));

    final var p = u.password();
    w.println(s.format(
      "admin.show.password.algorithm",
      p.algorithm().identifier()));
    w.println(s.format("admin.show.password.hash", p.hash()));
    w.println(s.format("admin.show.password.salt", p.salt()));
    w.println(s.format("admin.show.permissions", u.permissions()));
  }
}
