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

package com.io7m.eigion.gui.internal.login;

import com.io7m.eigion.client.api.EIClientStatusType.EIClientStatusLoggedIn;
import com.io7m.eigion.client.api.EIClientStatusType.EIClientStatusLoggingIn;
import com.io7m.eigion.client.api.EIClientStatusType.EIClientStatusLoginFailed;
import com.io7m.eigion.gui.EIGConfiguration;
import com.io7m.eigion.gui.internal.EIGEventBus;
import com.io7m.eigion.gui.internal.EIGEventType;
import com.io7m.eigion.gui.internal.EIGIcons;
import com.io7m.eigion.gui.internal.EIGPerpetualSubscriber;
import com.io7m.eigion.gui.internal.client.EIGClient;
import com.io7m.eigion.gui.internal.client.EIGClientStatusChanged;
import com.io7m.eigion.gui.internal.errors.EIGErrorDialogs;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import com.io7m.eigion.taskrecorder.EITask;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

import static com.io7m.eigion.gui.internal.login.EIGLoginScreenEvent.LOGIN_FINISHED;
import static com.io7m.eigion.gui.internal.login.EIGLoginScreenEvent.LOGIN_REQUIRED;

/**
 * The controller for the login screen.
 */

public final class EIGLoginScreenController implements Initializable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIGLoginScreenController.class);

  private final EIGConfiguration configuration;
  private final EIGEventBus eventBus;
  private final EIGClient client;
  private final EIGErrorDialogs errors;
  private final EIGIcons icons;

  @FXML private Pane loginLayout;
  @FXML private TextField username;
  @FXML private TextField password;
  @FXML private Button login;
  @FXML private ProgressBar progress;
  @FXML private Pane errorLayout;
  @FXML private ImageView errorIcon;

  private EITask<?> task;

  /**
   * The controller for the login screen.
   *
   * @param services        The service directory
   * @param inConfiguration The UI configuration
   */

  public EIGLoginScreenController(
    final EIServiceDirectoryType services,
    final EIGConfiguration inConfiguration)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");

    this.errors = services.requireService(EIGErrorDialogs.class);
    this.client = services.requireService(EIGClient.class);
    this.icons = services.requireService(EIGIcons.class);
    this.eventBus = services.requireService(EIGEventBus.class);
    this.eventBus.subscribe(new EIGPerpetualSubscriber<>(this::onEvent));
  }

  private void onEvent(
    final EIGEventType event)
  {
    if (event instanceof EIGLoginScreenEvent loginScreenEvent) {
      switch (loginScreenEvent) {
        case LOGIN_REQUIRED -> Platform.runLater(this::onLoginRequired);
        case LOGIN_FINISHED -> Platform.runLater(this::onLoginFinished);
      }
      return;
    }
    if (event instanceof EIGClientStatusChanged status) {
      Platform.runLater(() -> this.onClientStatusChanged(status));
      return;
    }
  }

  private void onClientStatusChanged(
    final EIGClientStatusChanged changed)
  {
    final var status = changed.status();

    if (status instanceof EIClientStatusLoggingIn) {
      this.formLock();
      this.progress.setVisible(true);
      return;
    }

    if (status instanceof EIClientStatusLoginFailed failed) {
      this.formUnlock();
      this.progress.setVisible(false);
      this.errorLayout.setVisible(true);
      this.task = failed.task();
      return;
    }

    if (status instanceof EIClientStatusLoggedIn) {
      this.eventBus.submit(LOGIN_FINISHED);
    }
  }

  private void formUnlock()
  {
    this.username.setDisable(false);
    this.password.setDisable(false);
    this.login.setDisable(false);
  }

  private void formLock()
  {
    this.username.setDisable(true);
    this.password.setDisable(true);
    this.login.setDisable(true);
  }

  private void onLoginFinished()
  {
    this.loginLayout.setVisible(false);
  }

  private void onLoginRequired()
  {
    this.loginLayout.setVisible(true);
    this.username.setText("");
    this.username.setDisable(false);
    this.password.setText("");
    this.password.setDisable(false);
    this.login.setDisable(true);
    this.progress.setVisible(false);
    this.errorLayout.setVisible(false);
  }

  @FXML
  private void onFormUpdated()
  {
    final var textU =
      this.username.getText();
    final var textP =
      this.password.getText();

    this.login.setDisable(
      textU.isBlank() || textP.isBlank()
    );
  }

  @FXML
  private void onLoginErrorDetailsSelected()
  {
    this.errors.open(this.task);
  }

  @FXML
  private void onLoginSelected()
  {
    this.formLock();
    this.progress.setVisible(true);

    this.client.login(this.username.getText(), this.password.getText());
  }

  @Override
  public void initialize(
    final URL location,
    final ResourceBundle resources)
  {
    LOG.debug("init");
    this.progress.setVisible(false);

    this.errorIcon.setImage(this.icons.error24());
    this.loginLayout.setBackground(
      new Background(new BackgroundFill(Color.WHEAT, null, null))
    );

    if (this.configuration.serverConfiguration().requiresLogin()) {
      this.eventBus.submit(LOGIN_REQUIRED);
    } else {
      this.eventBus.submit(LOGIN_FINISHED);
    }
  }
}
