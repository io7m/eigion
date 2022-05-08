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


package com.io7m.eigion.gui.internal;

import com.io7m.eigion.gui.EIGConfiguration;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * The main application class responsible for starting up the "main" view.
 */

public final class EIGApplication extends Application
{
  private final EIGConfiguration configuration;

  /**
   * The main application class responsible for starting up the "main" view.
   *
   * @param inConfiguration The UI configuration
   */

  public EIGApplication(
    final EIGConfiguration inConfiguration)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
  }

  @Override
  public void start(
    final Stage stage)
    throws Exception
  {
    final var mainXML =
      EIGApplication.class.getResource("/com/io7m/eigion/gui/internal/main.fxml");
    Objects.requireNonNull(mainXML, "mainXML");

    final var services =
      EIGServices.create(this.configuration);
    final var strings =
      services.requireService(EIGStrings.class);

    final var loader = new FXMLLoader(mainXML, strings.resources());
    loader.setControllerFactory(
      new EIGControllerFactory(services, this.configuration)
    );

    final AnchorPane pane = loader.load();
    this.configuration.customCSS()
      .ifPresent(customCSS -> {
        pane.getStylesheets().add(customCSS.toString());
      });

    stage.setTitle("Eigion");
    stage.setWidth(800.0);
    stage.setHeight(600.0);
    stage.setScene(new Scene(pane));
    stage.show();
  }
}
