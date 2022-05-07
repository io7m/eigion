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


package com.io7m.eigion.launcher.felix.internal;

import com.io7m.eigion.launcher.api.EILauncherConfiguration;
import com.io7m.eigion.launcher.api.EILauncherType;
import org.apache.felix.atomos.Atomos;
import org.apache.felix.atomos.AtomosLayer;
import org.osgi.framework.launch.Framework;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.felix.atomos.Atomos.ATOMOS_CONTENT_INSTALL;
import static org.apache.felix.atomos.Atomos.ATOMOS_CONTENT_START;
import static org.osgi.framework.Constants.FRAMEWORK_STORAGE;

/**
 * A launcher using Felix.
 */

public final class EIFelix implements EILauncherType
{
  private final EILauncherConfiguration configuration;
  private final AtomicBoolean started;
  private Framework framework;

  private EIFelix(
    final EILauncherConfiguration inConfiguration)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");

    this.started = new AtomicBoolean(false);
  }

  /**
   * Create a launcher.
   *
   * @param configuration The configuration
   *
   * @return A launcher
   */

  public static EILauncherType create(
    final EILauncherConfiguration configuration)
  {
    return new EIFelix(configuration);
  }

  @Override
  public void run()
    throws Exception
  {
    if (this.started.compareAndSet(false, true)) {
      final var configMap = new HashMap<String, String>();
      configMap.put("org.osgi.framework.system.packages", "");
      configMap.put(
        FRAMEWORK_STORAGE,
        this.configuration.runtimeDirectory()
          .toAbsolutePath()
          .toString()
      );
      configMap.put(ATOMOS_CONTENT_INSTALL, "false");
      configMap.put(ATOMOS_CONTENT_START, "false");

      final var atomos =
        Atomos.newAtomos(configMap);

      final var bootLayer =
        atomos.getBootLayer();
      final var javaModules =
        this.configuration.javaModules();

      this.framework = atomos.newFramework(configMap);
      this.framework.init();

      for (final var module : javaModules) {
        final var name =
          module.getFileName().toString();
        final var directory =
          module.getParent();

        final var layer =
          atomos.addLayer(
            List.of(bootLayer),
            name,
            AtomosLayer.LoaderType.SINGLE,
            directory
          );

        for (final var content : layer.getAtomosContents()) {
          content.install();
        }
      }

      this.framework.start();
    }
  }

  @Override
  public EILauncherConfiguration configuration()
  {
    return this.configuration;
  }

  @Override
  public void close()
    throws Exception
  {
    if (this.started.compareAndSet(true, false)) {
      this.framework.stop();
    }
  }
}
