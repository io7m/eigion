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
import com.io7m.eigion.model.EIGroupCreationRequestStatusType.Failed;
import com.io7m.eigion.model.EIGroupCreationRequestStatusType.Succeeded;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupCreateBegin;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupCreateCancel;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupCreateReady;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupCreateRequests;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupInvite;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupInviteByName;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupInvitesReceived;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupInvitesSent;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandLogin;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseError;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroupCreateBegin;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroupCreateCancel;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroupCreateReady;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroupCreateRequests;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroupInvite;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroupInvites;
import com.io7m.eigion.server.database.api.EIServerDatabaseGroupsQueriesType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.EIGION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class EIServerPublicAPIGroupsTest extends EIServerContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIServerPublicAPIGroupsTest.class);

  /**
   * Starting a group creation request works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreateBegin()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var adminId =
      this.createAdminInitial("someone", "12345678");

    this.createUserSomeone(adminId);
    this.login();

    {
      final var r =
        this.msgSendPublicBytes(
          "/public/1/0/command",
          this.messagesPublicV1().serialize(
            new EISP1CommandGroupCreateBegin(
              "com.io7m.ex"
            ))
        );
      assertEquals(200, r.statusCode());

      final var response =
        this.msgParsePublic(r, EISP1ResponseGroupCreateBegin.class);

      LOG.debug("group name {}", response.groupName());
      LOG.debug("url        {}", response.location());
      LOG.debug("token      {}", response.token());

      assertEquals("ex.io7m.com", response.location().getHost());
    }
  }

  /**
   * Starting a group creation request fails if too many requests have been
   * made.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreateBeginTooMany()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var adminId =
      this.createAdminInitial("someone", "12345678");

    this.createUserSomeone(adminId);
    this.login();

    {
      final var messages =
        this.messagesPublicV1();
      final var cmd =
        new EISP1CommandGroupCreateBegin("com.io7m.ex");

      final var responses =
        new ArrayList<HttpResponse<byte[]>>();

      responses.add(
        this.msgSendPublicBytes("/public/1/0/command", messages.serialize(cmd))
      );
      responses.add(
        this.msgSendPublicBytes("/public/1/0/command", messages.serialize(cmd))
      );
      responses.add(
        this.msgSendPublicBytes("/public/1/0/command", messages.serialize(cmd))
      );
      responses.add(
        this.msgSendPublicBytes("/public/1/0/command", messages.serialize(cmd))
      );
      responses.add(
        this.msgSendPublicBytes("/public/1/0/command", messages.serialize(cmd))
      );
      responses.add(
        this.msgSendPublicBytes("/public/1/0/command", messages.serialize(cmd))
      );

      final var res0 =
        this.msgParsePublic(responses.get(0), EISP1ResponseGroupCreateBegin.class);
      final var res1 =
        this.msgParsePublic(responses.get(1), EISP1ResponseGroupCreateBegin.class);
      final var res2 =
        this.msgParsePublic(responses.get(2), EISP1ResponseGroupCreateBegin.class);
      final var res3 =
        this.msgParsePublic(responses.get(3), EISP1ResponseGroupCreateBegin.class);
      final var res4 =
        this.msgParsePublic(responses.get(4), EISP1ResponseGroupCreateBegin.class);
      final var res5 =
        this.msgParsePublic(responses.get(5), EISP1ResponseError.class);

      assertTrue(res5.message().contains("Too many"));
    }
  }

  /**
   * Group creation requests can be listed.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreateRequests()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var adminId =
      this.createAdminInitial("someone", "12345678");

    this.createUserSomeone(adminId);
    this.login();

    {
      final var messages =
        this.messagesPublicV1();
      final var cmd =
        new EISP1CommandGroupCreateBegin("com.io7m.ex");
      final var responses =
        new ArrayList<HttpResponse<byte[]>>();

      responses.add(
        this.msgSendPublicBytes("/public/1/0/command", messages.serialize(cmd))
      );
      responses.add(
        this.msgSendPublicBytes("/public/1/0/command", messages.serialize(cmd))
      );
      responses.add(
        this.msgSendPublicBytes("/public/1/0/command", messages.serialize(cmd))
      );

      final var res0 =
        this.msgParsePublic(responses.get(0), EISP1ResponseGroupCreateBegin.class);
      final var res1 =
        this.msgParsePublic(responses.get(1), EISP1ResponseGroupCreateBegin.class);
      final var res2 =
        this.msgParsePublic(responses.get(2), EISP1ResponseGroupCreateBegin.class);

      final var listR =
        this.msgSendPublicBytes(
          "/public/1/0/command",
          messages.serialize(new EISP1CommandGroupCreateRequests()));

      final var list =
        this.msgParsePublic(listR, EISP1ResponseGroupCreateRequests.class);

      assertEquals(3, list.requests().size());
      assertEquals(res0.token(), list.requests().get(0).token());
      assertEquals(res1.token(), list.requests().get(1).token());
      assertEquals(res2.token(), list.requests().get(2).token());
    }
  }

  /**
   * Group creation requests can be cancelled.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreateCancel()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var adminId =
      this.createAdminInitial("someone", "12345678");

    this.createUserSomeone(adminId);
    this.login();

    {
      final var messages =
        this.messagesPublicV1();

      final var r0 =
        this.msgParsePublic(
          this.msgSendPublicBytes(
            "/public/1/0/command",
            messages.serialize(new EISP1CommandGroupCreateBegin("com.io7m.ex"))),
          EISP1ResponseGroupCreateBegin.class
        );

      final var r1 =
        this.msgParsePublic(
          this.msgSendPublicBytes(
            "/public/1/0/command",
            messages.serialize(new EISP1CommandGroupCreateCancel(r0.token()))),
          EISP1ResponseGroupCreateCancel.class
        );

      final var r2 =
        this.msgParsePublic(
          this.msgSendPublicBytes(
            "/public/1/0/command",
            messages.serialize(new EISP1CommandGroupCreateRequests())),
          EISP1ResponseGroupCreateRequests.class
        );

      assertEquals(1, r2.requests().size());
      assertEquals(r0.token(), r2.requests().get(0).token());
      assertEquals("CANCELLED", r2.requests().get(0).status());
    }
  }

  /**
   * Groups are created if domain checking succeeds.
   *
   * @throws Exception On errors
   */

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  public void testGroupCreateReadySucceeds()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var adminId =
      this.createAdminInitial("someone", "12345678");
    final var userId =
      this.createUserSomeone(adminId);

    this.login();

    final var messages =
      this.messagesPublicV1();

    /*
     * Start creation...
     */

    final var begin0 =
      this.msgParsePublic(
        this.msgSendPublicBytes(
          "/public/1/0/command",
          messages.serialize(new EISP1CommandGroupCreateBegin("com.io7m.ex"))),
        EISP1ResponseGroupCreateBegin.class
      );

    /*
     * Enqueue a "success" result in the domain checker, and then tell the
     * server that the domain is ready for checking.
     */

    this.domainCheckers().enqueue(CompletableFuture.completedFuture(
      new EIGroupCreationRequest(
        new EIGroupName(begin0.groupName()),
        userId,
        new EIToken(begin0.token()),
        new Succeeded(timeNow(), timeNow())
      )
    ));

    final var ready0 =
      this.msgParsePublic(
        this.msgSendPublicBytes(
          "/public/1/0/command",
          messages.serialize(new EISP1CommandGroupCreateReady(begin0.token()))),
        EISP1ResponseGroupCreateReady.class
      );

    /*
     * Wait until the server reports that the creation was successful...
     */

    while (true) {
      final var requests =
        this.msgParsePublic(
          this.msgSendPublicBytes(
            "/public/1/0/command",
            messages.serialize(new EISP1CommandGroupCreateRequests())),
          EISP1ResponseGroupCreateRequests.class
        );

      final var request = requests.requests().get(0);
      if (!Objects.equals(request.status(), "IN_PROGRESS")) {
        assertEquals("SUCCEEDED", request.status());
        break;
      }

      Thread.sleep(100L);
    }

    /*
     * Now check that the group exists.
     */

    final var database = this.databases().mostRecent();
    try (var c = database.openConnection(EIGION)) {
      try (var t = c.openTransaction()) {
        final var q =
          t.queries(EIServerDatabaseGroupsQueriesType.class);
        assertTrue(q.groupExists(new EIGroupName("com.io7m.ex")));
      }
    }
  }

  /**
   * Groups are not created if domain checking fails.
   *
   * @throws Exception On errors
   */

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  public void testGroupCreateReadyFails()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var adminId =
      this.createAdminInitial("someone", "12345678");
    final var userId =
      this.createUserSomeone(adminId);

    this.login();

    final var messages =
      this.messagesPublicV1();

    /*
     * Start creation...
     */

    final var begin0 =
      this.msgParsePublic(
        this.msgSendPublicBytes(
          "/public/1/0/command",
          messages.serialize(new EISP1CommandGroupCreateBegin("com.io7m.ex"))),
        EISP1ResponseGroupCreateBegin.class
      );

    /*
     * Enqueue a "failed" result in the domain checker, and then tell the
     * server that the domain is ready for checking.
     */

    this.domainCheckers().enqueue(CompletableFuture.completedFuture(
      new EIGroupCreationRequest(
        new EIGroupName(begin0.groupName()),
        userId,
        new EIToken(begin0.token()),
        new Failed(timeNow(), timeNow(), "No!")
      )
    ));

    final var ready0 =
      this.msgParsePublic(
        this.msgSendPublicBytes(
          "/public/1/0/command",
          messages.serialize(new EISP1CommandGroupCreateReady(begin0.token()))),
        EISP1ResponseGroupCreateReady.class
      );

    /*
     * Wait until the server reports that the creation has failed...
     */

    while (true) {
      final var requests =
        this.msgParsePublic(
          this.msgSendPublicBytes(
            "/public/1/0/command",
            messages.serialize(new EISP1CommandGroupCreateRequests())),
          EISP1ResponseGroupCreateRequests.class
        );

      final var request = requests.requests().get(0);
      if (!Objects.equals(request.status(), "IN_PROGRESS")) {
        assertEquals("FAILED", request.status());
        break;
      }

      Thread.sleep(100L);
    }

    /*
     * Now check that the group exists.
     */

    final var database = this.databases().mostRecent();
    try (var c = database.openConnection(EIGION)) {
      try (var t = c.openTransaction()) {
        final var q =
          t.queries(EIServerDatabaseGroupsQueriesType.class);
        assertFalse(q.groupExists(new EIGroupName("com.io7m.ex")));
      }
    }
  }

  /**
   * Domain checking doesn't proceed for nonexistent requests.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreateReadyNonexistent()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var adminId =
      this.createAdminInitial("someone", "12345678");
    final var userId =
      this.createUserSomeone(adminId);

    this.login();

    final var messages =
      this.messagesPublicV1();

    final var ready0 =
      this.msgSendPublicBytes(
        "/public/1/0/command",
        messages.serialize(new EISP1CommandGroupCreateReady("A0")));

    assertEquals(404, ready0.statusCode());
  }

  /**
   * Groups are not created if domain checking fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreateReadyWrongUser()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var adminId =
      this.createAdminInitial("someone", "12345678");
    final var user0 =
      this.createUser(adminId, "someone0");
    final var user1 =
      this.createUser(adminId, "someone1");

    this.loginFor("someone0", "12345678");

    final var messages =
      this.messagesPublicV1();

    /*
     * Start creation...
     */

    final var begin0 =
      this.msgParsePublic(
        this.msgSendPublicBytes(
          "/public/1/0/command",
          messages.serialize(new EISP1CommandGroupCreateBegin("com.io7m.ex"))),
        EISP1ResponseGroupCreateBegin.class
      );

    /*
     * Login as someone else, and try to "ready" the token as that user.
     */

    this.loginFor("someone1", "12345678");

    final var r =
      this.msgSendPublicBytes(
        "/public/1/0/command",
        messages.serialize(new EISP1CommandGroupCreateReady(begin0.token())));

    assertEquals(403, r.statusCode());
  }

  /**
   * Cancelled requests cannot be made ready.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreateReadyCancelled()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var adminId =
      this.createAdminInitial("someone", "12345678");
    final var userId =
      this.createUserSomeone(adminId);

    this.login();

    final var messages =
      this.messagesPublicV1();

    /*
     * Start creation...
     */

    final var begin0 =
      this.msgParsePublic(
        this.msgSendPublicBytes(
          "/public/1/0/command",
          messages.serialize(new EISP1CommandGroupCreateBegin("com.io7m.ex"))),
        EISP1ResponseGroupCreateBegin.class
      );

    /*
     * Cancel the creation...
     */

    final var cancel0 =
      this.msgParsePublic(
        this.msgSendPublicBytes(
          "/public/1/0/command",
          messages.serialize(new EISP1CommandGroupCreateCancel(begin0.token()))),
        EISP1ResponseGroupCreateCancel.class
      );

    /*
     * Now try to make the request ready...
     */

    final var r =
      this.msgSendPublicBytes(
        "/public/1/0/command",
        messages.serialize(new EISP1CommandGroupCreateReady(begin0.token())));

    assertEquals(400, r.statusCode());
  }

  /**
   * Users see group invites as is appropriate.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupInviteVisible()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var adminId =
      this.createAdminInitial("someone", "12345678");
    final var user0 =
      this.createUser(adminId, "someone0");
    final var user1 =
      this.createUser(adminId, "someone1");
    final var group0 =
      this.createGroup(user0, "com.io7m.ex");

    this.loginFor("someone0", "12345678");

    final var invite =
      this.msgSendPublicCommandOrFail(
        new EISP1CommandGroupInvite("com.io7m.ex", user1),
        EISP1ResponseGroupInvite.class
      );

    {
      final var invites =
        this.msgSendPublicCommandOrFail(
          new EISP1CommandGroupInvitesSent(),
          EISP1ResponseGroupInvites.class
        );

      final var inviteList = invites.invites();
      assertEquals(invite.token(), inviteList.get(0).token());
      assertEquals(1, inviteList.size());
    }

    {
      final var invites =
        this.msgSendPublicCommandOrFail(
          new EISP1CommandGroupInvitesReceived(),
          EISP1ResponseGroupInvites.class
        );

      final var inviteList = invites.invites();
      assertEquals(0, inviteList.size());
    }

    this.loginFor("someone1", "12345678");

    {
      final var invites =
        this.msgSendPublicCommandOrFail(
          new EISP1CommandGroupInvitesReceived(),
          EISP1ResponseGroupInvites.class
        );

      final var inviteList = invites.invites();
      assertEquals(invite.token(), inviteList.get(0).token());
      assertEquals(1, inviteList.size());
    }

    {
      final var invites =
        this.msgSendPublicCommandOrFail(
          new EISP1CommandGroupInvitesSent(),
          EISP1ResponseGroupInvites.class
        );

      final var inviteList = invites.invites();
      assertEquals(0, inviteList.size());
    }
  }

  /**
   * Users see group invites as is appropriate.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupInviteByNameVisible()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var adminId =
      this.createAdminInitial("someone", "12345678");
    final var user0 =
      this.createUser(adminId, "someone0");
    final var user1 =
      this.createUser(adminId, "someone1");
    final var group0 =
      this.createGroup(user0, "com.io7m.ex");

    this.loginFor("someone0", "12345678");

    final var invite =
      this.msgSendPublicCommandOrFail(
        new EISP1CommandGroupInviteByName("com.io7m.ex", "someone1"),
        EISP1ResponseGroupInvite.class
      );

    {
      final var invites =
        this.msgSendPublicCommandOrFail(
          new EISP1CommandGroupInvitesSent(),
          EISP1ResponseGroupInvites.class
        );

      final var inviteList = invites.invites();
      assertEquals(invite.token(), inviteList.get(0).token());
      assertEquals(1, inviteList.size());
    }

    {
      final var invites =
        this.msgSendPublicCommandOrFail(
          new EISP1CommandGroupInvitesReceived(),
          EISP1ResponseGroupInvites.class
        );

      final var inviteList = invites.invites();
      assertEquals(0, inviteList.size());
    }

    this.loginFor("someone1", "12345678");

    {
      final var invites =
        this.msgSendPublicCommandOrFail(
          new EISP1CommandGroupInvitesReceived(),
          EISP1ResponseGroupInvites.class
        );

      final var inviteList = invites.invites();
      assertEquals(invite.token(), inviteList.get(0).token());
      assertEquals(1, inviteList.size());
    }

    {
      final var invites =
        this.msgSendPublicCommandOrFail(
          new EISP1CommandGroupInvitesSent(),
          EISP1ResponseGroupInvites.class
        );

      final var inviteList = invites.invites();
      assertEquals(0, inviteList.size());
    }
  }

  /**
   * Users cannot invite into groups of which they aren't a member.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupInviteNotInGroup()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var adminId =
      this.createAdminInitial("someone", "12345678");
    final var user0 =
      this.createUser(adminId, "someone0");
    final var user1 =
      this.createUser(adminId, "someone1");
    final var group0 =
      this.createGroup(user0, "com.io7m.ex");

    this.loginFor("someone1", "12345678");

    final var r =
      this.msgSendPublicCommand(
        new EISP1CommandGroupInvite("com.io7m.ex", user0)
      );

    assertEquals(403, r.statusCode());
  }

  /**
   * Sending too many invites is denied.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupInviteTooMany()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var adminId =
      this.createAdminInitial("someone", "12345678");
    final var user0 =
      this.createUser(adminId, "someone0");
    final var user1 =
      this.createUser(adminId, "someone1");

    final var groups =
      IntStream.range(0, 6)
        .mapToObj(i -> "com.io7m.ex0" + i)
        .map(EIGroupName::new)
        .toList();

    for (final var group : groups) {
      this.createGroup(user0, group.value());
    }

    this.loginFor("someone0", "12345678");

    for (int index = 0; index < groups.size() - 1; ++index) {
      final var group = groups.get(index);
      this.msgSendPublicCommandOrFail(
        new EISP1CommandGroupInviteByName(group.value(), "someone1"),
        EISP1ResponseGroupInvite.class
      );
    }

    final var r =
      this.msgSendPublicCommand(
        new EISP1CommandGroupInviteByName(
          groups.get(groups.size() - 1).value(),
          "someone1"));
    assertEquals(403, r.statusCode());
  }

  /**
   * Users cannot invite users without sufficient roles.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupInviteMissingRole()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var adminId =
      this.createAdminInitial("someone", "12345678");
    final var user0 =
      this.createUser(adminId, "someone0");
    final var user1 =
      this.createUser(adminId, "someone1");
    final var user2 =
      this.createUser(adminId, "someone2");
    final var group0 =
      this.createGroup(user0, "com.io7m.ex");

    this.setGroupRoles(user0, "com.io7m.ex", Set.of());
    this.loginFor("someone0", "12345678");

    final var r =
      this.msgSendPublicCommand(
        new EISP1CommandGroupInvite("com.io7m.ex", user1)
      );

    assertEquals(403, r.statusCode());
  }

  private void loginFor(
    final String user,
    final String pass)
    throws Exception
  {
    final var r =
      this.msgSendPublicBytes(
        "/public/1/0/login",
        this.messagesPublicV1().serialize(
          new EISP1CommandLogin(user, pass))
      );
    assertEquals(200, r.statusCode());
  }

  private void login()
    throws Exception
  {
    this.loginFor("someone", "12345678");
  }
}
