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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class EIFakeServerDomainCheckServlet extends HttpServlet
{
  public static volatile Optional<String> RETURN_TOKEN = Optional.empty();

  public EIFakeServerDomainCheckServlet()
  {

  }

  @Override
  protected void service(
    final HttpServletRequest req,
    final HttpServletResponse resp)
    throws ServletException, IOException
  {
    if (RETURN_TOKEN.isEmpty()) {
      resp.setStatus(404);
      resp.setContentLength(0);
      return;
    }

    final var token = RETURN_TOKEN.get();
    resp.setStatus(200);
    try (var output = resp.getOutputStream()) {
      final var bytes = token.getBytes(StandardCharsets.UTF_8);
      resp.setContentLength(bytes.length);
      output.write(bytes);
    }
  }
}
