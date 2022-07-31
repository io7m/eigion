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

import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.pike.EIPClients;
import com.io7m.eigion.pike.api.EIPClientException;
import com.io7m.eigion.pike.api.EIPClientType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Locale;
import java.util.UUID;

import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.Cancelled;
import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.InProgress;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pike client tests.
 */

public final class EIPikeTest extends EIWithServerContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIPikeTest.class);

  private EIPClients clients;
  private EIPClientType client;

  @BeforeEach
  public void setup()
    throws Exception
  {
    LOG.debug("setup");
    this.clients = new EIPClients();
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
    this.serverCreateUser(
      this.serverCreateAdminInitial("someone", "12345678"),
      "someone");
    this.client.login("someone", "12345678", this.serverPublicURI());
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
      assertThrows(EIPClientException.class, () -> {
        this.client.login("someone", "12345678", this.serverPublicURI());
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
        assertThrows(EIPClientException.class, () -> {
          this.client.login(
            "someone",
            "12345678",
            URI.create("http://localhost:20000/"));
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
        assertThrows(EIPClientException.class, () -> {
          this.client.login(
            "someone",
            "12345678",
            URI.create("http://localhost:20000/"));
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
  public void testLoginPublicAPIGarbage()
    throws Exception
  {
    try (var ignored = EIFakeServerPublicButGarbage.create(20000)) {
      final var ex =
        assertThrows(EIPClientException.class, () -> {
          this.client.login(
            "someone",
            "12345678",
            URI.create("http://localhost:20000/"));
        });
      LOG.debug("", ex);
      assertTrue(ex.getMessage().contains("Unrecognized token 'hello'"));
    }
  }

  /**
   * Creating and then cancelling a group creation works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreateCancel()
    throws Exception
  {
    this.serverCreateUser(
      this.serverCreateAdminInitial("someone", "12345678"),
      "someone");

    this.client.login("someone", "12345678", this.serverPublicURI());

    final var challenge =
      this.client.groupCreationBegin(new EIGroupName("com.io7m.ex"));

    final var requestsBefore =
      this.client.groupCreationRequests();

    assertEquals(InProgress.class, requestsBefore.get(0).status().getClass());

    this.client.groupCreationCancel(challenge.token());

    final var requestsAfter =
      this.client.groupCreationRequests();

    assertEquals(Cancelled.class, requestsAfter.get(0).status().getClass());
  }

  /**
   * Creating too many group requests fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreateTooMany()
    throws Exception
  {
    this.serverCreateUser(
      this.serverCreateAdminInitial("someone", "12345678"),
      "someone");

    this.client.login("someone", "12345678", this.serverPublicURI());

    this.client.groupCreationBegin(new EIGroupName("com.io7m.ex"));
    this.client.groupCreationBegin(new EIGroupName("com.io7m.ex"));
    this.client.groupCreationBegin(new EIGroupName("com.io7m.ex"));
    this.client.groupCreationBegin(new EIGroupName("com.io7m.ex"));
    this.client.groupCreationBegin(new EIGroupName("com.io7m.ex"));

    final var ex = assertThrows(EIPClientException.class, () -> {
      this.client.groupCreationBegin(new EIGroupName("com.io7m.ex"));
    });
    assertTrue(ex.getMessage().contains("Too many requests"));
  }

  /**
   * Trying to cancel a different user's group creation request fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreateNotMine()
    throws Exception
  {
    final var admin =
      this.serverCreateAdminInitial("someone", "12345678");
    this.serverCreateUser(admin, "someone0");
    this.serverCreateUser(admin, "someone1");

    this.client.login("someone0", "12345678", this.serverPublicURI());

    final var challenge =
      this.client.groupCreationBegin(new EIGroupName("com.io7m.ex"));

    this.client.login("someone1", "12345678", this.serverPublicURI());

    final var ex = assertThrows(EIPClientException.class, () -> {
      this.client.groupCreationCancel(challenge.token());
    });
    assertTrue(ex.getMessage().contains("may only cancel their own"));
  }

  /**
   * Cancelling a nonexistent group creation fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreateNonexistent()
    throws Exception
  {
    this.serverCreateUser(
      this.serverCreateAdminInitial("someone", "12345678"),
      "someone");

    this.client.login("someone", "12345678", this.serverPublicURI());

    final var ex = assertThrows(EIPClientException.class, () -> {
      this.client.groupCreationCancel(new EIToken("49F8ACBA8A607A3C39B335A436D4E764"));
    });
    assertTrue(ex.getMessage().contains("Not found"));
  }
}
