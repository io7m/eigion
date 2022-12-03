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

package com.io7m.eigion.server.pike_v1;

import com.io7m.eigion.protocol.pike.cb.EIPCB1Messages;
import com.io7m.eigion.server.service.verdant.EISVerdantMessagesType;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import com.io7m.verdant.core.VProtocolException;
import com.io7m.verdant.core.VProtocolSupported;
import com.io7m.verdant.core.VProtocols;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A versioning servlet.
 */

public final class EISP1Versions extends HttpServlet
{
  private static final VProtocols PROTOCOLS =
    createProtocols();

  private final EISVerdantMessagesType messages;

  /**
   * A versioning servlet.
   *
   * @param inServices The service directory
   */

  public EISP1Versions(
    final EIServiceDirectoryType inServices)
  {
    this.messages =
      inServices.requireService(EISVerdantMessagesType.class);
  }

  private static VProtocols createProtocols()
  {
    final var supported = new ArrayList<VProtocolSupported>();
    supported.add(
      new VProtocolSupported(
        EIPCB1Messages.protocolId(),
        1L,
        0L,
        "/pike/1/0/"
      )
    );
    return new VProtocols(List.copyOf(supported));
  }

  @Override
  protected void service(
    final HttpServletRequest request,
    final HttpServletResponse response)
    throws IOException
  {
    try {
      response.setContentType(EISVerdantMessagesType.contentType());
      response.setStatus(200);

      final var data = this.messages.serialize(PROTOCOLS, 1);
      response.setContentLength(data.length);

      try (var output = response.getOutputStream()) {
        output.write(data);
      }
    } catch (final VProtocolException e) {
      throw new IOException(e);
    }
  }
}
