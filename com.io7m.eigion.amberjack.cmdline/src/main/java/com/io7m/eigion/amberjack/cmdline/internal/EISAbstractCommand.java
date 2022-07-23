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

import java.util.Map;
import java.util.Objects;

/**
 * An abstract base class for commands.
 */

public abstract class EISAbstractCommand
  implements EISCommandType
{
  private final String name;
  private final EISController controller;
  private final EISStrings strings;

  /**
   * An abstract base class for commands.
   *
   * @param inStrings    The string resources
   * @param inController The controller
   * @param inName       The command name
   */

  public EISAbstractCommand(
    final EISController inController,
    final EISStrings inStrings,
    final String inName)
  {
    this.controller =
      Objects.requireNonNull(inController, "commands");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.name =
      Objects.requireNonNull(inName, "name");
  }

  /**
   * @return The controller commands
   */

  protected final Map<String, EISCommandType> commands()
  {
    return this.controller.commands();
  }

  /**
   * @return The string resources
   */

  protected final EISStrings strings()
  {
    return this.strings;
  }

  @Override
  public final String help()
  {
    return this.strings.format("%s.help".formatted(this.name));
  }

  @Override
  public final String name()
  {
    return this.name;
  }

  /**
   * @return The controller
   */

  protected final EISController controller()
  {
    return this.controller;
  }
}
