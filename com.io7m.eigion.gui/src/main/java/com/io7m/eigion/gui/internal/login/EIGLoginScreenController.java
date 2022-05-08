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

import com.io7m.eigion.client.api.EIClientLoggedIn;
import com.io7m.eigion.client.api.EIClientLoggedOut;
import com.io7m.eigion.client.api.EIClientLoginFailed;
import com.io7m.eigion.client.api.EIClientLoginInProcess;
import com.io7m.eigion.client.api.EIClientLoginNotRequired;
import com.io7m.eigion.client.api.EIClientLoginStatusType;
import com.io7m.eigion.client.api.EIClientLoginWentOffline;
import com.io7m.eigion.gui.internal.EIGIcons;
import com.io7m.eigion.gui.internal.client.EIGClient;
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
import javafx.scene.layout.Pane;

import java.net.URL;
import java.util.ResourceBundle;

import static com.io7m.eigion.client.api.EIClientOnline.CLIENT_OFFLINE;

/**
 * The controller for the login screen.
 */

public final class EIGLoginScreenController implements Initializable
{
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
  @FXML private Button offline;

  private EITask<?> task;

  /**
   * The controller for the login screen.
   *
   * @param services The service directory
   */

  public EIGLoginScreenController(
    final EIServiceDirectoryType services)
  {
    this.errors =
      services.requireService(EIGErrorDialogs.class);
    this.client =
      services.requireService(EIGClient.class);
    this.icons =
      services.requireService(EIGIcons.class);
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

  @FXML
  private void onOfflineSelected()
  {
    this.client.onlineSet(CLIENT_OFFLINE);
  }

  @Override
  public void initialize(
    final URL location,
    final ResourceBundle resources)
  {
    this.progress.setVisible(false);

    this.errorIcon.setImage(this.icons.error24());
    this.client.loginStatus()
      .subscribe((oldValue, newValue) -> {
        Platform.runLater(() -> {
          this.onLoginStatusChanged(newValue);
        });
      });

    this.onLoginStatusChanged(this.client.loginStatus().get());
  }

  private void onLoginStatusChanged(
    final EIClientLoginStatusType status)
  {
    if (status instanceof EIClientLoginNotRequired) {
      return;
    }

    if (status instanceof EIClientLoginWentOffline) {
      return;
    }

    if (status instanceof EIClientLoggedIn) {
      return;
    }

    if (status instanceof EIClientLoggedOut) {
      this.onClientLoginInitial();
      return;
    }

    if (status instanceof EIClientLoginInProcess) {
      this.onClientLoginInProcess();
      return;
    }

    if (status instanceof EIClientLoginFailed failed) {
      this.onClientLoginFailed(failed.task());
      return;
    }

    throw new IllegalStateException();
  }

  private void onClientLoginFailed(
    final EITask<Void> newTask)
  {
    this.formUnlock();
    this.progress.setVisible(false);
    this.errorLayout.setVisible(true);
    this.task = newTask;
    return;
  }

  private void onClientLoginInProcess()
  {
    this.formLock();
    this.progress.setVisible(true);
  }

  private void onClientLoginInitial()
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
}
