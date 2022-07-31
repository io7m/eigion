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

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import java.net.InetSocketAddress;
import java.util.Objects;

public final class EIFakeServerPublicButGarbage
  implements AutoCloseable
{
  private final Server server;

  private EIFakeServerPublicButGarbage(
    final Server inServer)
  {
    this.server = Objects.requireNonNull(inServer, "server");
  }

  public static EIFakeServerPublicButGarbage create(
    final int port)
    throws Exception
  {
    final var server =
      new Server(new InetSocketAddress("localhost", port));

    final var servlets = new ServletContextHandler();
    servlets.addServlet(EIV1PublicProtocols.class, "/*");
    servlets.addServlet(EIV1PublicProtocolGarbage.class, "/public/1/0/*");
    servlets.addServlet(EIV1PublicProtocolGarbage.class, "/public/1/0");
    server.setHandler(servlets);
    server.start();
    return new EIFakeServerPublicButGarbage(server);
  }

  @Override
  public void close()
    throws Exception
  {
    this.server.stop();
  }
}
