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

import com.io7m.eigion.protocol.api.EIProtocolException;
import com.io7m.eigion.protocol.versions.EISVMessages;
import com.io7m.eigion.protocol.versions.EISVProtocolSupported;
import com.io7m.eigion.protocol.versions.EISVProtocols;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public final class EIV1NonsenseProtocols extends HttpServlet
{
  private final EISVMessages messages;

  public EIV1NonsenseProtocols()
  {
    this.messages = new EISVMessages();
  }

  private static EISVProtocols createProtocols()
  {
    final var supported = new ArrayList<EISVProtocolSupported>();
    supported.add(
      new EISVProtocolSupported(
        "com.io7m.nonsense",
        new BigInteger("1093"),
        new BigInteger("399"),
        "/nonsense/1093/399"
      )
    );
    return new EISVProtocols(List.copyOf(supported));
  }

  @Override
  protected void service(
    final HttpServletRequest request,
    final HttpServletResponse response)
    throws IOException
  {
    try {
      response.setContentType(EISVMessages.contentType());
      response.setStatus(200);

      final var data = this.messages.serialize(createProtocols());
      response.setContentLength(data.length);

      try (var output = response.getOutputStream()) {
        output.write(data);
      }
    } catch (final EIProtocolException e) {
      throw new IOException(e);
    }
  }
}
