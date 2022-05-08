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


package com.io7m.eigion.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.io7m.eigion.client.vanilla.v1.EIV1ClientMessagesType;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class EIV1NewsSlowOK extends HttpServlet
{
  public EIV1NewsSlowOK()
  {

  }

  @Override
  protected void service(
    final HttpServletRequest req,
    final HttpServletResponse resp)
    throws IOException
  {
    final var text =
      new String(
        EIV1NewsSlowOK.class.getResource("/com/io7m/eigion/tests/news-0.xml")
          .openStream()
          .readAllBytes(),
        UTF_8
      );

    final var item0 =
      new EIV1ClientMessagesType.EIV1NewsItem(
        "newsItem",
        "2022-05-07T12:39:53+00:00",
        "application/xml+eigion_news",
        "News Item 0",
        text
      );

    final var item1 =
      new EIV1ClientMessagesType.EIV1NewsItem(
        "newsItem",
        "2022-05-07T12:39:52+00:00",
        "application/xml+eigion_news",
        "News Item 1",
        "<x/>"
      );

    final var item2 =
      new EIV1ClientMessagesType.EIV1NewsItem(
        "newsItem",
        "2022-05-07T12:39:51+00:00",
        "application/xml+eigion_news",
        "News Item 2",
        "<NewsItem></NewsItem>"
      );

    final var item3 =
      new EIV1ClientMessagesType.EIV1NewsItem(
        "newsItem",
        "2022-05-07T12:39:51+00:00",
        "application/xml+eigion_news",
        "News Item 2",
        "<NewsItem><Q?></NewsItem>"
      );

    final var item4 =
      new EIV1ClientMessagesType.EIV1NewsItem(
        "newsItem",
        "2022-05-07T12:39:53+00:00",
        "application/octet-stream",
        "News Item X",
        text
      );

    final var news =
      new EIV1ClientMessagesType.EIV1News("news", List.of(item0, item1, item2, item3, item4));

    final var mapper =
      new ObjectMapper();
    final var result =
      mapper.writeValueAsBytes(news);

    try {
      Thread.sleep(2_000L);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    resp.setContentLength(result.length);
    resp.setStatus(200);

    try (var output = resp.getOutputStream()) {
      output.write(result);
    }
  }
}
