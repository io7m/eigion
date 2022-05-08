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

import com.io7m.eigion.client.api.EIClientLoggedIn;
import com.io7m.eigion.client.api.EIClientLoggedOut;
import com.io7m.eigion.client.api.EIClientLoginFailed;
import com.io7m.eigion.client.api.EIClientLoginInProcess;
import com.io7m.eigion.client.api.EIClientLoginNotRequired;
import com.io7m.eigion.client.api.EIClientLoginStatusType;
import com.io7m.eigion.client.api.EIClientLoginWentOffline;
import com.io7m.eigion.gui.EIGConfiguration;
import com.io7m.eigion.gui.internal.client.EIGClient;
import com.io7m.eigion.gui.internal.logo.EIGLogoEvent;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * The controller for the main screen.
 */

public final class EIGMainController implements Initializable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIGMainController.class);

  private final EIGClient client;
  private final EIGEventBus events;

  @FXML private Node loginLayout;
  @FXML private Node logoLayout;
  @FXML private Node dashboardLayout;

  /**
   * The controller for the main screen.
   *
   * @param services      The service directory
   * @param configuration The configuration
   */

  public EIGMainController(
    final EIServiceDirectoryType services,
    final EIGConfiguration configuration)
  {
    this.client =
      services.requireService(EIGClient.class);
    this.events =
      services.requireService(EIGEventBus.class);
  }

  private static void fadeOutAndThen(
    final Node layout,
    final Runnable runnable)
  {
    final var fadeOut = new FadeTransition(Duration.millis(250.0), layout);
    fadeOut.setFromValue(1.0);
    fadeOut.setToValue(0.0);
    fadeOut.play();
    fadeOut.setOnFinished(e -> runnable.run());
  }

  private void onEvent(
    final EIGEventType event)
  {
    if (event instanceof EIGLogoEvent logoEvent) {
      Platform.runLater(() -> {
        switch (logoEvent) {
          case LOGO_SCREEN_WANT_OPEN -> {
            this.logoLayout.setVisible(true);
          }

          case LOGO_SCREEN_WANT_CLOSE_IMMEDIATE -> {
            this.logoLayout.setVisible(false);
          }

          case LOGO_SCREEN_WANT_CLOSE -> {
            this.logoLayout.setVisible(true);

            fadeOutAndThen(this.logoLayout, () -> {
              this.logoLayout.setVisible(false);
              this.logoLayout.setOpacity(1.0);
            });
          }
        }
      });
    }
  }

  private void onLoginStatusChanged(
    final EIClientLoginStatusType status)
  {
    if (status instanceof EIClientLoginNotRequired) {
      this.loginLayout.setVisible(false);
      return;
    }

    if (status instanceof EIClientLoginWentOffline) {
      fadeOutAndThen(this.loginLayout, () -> {
        this.loginLayout.setVisible(false);
        this.loginLayout.setOpacity(1.0);
      });
      return;
    }

    if (status instanceof EIClientLoggedIn) {
      fadeOutAndThen(this.loginLayout, () -> {
        this.loginLayout.setVisible(false);
      });
      return;
    }

    if (status instanceof EIClientLoggedOut) {
      this.loginLayout.setVisible(true);
      return;
    }

    if (status instanceof EIClientLoginInProcess) {
      this.loginLayout.setVisible(true);
      return;
    }

    if (status instanceof EIClientLoginFailed) {
      this.loginLayout.setVisible(true);
      return;
    }

    throw new IllegalStateException();
  }

  @Override
  public void initialize(
    final URL location,
    final ResourceBundle resources)
  {
    this.client.loginStatus()
      .subscribe((oldValue, newValue) -> {
        Platform.runLater(() -> {
          this.onLoginStatusChanged(newValue);
        });
      });

    this.events.subscribe(
      new EIGPerpetualSubscriber<>(this::onEvent));
  }
}
