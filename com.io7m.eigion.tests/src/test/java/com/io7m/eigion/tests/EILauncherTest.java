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

package com.io7m.eigion.tests;

import com.io7m.eigion.launcher.api.EILauncherConfiguration;
import com.io7m.eigion.launcher.felix.EILauncher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class EILauncherTest
{
  private Path directory;

  @BeforeEach
  public void setup()
    throws IOException
  {
    this.directory = EITestDirectories.createTempDirectory();
  }

  @AfterEach
  public void tearDown()
    throws IOException
  {
    EITestDirectories.deleteDirectory(this.directory);
  }

  private Path serializeConfig(
    final EILauncherConfiguration config)
    throws IOException
  {
    final var file = this.directory.resolve("config.properties");
    try (var output = Files.newOutputStream(
      file,
      CREATE,
      WRITE,
      TRUNCATE_EXISTING)) {
      config.toProperties().store(output, "");
    }
    return file;
  }

  /**
   * Missing configuration files fail.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLauncherMissingConfigurationFile()
    throws Exception
  {
    assertThrows(NoSuchFileException.class, () -> {
      try (var ignored =
             EILauncher.create(this.directory.resolve("nonexistent.conf"))) {

      }
    });
  }

  /**
   * Simple configuration files work.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLauncherSimple0()
    throws Exception
  {
    final var runtimeDirectory =
      this.directory.resolve("runtime");
    final var module0 =
      this.resourceOf("com.io7m.jaffirm.core-4.0.0.jar");

    final var config =
      EILauncherConfiguration.builder(runtimeDirectory)
        .addJavaModule(module0)
        .build();

    final var file =
      this.serializeConfig(config);

    try (var launcher = EILauncher.create(file)) {

    }
  }

  /**
   * An empty framework can start up.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLauncherStartsEmpty()
    throws Exception
  {
    final var runtimeDirectory =
      this.directory.resolve("runtime");

    final var config =
      EILauncherConfiguration.builder(runtimeDirectory)
        .build();

    final var file =
      this.serializeConfig(config);

    try (var launcher = EILauncher.create(file)) {
      launcher.run();
    }
  }

  /**
   * An empty framework can start up.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLauncherStartsSimple()
    throws Exception
  {
    final var runtimeDirectory =
      this.directory.resolve("runtime");
    final var module0 =
      this.resourceOf("com.io7m.jaffirm.core-4.0.0.jar");

    final var config =
      EILauncherConfiguration.builder(runtimeDirectory)
        .addJavaModule(module0)
        .build();

    final var file =
      this.serializeConfig(config);

    try (var launcher = EILauncher.create(file)) {
      launcher.run();
    }
  }

  private Path resourceOf(
    final String name)
    throws IOException
  {
    return EITestDirectories.resourceOf(
      EILauncherTest.class,
      this.directory,
      name
    );
  }
}
