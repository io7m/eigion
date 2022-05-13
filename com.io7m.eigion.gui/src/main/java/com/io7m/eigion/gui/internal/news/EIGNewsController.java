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

package com.io7m.eigion.gui.internal.news;

import com.io7m.eigion.client.api.EIClientNewsItem;
import com.io7m.eigion.gui.EIGConfiguration;
import com.io7m.eigion.gui.internal.EIGStrings;
import com.io7m.eigion.gui.internal.client.EIGClient;
import com.io7m.eigion.gui.internal.client.EIGClientNewsStatusType;
import com.io7m.eigion.gui.internal.client.EIGNewsStatusAvailable;
import com.io7m.eigion.gui.internal.client.EIGNewsStatusFetching;
import com.io7m.eigion.gui.internal.client.EIGNewsStatusInitial;
import com.io7m.eigion.gui.internal.client.EIGNewsStatusOffline;
import com.io7m.eigion.gui.internal.main.EIScreenControllerWithServicesType;
import com.io7m.eigion.gui.internal.views.EIGNoSelectionModel;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * The controller for the news view.
 */

public final class EIGNewsController
  implements EIScreenControllerWithServicesType
{
  private final EIGConfiguration configuration;
  private final EIServiceDirectoryType services;
  private final EIGStrings strings;
  private final EIGClient client;

  @FXML private ListView<EIClientNewsItem> newsList;
  @FXML private Label newsOfflineText;
  @FXML private Pane newsProgress;

  /**
   * The controller for the news view.
   *
   * @param inConfiguration The application configuration
   * @param inServices      The service directory
   */

  public EIGNewsController(
    final EIGConfiguration inConfiguration,
    final EIServiceDirectoryType inServices)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.services =
      Objects.requireNonNull(inServices, "services");
    this.strings =
      inServices.requireService(EIGStrings.class);
    this.client =
      inServices.requireService(EIGClient.class);
  }

  @Override
  public void initialize(
    final URL location,
    final ResourceBundle resources)
  {
    this.newsList.setVisible(false);
    this.newsOfflineText.setVisible(false);
    this.newsProgress.setVisible(false);

    this.client.newsStatus()
      .subscribe((oldValue, newValue) -> {
        Platform.runLater(() -> {
          this.clientNewsChanged(newValue);
        });
      });

    this.newsList.setSelectionModel(new EIGNoSelectionModel<>());
    this.newsList.setFocusTraversable(false);
    this.newsList.setCellFactory(param -> {
      return new EIGNewsItemCell(
        this.services,
        this.configuration,
        this.strings
      );
    });
  }

  private void clientNewsChanged(
    final EIGClientNewsStatusType status)
  {
    if (status instanceof EIGNewsStatusAvailable available) {
      this.newsList.getItems().setAll(available.newsItems());
      this.newsList.setVisible(true);
      this.newsOfflineText.setVisible(false);
      this.newsProgress.setVisible(false);
      return;
    }

    if (status instanceof EIGNewsStatusFetching) {
      this.newsList.setVisible(false);
      this.newsOfflineText.setVisible(false);
      this.newsProgress.setVisible(true);
      return;
    }

    if (status instanceof EIGNewsStatusInitial) {
      this.newsList.setVisible(false);
      this.newsOfflineText.setVisible(false);
      this.newsProgress.setVisible(false);
      return;
    }

    if (status instanceof EIGNewsStatusOffline) {
      this.newsList.setVisible(false);
      this.newsOfflineText.setVisible(true);
      this.newsProgress.setVisible(false);
      return;
    }
  }
}
