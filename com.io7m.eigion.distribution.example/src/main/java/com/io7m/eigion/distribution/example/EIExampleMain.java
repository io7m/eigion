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


package com.io7m.eigion.distribution.example;

import com.io7m.eigion.gui.EIGConfiguration;
import com.io7m.eigion.gui.EIGIconsConfiguration;
import com.io7m.eigion.gui.EIGLogoScreenConfiguration;
import com.io7m.eigion.gui.EIGServerConfiguration;
import com.io7m.eigion.gui.EIGUI;
import com.io7m.jade.api.ApplicationDirectories;
import com.io7m.jade.api.ApplicationDirectoryConfiguration;
import com.io7m.jtensors.core.unparameterized.vectors.Vector3D;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

/**
 * An example application.
 */

public final class EIExampleMain
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIExampleMain.class);

  private EIExampleMain()
  {

  }

  /**
   * The main entry point.
   *
   * @param args The command-line arguments
   *
   * @throws Exception On errors
   */

  public static void main(
    final String[] args)
    throws Exception
  {
    final var directoryConfiguration =
      ApplicationDirectoryConfiguration.builder()
        .setApplicationName("com.io7m.eigion")
        .setPortablePropertyName("com.io7m.eigion.portable")
        .build();

    final var directories =
      ApplicationDirectories.get(directoryConfiguration);

    final var logoConfiguration =
      new EIGLogoScreenConfiguration(
        EIExampleMain.class.getResource(
            "/com/io7m/eigion/distribution/example/io7m.png")
          .toURI(),
        Duration.ofSeconds(3L),
        Vector3D.of(1.0, 1.0, 1.0),
        300.0,
        100.0
      );

    final var serverConfiguration =
      new EIGServerConfiguration(URI.create("http://localhost:40000"), true);

    final var configuration =
      new EIGConfiguration(
        directories,
        Locale.getDefault(),
        Optional.of(logoConfiguration),
        serverConfiguration,
        EIGIconsConfiguration.defaults()
      );

    Platform.startup(() -> {
      try {
        EIGUI.start(configuration);
      } catch (final Exception e) {
        LOG.error("failed startup: ", e);
        Platform.exit();
      }
    });
  }
}
