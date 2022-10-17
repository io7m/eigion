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

import com.io7m.eigion.client.api.EIClientConfiguration;
import com.io7m.eigion.client.api.EIClientType;
import com.io7m.eigion.client.vanilla.EIClients;
import com.io7m.eigion.taskrecorder.EISucceeded;
import com.io7m.jade.api.ApplicationDirectoriesType;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Path;

import static com.io7m.eigion.client.api.EIClientLoggedIn.CLIENT_LOGGED_IN;
import static com.io7m.eigion.client.api.EIClientLoggedOut.CLIENT_LOGGED_OUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class EIClientTest
{
  private ServletContextHandler servlets;
  private Server server;
  private EIClients clients;
  private Path directory;
  private ApplicationDirectoriesType directories;
  private EIClientType client;

  @BeforeEach
  public void setup()
    throws Exception
  {
    this.directory =
      EITestDirectories.createTempDirectory();
    this.directories =
      new ApplicationDirectoriesType()
      {
        @Override
        public Path configurationDirectory()
        {
          return EIClientTest.this.directory.resolve("config");
        }

        @Override
        public Path dataDirectory()
        {
          return EIClientTest.this.directory.resolve("data");
        }

        @Override
        public Path cacheDirectory()
        {
          return EIClientTest.this.directory.resolve("cache");
        }
      };

    this.clients = new EIClients();
    this.client =
      this.clients.create(
        EIClientConfiguration.builder(
            this.directories,
            URI.create("http://localhost:50000/"))
          .build()
      );

    this.server =
      new Server(new InetSocketAddress("localhost", 50000));

    this.servlets = new ServletContextHandler();
    this.servlets.addServlet(EIFailingServlet.class, "/");
    this.server.setHandler(this.servlets);
  }

  @AfterEach
  public void tearDown()
    throws Exception
  {
    this.client.close();
    this.server.stop();
  }

  /**
   * Logging in works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLogin()
    throws Exception
  {
    this.servlets.addServlet(EIV1LoginOK.class, "/v1/login");
    this.server.start();

    assertEquals(CLIENT_LOGGED_OUT, this.client.loginStatus().get());
    this.client.login("user", "pass");
    assertEquals(CLIENT_LOGGED_IN, this.client.loginStatus().get());
  }
}
