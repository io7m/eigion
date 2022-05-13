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
import com.io7m.eigion.gui.internal.main.EIScreenControllerWithoutServicesType;
import com.io7m.eigion.taskrecorder.EIStep;
import com.io7m.eigion.taskrecorder.EIStepType;
import com.io7m.eigion.taskrecorder.EITask;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

import static com.io7m.eigion.icons.EIIconSemantic.ERROR_24;

/**
 * A controller for the error screen.
 */

public final class EIGErrorController
  implements EIScreenControllerWithoutServicesType
{
  private final EITask<?> task;
  private final Stage stage;
  private final EIGConfiguration configuration;
  private final EIGStrings strings;

  @FXML private ImageView errorIcon;
  @FXML private Label errorTaskTitle;
  @FXML private Label errorTaskMessage;
  @FXML private TreeView<EIStepType> errorDetails;

  /**
   * A controller for the error screen.
   *
   * @param inStrings       The strings
   * @param inConfiguration The application configuration
   * @param inTask          The failed task
   * @param inStage         The containing window
   */

  public EIGErrorController(
    final EIGConfiguration inConfiguration,
    final EIGStrings inStrings,
    final EITask<?> inTask,
    final Stage inStage)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "inConfiguration");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.task =
      Objects.requireNonNull(inTask, "task");
    this.stage =
      Objects.requireNonNull(inStage, "stage");
  }

  private static TreeItem<EIStepType> buildTree(
    final EIStepType node)
  {
    if (node instanceof EIStep step) {
      return new TreeItem<>(step);
    }

    if (node instanceof EITask<?> task) {
      final var taskNode = new TreeItem<EIStepType>(task);
      for (final var step : task.steps()) {
        taskNode.getChildren().add(buildTree(step));
      }
      return taskNode;
    }

    throw new IllegalStateException();
  }

  @Override
  public void initialize(
    final URL location,
    final ResourceBundle resources)
  {
    this.errorIcon.setImage(
      this.configuration.iconsConfiguration()
        .icon(ERROR_24)
    );

    this.errorTaskTitle.setText(this.task.name());
    this.errorTaskMessage.setText(this.task.resolution().message());

    this.errorDetails.setCellFactory(param -> {
      return new EIGErrorTreeCell(this.configuration, this.strings);
    });
    this.errorDetails.setRoot(buildTree(this.task));
    this.errorDetails.setShowRoot(false);
  }

  @FXML
  private void onDismissSelected()
  {
    this.stage.close();
  }

  @FXML
  private void onReportSelected()
  {

  }
}
