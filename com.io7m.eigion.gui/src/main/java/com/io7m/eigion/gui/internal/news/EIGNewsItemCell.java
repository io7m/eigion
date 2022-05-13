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
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

import static javafx.scene.control.ContentDisplay.GRAPHIC_ONLY;

/**
 * A cell containing a news item.
 */

public final class EIGNewsItemCell extends ListCell<EIClientNewsItem>
{
  private final Pane root;
  private final EIGNewsItemCellController controller;

  /**
   * A cell containing a news item.
   *
   * @param services      The service directory
   * @param configuration The application configuration
   * @param strings       The strings
   */

  public EIGNewsItemCell(
    final EIServiceDirectoryType services,
    final EIGConfiguration configuration,
    final EIGStrings strings)
  {
    try {
      final FXMLLoader loader =
        new FXMLLoader(
          EIGNewsItemCell.class.getResource(
            "/com/io7m/eigion/gui/internal/newsItem.fxml"));
      loader.setResources(strings.resources());
      loader.setControllerFactory(param -> {
        return new EIGNewsItemCellController(configuration, services);
      });
      this.root = loader.load();
      this.controller = loader.getController();
      Objects.requireNonNull(this.root, "this.root");
      Objects.requireNonNull(this.controller, "this.controller");
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  protected void updateItem(
    final EIClientNewsItem item,
    final boolean empty)
  {
    super.updateItem(item, empty);

    this.setContentDisplay(GRAPHIC_ONLY);
    if (empty || item == null) {
      this.setGraphic(null);
      this.setText(null);
    } else {
      this.controller.setItem(item);
      this.setGraphic(this.root);
      this.setText(null);
    }
  }
}
