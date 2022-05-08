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
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ScheduledFuture;

import static com.io7m.eigion.gui.internal.logo.EIGLogoEvent.LOGO_SCREEN_WANT_CLOSE;
import static com.io7m.eigion.gui.internal.logo.EIGLogoEvent.LOGO_SCREEN_WANT_CLOSE_IMMEDIATE;
import static com.io7m.eigion.gui.internal.logo.EIGLogoEvent.LOGO_SCREEN_WANT_OPEN;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * The controller for the logo screen.
 */

public final class EIGLogoController implements Initializable
{
  private final EIGEventBus eventBus;
  private final EIGBackgroundSchedulerService executor;
  private final EIGConfiguration configuration;
  private ScheduledFuture<?> delayFuture;

  @FXML private Pane logoLayout;
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
        case LOGO_SCREEN_WANT_OPEN -> Platform.runLater(this::onLogoWantOpen);
        case LOGO_SCREEN_WANT_CLOSE, LOGO_SCREEN_WANT_CLOSE_IMMEDIATE -> {

        }
      }
    }
  }

  private void onLogoWantOpen()
  {
    final var logoOpt =
      this.configuration.logoConfiguration();

    if (logoOpt.isEmpty()) {
      this.eventBus.submit(LOGO_SCREEN_WANT_CLOSE);
      return;
    }

    final var logo = logoOpt.get();

    final var bgColor = Color.color(
      logo.logoBackgroundColor().x(),
      logo.logoBackgroundColor().y(),
      logo.logoBackgroundColor().z()
    );

    this.logoLayout.setBackground(
      new Background(new BackgroundFill(bgColor, null, null))
    );
    this.logoImage.setFitWidth(logo.logoWidth());
    this.logoImage.setFitHeight(logo.logoHeight());
    this.logoImage.setImage(
      new Image(logo.logoURI().toString(), true)
    );

    final var seconds = logo.logoDuration().toSeconds();
    this.delayFuture =
      this.executor.executor()
        .schedule(() -> Platform.runLater(this::closeLogo), seconds, SECONDS);
  }

  @Override
  public void initialize(
    final URL location,
    final ResourceBundle resources)
  {
    final var logoOpt =
      this.configuration.logoConfiguration();

    if (logoOpt.isPresent()) {
      this.eventBus.submit(LOGO_SCREEN_WANT_OPEN);
    } else {
      this.eventBus.submit(LOGO_SCREEN_WANT_CLOSE_IMMEDIATE);
    }
  }

  @FXML
  private void onLogoClicked()
  {
    final var f = this.delayFuture;
    if (f != null) {
      f.cancel(true);
    }

    this.closeLogo();
  }

  private void closeLogo()
  {
    this.eventBus.submit(LOGO_SCREEN_WANT_CLOSE);
  }
}
