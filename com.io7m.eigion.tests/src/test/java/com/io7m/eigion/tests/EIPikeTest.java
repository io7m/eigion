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

import com.io7m.eigion.model.EIGroupCreationRequest;
import com.io7m.eigion.model.EIGroupCreationRequestStatusType.Succeeded;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIGroupRole;
import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.pike.EIPClients;
import com.io7m.eigion.pike.api.EIPClientException;
import com.io7m.eigion.pike.api.EIPClientType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.Cancelled;
import static com.io7m.eigion.model.EIGroupCreationRequestStatusType.InProgress;
import static com.io7m.eigion.model.EIGroupRole.USER_DISMISS;
import static com.io7m.eigion.model.EIGroupRole.USER_INVITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    this.serverStartIfNecessary();
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
      this.client.groupCreationCancel(new EIToken(
        "49F8ACBA8A607A3C39B335A436D4E764"));
    });
    assertTrue(ex.getMessage().contains("Not found"));
  }

  /**
   * Group creation works.
   *
   * @throws Exception On errors
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  public void testGroupCreate()
    throws Exception
  {
    final var user =
      this.serverCreateUser(
        this.serverCreateAdminInitial("someone", "12345678"),
        "someone");

    this.client.login("someone", "12345678", this.serverPublicURI());

    final var groupName =
      new EIGroupName("com.io7m.ex");
    final var c =
      this.client.groupCreationBegin(groupName);

    this.domainCheckers()
      .enqueue(CompletableFuture.completedFuture(
        new EIGroupCreationRequest(
          groupName,
          user,
          c.token(),
          new Succeeded(timeNow(), timeNow())
        )
      ));

    this.client.groupCreationReady(c.token());

    while (true) {
      final var rs =
        this.client.groupCreationRequests();
      final var r =
        rs.get(0);

      if (r.status() instanceof InProgress) {
        Thread.sleep(100L);
        continue;
      }

      break;
    }

    final var groups = this.client.groups();
    assertEquals(groups.get(0).group(), groupName);
    assertEquals(groups.get(0).roles(), EnumSet.allOf(EIGroupRole.class));
    assertEquals(1, groups.size());
  }

  /**
   * Granting roles in a group works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupGrant()
    throws Exception
  {
    final var admin =
      this.serverCreateAdminInitial("someone", "12345678");
    final var user0 =
      this.serverCreateUser(admin, "some0");
    final var user1 =
      this.serverCreateUser(admin, "some1");

    this.groupCreate(user0, "com.io7m.ex");
    this.groupAddUser(user1, "com.io7m.ex", Set.of(USER_INVITE));

    this.client.login("some0", "12345678", this.serverPublicURI());
    this.client.groupGrant(new EIGroupName("com.io7m.ex"), user1, USER_DISMISS);
  }

  /**
   * Unowned roles cannot be granted.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupGrantNotOwned()
    throws Exception
  {
    final var admin =
      this.serverCreateAdminInitial("someone", "12345678");
    final var user0 =
      this.serverCreateUser(admin, "some0");
    final var user1 =
      this.serverCreateUser(admin, "some1");

    this.groupCreate(user0, "com.io7m.ex");
    this.groupAddUser(user1, "com.io7m.ex", Set.of(USER_INVITE));

    this.client.login("some1", "12345678", this.serverPublicURI());

    final var ex =
    assertThrows(EIPClientException.class, () -> {
      this.client.groupGrant(new EIGroupName("com.io7m.ex"), user1, USER_DISMISS);
    });
    assertTrue(ex.getMessage().contains("USER_DISMISS role"));
  }

  /**
   * Leaving a group works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupLeave()
    throws Exception
  {
    final var admin =
      this.serverCreateAdminInitial("someone", "12345678");
    final var user0 =
      this.serverCreateUser(admin, "some0");
    final var user1 =
      this.serverCreateUser(admin, "some1");

    final var groupName = new EIGroupName("com.io7m.ex");
    this.groupCreate(user0, groupName.value());
    this.groupAddUser(user1, groupName.value(), Set.of(USER_INVITE));

    this.client.login("some1", "12345678", this.serverPublicURI());

    final var self0 = this.client.userSelf();
    assertTrue(self0.groupMembership().containsKey(groupName));

    this.client.groupLeave(groupName);

    final var self1 = this.client.userSelf();
    assertFalse(self1.groupMembership().containsKey(groupName));
  }
}
