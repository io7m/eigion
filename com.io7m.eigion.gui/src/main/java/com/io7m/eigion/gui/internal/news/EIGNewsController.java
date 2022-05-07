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
import com.io7m.eigion.client.api.EIClientStatusType.EIClientStatusLoggedIn;
import com.io7m.eigion.gui.internal.EIGEventBus;
import com.io7m.eigion.gui.internal.EIGEventType;
import com.io7m.eigion.gui.internal.EIGPerpetualSubscriber;
import com.io7m.eigion.gui.internal.EIGStrings;
import com.io7m.eigion.gui.internal.client.EIGClient;
import com.io7m.eigion.gui.internal.client.EIGClientStatusChanged;
import com.io7m.eigion.gui.internal.views.EIGNoSelectionModel;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import com.io7m.eigion.taskrecorder.EITask;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * The controller for the news view.
 */

public final class EIGNewsController implements Initializable
{
  private final EIServiceDirectoryType services;
  private final EIGStrings strings;
  private final EIGEventBus eventBus;
  private final EIGClient client;

  @FXML private ListView<EIClientNewsItem> newsList;

  /**
   * The controller for the news view.
   *
   * @param inServices The service directory
   */

  public EIGNewsController(
    final EIServiceDirectoryType inServices)
  {
    this.services =
      Objects.requireNonNull(inServices, "services");
    this.strings =
      inServices.requireService(EIGStrings.class);

    this.client =
      this.services.requireService(EIGClient.class);
    this.eventBus =
      this.services.requireService(EIGEventBus.class);
  }

  private void onEvent(
    final EIGEventType event)
  {
    if (event instanceof EIGClientStatusChanged status) {
      if (status.status() instanceof EIClientStatusLoggedIn) {
        this.client.news()
          .thenAccept(newsItems -> {
            Platform.runLater(() -> this.onNewsReceived(newsItems));
          });
      }
    }
  }

  private void onNewsReceived(
    final EITask<List<EIClientNewsItem>> newsItems)
  {
    this.newsList.getItems()
      .setAll(newsItems.result().orElse(List.of()));
  }

  @Override
  public void initialize(
    final URL location,
    final ResourceBundle resources)
  {
    this.newsList.setSelectionModel(new EIGNoSelectionModel<>());
    this.newsList.setFocusTraversable(false);
    this.newsList.setCellFactory(param -> {
      return new EIGNewsItemCell(this.services, this.strings);
    });

    this.eventBus.subscribe(new EIGPerpetualSubscriber<>(this::onEvent));
  }
}
