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

package com.io7m.eigion.gui.internal.dashboard;

import com.io7m.eigion.client.api.EIClientOnline;
import com.io7m.eigion.gui.EIGConfiguration;
import com.io7m.eigion.gui.internal.EIGApplication;
import com.io7m.eigion.gui.internal.EIGStrings;
import com.io7m.eigion.gui.internal.client.EIGClient;
import com.io7m.eigion.gui.internal.main.EIGMainControllerFactory;
import com.io7m.eigion.gui.internal.main.EIGMainScreenController;
import com.io7m.eigion.gui.internal.main.EIScreenControllerWithServicesType;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

import static com.io7m.eigion.client.api.EIClientOnline.CLIENT_OFFLINE;
import static com.io7m.eigion.client.api.EIClientOnline.CLIENT_ONLINE;
import static com.io7m.eigion.gui.internal.main.EIGScreenTransition.WIPE;

/**
 * The dashboard controller.
 */

public final class EIGDashboardController
  implements EIScreenControllerWithServicesType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIGDashboardController.class);

  private final EIGMainScreenController mainScreenController;
  private final EIGStrings strings;
  private final EIGClient client;
  private final EIGConfiguration configuration;
  private final EIServiceDirectoryType services;

  @FXML private CheckBox onlineToggle;
  @FXML private Label onlineText;

  /**
   * The dashboard controller.
   *
   * @param inMainScreenController The main screen controller
   * @param inServices             The service directory
   * @param inConfiguration        The UI configuration
   */

  public EIGDashboardController(
    final EIGMainScreenController inMainScreenController,
    final EIServiceDirectoryType inServices,
    final EIGConfiguration inConfiguration)
  {
    this.mainScreenController =
      Objects.requireNonNull(inMainScreenController, "mainScreenController");
    this.strings =
      inServices.requireService(EIGStrings.class);
    this.client =
      inServices.requireService(EIGClient.class);
    this.configuration =
      Objects.requireNonNull(inConfiguration, "inConfiguration");
    this.services =
      Objects.requireNonNull(inServices, "inServices");
  }

  @Override
  public void initialize(
    final URL location,
    final ResourceBundle resources)
  {
    this.client.onlineStatus()
      .subscribe((oldValue, newValue) -> {
        Platform.runLater(() -> {
          this.clientOnlineChanged(newValue);
        });
      });
  }

  @FXML
  private void onOnlineToggleSelected()
  {
    final var mode =
      this.onlineToggle.isSelected() ? CLIENT_ONLINE : CLIENT_OFFLINE;

    this.client.onlineSet(mode);

    if (mode == CLIENT_ONLINE && this.configuration.serverConfiguration().requiresLogin()) {
      this.mainScreenController.openLoginScreen(this.services, WIPE);
    }
  }

  @FXML
  private void onMenuHelpServicesSelected()
    throws IOException
  {
    final var xml =
      EIGApplication.class.getResource(
        "/com/io7m/eigion/gui/internal/services.fxml");
    Objects.requireNonNull(xml, "mainXML");

    final var mainLoader =
      new FXMLLoader(xml, this.strings.resources());

    mainLoader.setControllerFactory(
      new EIGMainControllerFactory(
        this.mainScreenController,
        this.services,
        this.configuration));

    final Pane pane = mainLoader.load();
    this.configuration.customCSS()
      .ifPresent(customCSS -> {
        pane.getStylesheets()
          .add(customCSS.toString());
      });

    final var stage = new Stage();
    stage.setTitle(this.strings.format("services"));
    stage.setWidth(640.0);
    stage.setHeight(480.0);
    stage.setScene(new Scene(pane));
    stage.show();
  }

  private void clientOnlineChanged(
    final EIClientOnline status)
  {
    switch (status) {
      case CLIENT_ONLINE -> {
        this.onlineText.setText(this.strings.format("online.online"));
        this.onlineToggle.setSelected(true);
      }
      case CLIENT_OFFLINE -> {
        this.onlineText.setText(this.strings.format("online.offline"));
        this.onlineToggle.setSelected(false);
      }
    }
  }
}
