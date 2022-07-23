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

import com.io7m.eigion.amberjack.EIAClients;
import com.io7m.eigion.amberjack.api.EIAClientException;
import com.io7m.eigion.amberjack.api.EIAClientType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Amberjack client tests.
 */

public final class EIAmberjackTest extends EIWithServerContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIAmberjackTest.class);

  private EIAClients clients;
  private EIAClientType client;

  @BeforeEach
  public void setup()
    throws Exception
  {
    LOG.debug("setup");
    this.clients = new EIAClients();
    this.client = this.clients.create(Locale.getDefault());
  }

  @AfterEach
  public void tearDown()
    throws Exception
  {
    LOG.debug("tearDown");
    this.client.close();
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
    this.serverCreateAdminInitial("someone", "12345678");
    this.client.login("someone", "12345678", this.serverAdminURI());
  }

  /**
   * Logging in fails when it should fail.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginFails()
    throws Exception
  {
    final var ex =
      assertThrows(EIAClientException.class, () -> {
        this.client.login("someone", "12345678", this.serverAdminURI());
      });
    LOG.debug("", ex);
    assertTrue(ex.getMessage().contains("Login failed"));
  }

  /**
   * Logging in fails without usable protocols.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginFailsWithoutProtocols()
    throws Exception
  {
    try (var ignored = EIFakeServerNonsenseProtocols.create(20000)) {
      final var ex =
        assertThrows(EIAClientException.class, () -> {
          this.client.login("someone", "12345678", URI.create("http://localhost:20000/"));
        });
      LOG.debug("", ex);
      assertTrue(ex.getMessage().contains("client does not support"));
    }
  }

  /**
   * Logging in fails with a broken server.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginFailsWith500()
    throws Exception
  {
    try (var ignored = EIFakeServerAlways500.create(20000)) {
      final var ex =
        assertThrows(EIAClientException.class, () -> {
          this.client.login("someone", "12345678", URI.create("http://localhost:20000/"));
        });
      LOG.debug("", ex);
      assertTrue(ex.getMessage().contains("HTTP server returned an error: 500"));
    }
  }

  /**
   * Logging in fails if the actual admin API sends garbage.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginAdminAPIGarbage()
    throws Exception
  {
    try (var ignored = EIFakeServerAdminButGarbage.create(20000)) {
      final var ex =
        assertThrows(EIAClientException.class, () -> {
          this.client.login("someone", "12345678", URI.create("http://localhost:20000/"));
        });
      LOG.debug("", ex);
      assertTrue(ex.getMessage().contains("Unrecognized token 'hello'"));
    }
  }

  /**
   * Services cannot be retrieved without a login.
   *
   * @throws Exception On errors
   */

  @Test
  public void testServicesNotLoggedIn()
    throws Exception
  {
    final var ex =
      assertThrows(EIAClientException.class, () -> {
        this.client.services();
      });
    LOG.debug("", ex);
    assertTrue(ex.getMessage().contains("Not logged in"));
  }

  /**
   * Services can be retrieved.
   *
   * @throws Exception On errors
   */

  @Test
  public void testServices()
    throws Exception
  {
    this.serverCreateAdminInitial("someone", "12345678");
    this.client.login("someone", "12345678", this.serverAdminURI());

    final var services = this.client.services();
    assertEquals(11, services.size());
    assertTrue(
      services.stream()
        .anyMatch(s -> Objects.equals(
          s.name(),
          "com.io7m.eigion.server.database.postgres.internal.EIServerDatabase"))
    );
  }
}
