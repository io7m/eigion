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
import com.io7m.eigion.gui.internal.EIGIcons;
import com.io7m.eigion.gui.internal.EIGStrings;
import com.io7m.eigion.news.xml.EINXParagraph;
import com.io7m.eigion.news.xml.EINXText;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A controller for a news item.
 */

public final class EIGNewsItemCellController implements Initializable
{
  private final EIGNewsParsers parsers;
  private final EIGStrings strings;
  private final EIGIcons icons;

  @FXML private Label newsTitle;
  @FXML private TextFlow newsText;
  @FXML private ImageView newsImage;

  /**
   * A controller for a news item.
   *
   * @param services The service directory
   */

  public EIGNewsItemCellController(
    final EIServiceDirectoryType services)
  {
    this.parsers = services.requireService(EIGNewsParsers.class);
    this.strings = services.requireService(EIGStrings.class);
    this.icons = services.requireService(EIGIcons.class);
  }

  /**
   * Set the news item.
   *
   * @param item The news item
   */

  public void setItem(
    final EIClientNewsItem item)
  {
    this.newsImage.setImage(this.icons.news24());
    this.newsTitle.setText(item.title());

    final var childNodes = this.newsText.getChildren();
    childNodes.clear();

    switch (item.format()) {
      case "application/xml+eigion_news" -> {
        this.setItemXMLNews(item.text());
      }
      default -> {
        this.setItemFormatUnrecognized(item);
      }
    }
  }

  private void setItemFormatUnrecognized(
    final EIClientNewsItem item)
  {
    final var childNodes = this.newsText.getChildren();
    childNodes.add(new Text(this.strings.format(
      "news.formatUnrecognized",
      item.format())));
  }

  private void setItemXMLNews(
    final String text)
  {
    final var childNodes = this.newsText.getChildren();
    childNodes.add(new Text(this.strings.format("news.loading")));

    try {
      final var parser = this.parsers.newsParser();
      final var input = new ByteArrayInputStream(text.getBytes(UTF_8));
      final var document = parser.parse(input);

      childNodes.clear();
      for (final var element : document.elements()) {
        if (element instanceof EINXParagraph paragraph) {
          for (final var inline : paragraph.elements()) {
            if (inline instanceof EINXText eText) {
              childNodes.add(new Text(eText.text()));
            }
          }
          childNodes.add(new Text("\n\n"));
        }
      }
    } catch (final Exception e) {
      childNodes.clear();
      final var errorText =
        this.strings.format("news.error", e.getClass(), e.getMessage());
      childNodes.setAll(List.of(new Text(errorText)));
    }
  }

  @Override
  public void initialize(
    final URL location,
    final ResourceBundle resources)
  {

  }
}
