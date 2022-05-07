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


package com.io7m.eigion.gui.internal.errors;

import com.io7m.eigion.gui.EIGConfiguration;
import com.io7m.eigion.gui.internal.EIGStrings;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import com.io7m.eigion.services.api.EIServiceType;
import com.io7m.eigion.taskrecorder.EITask;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

import static javafx.stage.Modality.APPLICATION_MODAL;

/**
 * A service for creating error dialogs.
 */

public final class EIGErrorDialogs implements EIServiceType
{
  private final EIGStrings strings;
  private final EIServiceDirectoryType services;
  private final EIGConfiguration configuration;

  /**
   * A service for creating error dialogs.
   *
   * @param inServices      The service directory
   * @param inConfiguration The configuration
   */

  public EIGErrorDialogs(
    final EIServiceDirectoryType inServices,
    final EIGConfiguration inConfiguration)
  {
    this.services =
      Objects.requireNonNull(inServices, "services");
    this.strings =
      inServices.requireService(EIGStrings.class);
    this.configuration =
      Objects.requireNonNull(inConfiguration, "inConfiguration");
  }

  /**
   * Open an error dialog assuming that the given task failed.
   *
   * @param task The failed task
   */

  public void open(
    final EITask<?> task)
  {
    try {
      final var stage = new Stage();

      final var layout =
        EIGErrorDialogs.class.getResource(
          "/com/io7m/eigion/gui/internal/error.fxml");

      Objects.requireNonNull(layout, "layout");

      final var loader =
        new FXMLLoader(layout, this.strings.resources());

      loader.setControllerFactory(param -> {
        return new EIGErrorController(
          this.services,
          task,
          stage);
      });

      final Pane pane = loader.load();
      stage.initModality(APPLICATION_MODAL);
      stage.setTitle("Eigion");
      stage.setWidth(640.0);
      stage.setHeight(480.0);
      stage.setScene(new Scene(pane));
      stage.show();
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public String toString()
  {
    return String.format(
      "[EIGErrorDialogs 0x%08x]",
      Integer.valueOf(this.hashCode())
    );
  }
}
