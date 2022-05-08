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

import com.io7m.eigion.gui.internal.EIGIcons;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import com.io7m.eigion.taskrecorder.EIFailed;
import com.io7m.eigion.taskrecorder.EIStep;
import com.io7m.eigion.taskrecorder.EIStepType;
import com.io7m.eigion.taskrecorder.EITask;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ResourceBundle;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A controller for a cell within an error tree view.
 */

public final class EIGErrorTreeCellController implements Initializable
{
  private final EIGIcons icons;

  @FXML private ImageView stepIcon;
  @FXML private Label stepTitle;
  @FXML private Label stepResolution;
  @FXML private TextArea stepException;

  /**
   * A controller for a cell within an error tree view.
   *
   * @param services The service directory
   */

  public EIGErrorTreeCellController(
    final EIServiceDirectoryType services)
  {
    this.icons =
      services.requireService(EIGIcons.class);
  }

  /**
   * Set the task step.
   *
   * @param item The task step
   */

  public void setItem(
    final EIStepType item)
  {
    if (item.resolution() instanceof EIFailed) {
      this.stepIcon.setImage(this.icons.error16());
    } else {
      this.stepIcon.setImage(this.icons.task16());
    }

    if (item instanceof EITask task) {
      this.stepResolution.setVisible(false);
      this.stepException.setVisible(false);
      this.stepTitle.setText(item.name());
      return;
    }

    if (item instanceof EIStep step) {
      this.stepResolution.setVisible(true);
      this.stepTitle.setText(item.name());
      this.stepResolution.setText(step.resolution().message());

      this.stepException.setVisible(false);
      if (step.resolution() instanceof EIFailed failed) {
        failed.exception().ifPresent(exception -> {
          final var bytes = new ByteArrayOutputStream();
          try (var stream = new PrintStream(bytes, true, UTF_8)) {
            exception.printStackTrace(stream);
          }
          this.stepException.setText(bytes.toString(UTF_8));
          this.stepException.setVisible(true);
        });
      }
      return;
    }
  }

  @Override
  public void initialize(
    final URL location,
    final ResourceBundle resources)
  {
    this.stepException.managedProperty()
      .bind(this.stepException.visibleProperty());
    this.stepResolution.managedProperty()
      .bind(this.stepResolution.visibleProperty());
  }
}
