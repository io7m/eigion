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


package com.io7m.eigion.gui.internal.logo;

import com.io7m.eigion.gui.EIGConfiguration;
import com.io7m.eigion.gui.internal.EIGBackgroundSchedulerService;
import com.io7m.eigion.gui.internal.EIGEventBus;
import com.io7m.eigion.gui.internal.EIGEventType;
import com.io7m.eigion.gui.internal.EIGPerpetualSubscriber;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.io7m.eigion.gui.internal.logo.EIGLogoEvent.LOGO_APPEARED;
import static com.io7m.eigion.gui.internal.logo.EIGLogoEvent.LOGO_DISAPPEARED;

/**
 * The controller for the logo screen.
 */

public final class EIGLogoController implements Initializable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIGLogoController.class);

  private final EIGEventBus eventBus;
  private final EIGBackgroundSchedulerService executor;
  private final EIGConfiguration configuration;
  private ScheduledFuture<?> fadeTimer;

  @FXML private Pane logoBackdrop;
  @FXML private ImageView logoImage;

  /**
   * The controller for the logo screen
   *
   * @param services        The service directory
   * @param inConfiguration The UI configuration
   */

  public EIGLogoController(
    final EIServiceDirectoryType services,
    final EIGConfiguration inConfiguration)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.executor =
      services.requireService(EIGBackgroundSchedulerService.class);

    this.eventBus = services.requireService(EIGEventBus.class);
    this.eventBus.subscribe(new EIGPerpetualSubscriber<>(this::onEvent));
  }

  private void onEvent(
    final EIGEventType event)
  {
    if (event instanceof EIGLogoEvent logoEvent) {
      switch (logoEvent) {
        case LOGO_APPEARED -> Platform.runLater(this::onLogoAppeared);
        case LOGO_DISAPPEARED -> Platform.runLater(this::onLogoDisappeared);
      }
      return;
    }
  }

  private void onLogoDisappeared()
  {
    this.logoBackdrop.setVisible(false);
  }

  private void onLogoAppeared()
  {
    final var logoOpt =
      this.configuration.logoConfiguration();

    if (logoOpt.isEmpty()) {
      this.eventBus.submit(LOGO_DISAPPEARED);
      return;
    }

    final var logo = logoOpt.get();

    final var bgColor = Color.color(
      logo.logoBackgroundColor().x(),
      logo.logoBackgroundColor().y(),
      logo.logoBackgroundColor().z()
    );

    this.logoBackdrop.setBackground(
      new Background(new BackgroundFill(bgColor, null, null))
    );
    this.logoImage.setFitWidth(logo.logoWidth());
    this.logoImage.setFitHeight(logo.logoHeight());
    this.logoImage.setImage(
      new Image(logo.logoURI().toString(), true)
    );

    this.fadeTimer =
      this.executor.executor()
        .schedule(
          () -> Platform.runLater(this::closeLogo),
          logo.logoDuration().toSeconds(),
          TimeUnit.SECONDS
        );
  }

  @Override
  public void initialize(
    final URL location,
    final ResourceBundle resources)
  {
    LOG.debug("init");

    final var logoOpt =
      this.configuration.logoConfiguration();

    if (logoOpt.isPresent()) {
      this.eventBus.submit(LOGO_APPEARED);
    } else {
      this.eventBus.submit(LOGO_DISAPPEARED);
    }
  }

  @FXML
  private void onLogoClicked()
  {
    final var f = this.fadeTimer;
    if (f != null) {
      f.cancel(true);
    }

    this.closeLogo();
  }

  private void closeLogo()
  {
    final var fadeOut =
      new FadeTransition(Duration.millis(250.0), this.logoBackdrop);
    fadeOut.setFromValue(1.0);
    fadeOut.setToValue(0.0);
    fadeOut.play();
    fadeOut.setOnFinished(event -> {
      this.eventBus.submit(LOGO_DISAPPEARED);
    });
  }
}
