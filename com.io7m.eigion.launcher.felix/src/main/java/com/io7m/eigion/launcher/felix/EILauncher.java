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

package com.io7m.eigion.launcher.felix;

import com.io7m.eigion.launcher.api.EILauncherConfiguration;
import com.io7m.eigion.launcher.api.EILauncherType;
import com.io7m.eigion.launcher.felix.internal.EIFelix;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Functions to obtain launchers.
 */

public final class EILauncher
{
  private EILauncher()
  {

  }

  /**
   * Create a launcher using the given configuration.
   *
   * @param configuration The configuration
   *
   * @return A launcher
   *
   */

  public static EILauncherType create(
    final EILauncherConfiguration configuration)
  {
    return EIFelix.create(
      Objects.requireNonNull(configuration, "configuration")
    );
  }

  /**
   * Create a launcher using the given configuration file.
   *
   * @param configurationFile The configuration file
   *
   * @return A launcher
   *
   * @throws Exception On errors
   */

  public static EILauncherType create(
    final Path configurationFile)
    throws Exception
  {
    return create(
      EILauncherConfiguration.parseFile(configurationFile)
    );
  }
}
