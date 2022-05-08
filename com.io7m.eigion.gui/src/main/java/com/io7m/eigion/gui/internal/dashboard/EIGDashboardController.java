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
import com.io7m.eigion.gui.internal.EIGIcons;
import com.io7m.eigion.gui.internal.EIGStrings;
import com.io7m.eigion.gui.internal.client.EIGClient;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

import static com.io7m.eigion.client.api.EIClientOnline.CLIENT_OFFLINE;
import static com.io7m.eigion.client.api.EIClientOnline.CLIENT_ONLINE;

/**
 * The dashboard controller.
 */

public final class EIGDashboardController implements Initializable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIGDashboardController.class);

  private final EIGStrings strings;
  private final EIGClient client;
  private final EIGIcons icons;

  @FXML private CheckBox onlineToggle;
  @FXML private Label onlineText;

  /**
   * The dashboard controller.
   *
   * @param inServices      The service directory
   * @param inConfiguration The UI configuration
   */

  public EIGDashboardController(
    final EIServiceDirectoryType inServices,
    final EIGConfiguration inConfiguration)
  {
    this.strings =
      inServices.requireService(EIGStrings.class);

    this.client =
      inServices.requireService(EIGClient.class);
    this.icons =
      inServices.requireService(EIGIcons.class);
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
