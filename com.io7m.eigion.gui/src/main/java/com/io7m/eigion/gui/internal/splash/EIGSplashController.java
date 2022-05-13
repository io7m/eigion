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


package com.io7m.eigion.gui.internal.splash;

import com.io7m.eigion.gui.EIGConfiguration;
import com.io7m.eigion.gui.internal.EIGBackgroundSchedulerService;
import com.io7m.eigion.gui.internal.EIGStrings;
import com.io7m.eigion.gui.internal.errors.EIGErrorDialogs;
import com.io7m.eigion.gui.internal.main.EIGMainScreenController;
import com.io7m.eigion.gui.internal.main.EIScreenControllerWithoutServicesType;
import com.io7m.eigion.gui.internal.services.EIBootEvent;
import com.io7m.eigion.gui.internal.services.EIGBootServices;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import com.io7m.eigion.taskrecorder.EIFailed;
import com.io7m.eigion.taskrecorder.EITask;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import static com.io7m.eigion.gui.internal.main.EIGScreenTransition.WIPE;
import static com.io7m.eigion.icons.EIIconSemantic.ERROR_24;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * The controller for the logo screen.
 */

public final class EIGSplashController
  implements EIScreenControllerWithoutServicesType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIGSplashController.class);

  private final EIGMainScreenController mainScreenController;
  private final EIGConfiguration configuration;
  private final EIGStrings strings;

  @FXML private ImageView splashErrorIcon;
  @FXML private Pane splashErrorLayout;
  @FXML private Pane splashProgressLayout;
  @FXML private ImageView splashImage;
  @FXML private ProgressBar splashProgress;
  @FXML private ProgressBar splashProgressSmall;
  @FXML private Label splashText;

  private volatile EITask<EIServiceDirectoryType> task;

  /**
   * The controller for the logo screen
   *
   * @param inMainScreenController The main screen controller
   * @param inConfiguration        The UI configuration
   * @param inStrings              The strings
   */

  public EIGSplashController(
    final EIGMainScreenController inMainScreenController,
    final EIGConfiguration inConfiguration,
    final EIGStrings inStrings)
  {
    this.mainScreenController =
      Objects.requireNonNull(inMainScreenController, "mainScreenController");
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
  }

  @Override
  public void initialize(
    final URL location,
    final ResourceBundle resources)
  {
    final var logo =
      this.configuration.logoConfiguration();

    this.splashErrorIcon.setImage(
      this.configuration.iconsConfiguration().icon(ERROR_24));
    this.splashErrorLayout.setVisible(false);
    this.splashProgressLayout.setVisible(true);

    this.splashText.setText("");
    this.splashImage.setImage(new Image(logo.logoURI().toString()));
    this.splashImage.setFitWidth(logo.logoWidth());
    this.splashImage.setFitHeight(logo.logoHeight());

    final var future =
      EIGBootServices.create(this.configuration, this.strings, event -> {
        Platform.runLater(() -> this.onProgressUpdated(event));
      });

    future.whenComplete((bootTask, exception) -> {
      Platform.runLater(() -> {
        this.splashProgressSmall.setVisible(false);
      });

      this.task = bootTask;

      if (exception != null) {
        LOG.debug("services failed: ", exception);
        this.task = EITask.create(LOG, "Booting application...");
        this.task.setFailed(exception.getMessage(), Optional.of(exception));
        this.showError();
        return;
      }

      final var resolution = bootTask.resolution();
      if (resolution instanceof EIFailed failed) {
        LOG.debug("services failed: {}", failed);
        this.showError();
        return;
      }

      final var services =
        bootTask.result().orElseThrow();

      this.startApplication(services);
    });
  }

  private void showError()
  {
    Platform.runLater(() -> {
      this.splashProgressLayout.setVisible(false);
      this.splashErrorLayout.setVisible(true);
    });
  }

  private void startApplication(
    final EIServiceDirectoryType services)
  {
    final var executor =
      services.requireService(EIGBackgroundSchedulerService.class);

    final var seconds =
      this.configuration.logoConfiguration()
        .logoDuration()
        .toSeconds();

    executor.executor()
      .schedule(() -> this.openNextScreen(services), seconds, SECONDS);
  }

  private void openNextScreen(
    final EIServiceDirectoryType services)
  {
    if (this.configuration.serverConfiguration().requiresLogin()) {
      this.mainScreenController.openLoginScreen(services, WIPE);
    } else {
      this.mainScreenController.openDashboard(services, WIPE);
    }
  }

  private void onProgressUpdated(
    final EIBootEvent event)
  {
    this.splashProgress.setProgress(event.progress());
    this.splashText.setText(event.message());
  }

  @FXML
  private void onReportButtonClicked()
  {
    new EIGErrorDialogs(this.strings, this.configuration)
      .open(this.task);
  }

  @FXML
  private void onExitButtonClicked()
  {
    Platform.exit();
  }
}
