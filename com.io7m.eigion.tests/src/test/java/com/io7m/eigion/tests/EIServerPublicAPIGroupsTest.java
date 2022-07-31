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

import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupCreateBegin;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupCreateCancel;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupCreateRequests;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandLogin;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseError;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroupCreateBegin;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroupCreateCancel;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroupCreateRequests;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.http.HttpResponse;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    assertTrue(this.container().isRunning());
    this.server().start();

    final var adminId =
      this.createAdminInitial("someone", "12345678");

    this.createUserSomeone(adminId);
    this.login();

    {
      final var r =
        this.postPublicBytes(
          "/public/1/0/command",
          this.messagesPublicV1().serialize(
            new EISP1CommandGroupCreateBegin(
              "com.io7m.ex"
            ))
        );
      assertEquals(200, r.statusCode());

      final var response =
        this.parsePublic(r, EISP1ResponseGroupCreateBegin.class);

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
    assertTrue(this.container().isRunning());
    this.server().start();

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
        this.postPublicBytes("/public/1/0/command", messages.serialize(cmd))
      );
      responses.add(
        this.postPublicBytes("/public/1/0/command", messages.serialize(cmd))
      );
      responses.add(
        this.postPublicBytes("/public/1/0/command", messages.serialize(cmd))
      );
      responses.add(
        this.postPublicBytes("/public/1/0/command", messages.serialize(cmd))
      );
      responses.add(
        this.postPublicBytes("/public/1/0/command", messages.serialize(cmd))
      );
      responses.add(
        this.postPublicBytes("/public/1/0/command", messages.serialize(cmd))
      );

      final var res0 =
        this.parsePublic(responses.get(0), EISP1ResponseGroupCreateBegin.class);
      final var res1 =
        this.parsePublic(responses.get(1), EISP1ResponseGroupCreateBegin.class);
      final var res2 =
        this.parsePublic(responses.get(2), EISP1ResponseGroupCreateBegin.class);
      final var res3 =
        this.parsePublic(responses.get(3), EISP1ResponseGroupCreateBegin.class);
      final var res4 =
        this.parsePublic(responses.get(4), EISP1ResponseGroupCreateBegin.class);
      final var res5 =
        this.parsePublic(responses.get(5), EISP1ResponseError.class);

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
    assertTrue(this.container().isRunning());
    this.server().start();

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
        this.postPublicBytes("/public/1/0/command", messages.serialize(cmd))
      );
      responses.add(
        this.postPublicBytes("/public/1/0/command", messages.serialize(cmd))
      );
      responses.add(
        this.postPublicBytes("/public/1/0/command", messages.serialize(cmd))
      );

      final var res0 =
        this.parsePublic(responses.get(0), EISP1ResponseGroupCreateBegin.class);
      final var res1 =
        this.parsePublic(responses.get(1), EISP1ResponseGroupCreateBegin.class);
      final var res2 =
        this.parsePublic(responses.get(2), EISP1ResponseGroupCreateBegin.class);

      final var listR =
        this.postPublicBytes(
          "/public/1/0/command",
          messages.serialize(new EISP1CommandGroupCreateRequests()));

      final var list =
        this.parsePublic(listR, EISP1ResponseGroupCreateRequests.class);

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
    assertTrue(this.container().isRunning());
    this.server().start();

    final var adminId =
      this.createAdminInitial("someone", "12345678");

    this.createUserSomeone(adminId);
    this.login();

    {
      final var messages =
        this.messagesPublicV1();

      final var r0 =
        this.parsePublic(
          this.postPublicBytes(
            "/public/1/0/command",
            messages.serialize(new EISP1CommandGroupCreateBegin("com.io7m.ex"))),
          EISP1ResponseGroupCreateBegin.class
        );

      final var r1 =
        this.parsePublic(
          this.postPublicBytes(
            "/public/1/0/command",
            messages.serialize(new EISP1CommandGroupCreateCancel(r0.token()))),
          EISP1ResponseGroupCreateCancel.class
        );

      final var r2 =
        this.parsePublic(
          this.postPublicBytes(
            "/public/1/0/command",
            messages.serialize(new EISP1CommandGroupCreateRequests())),
          EISP1ResponseGroupCreateRequests.class
        );

      assertEquals(1, r2.requests().size());
      assertEquals(r0.token(), r2.requests().get(0).token());
      assertEquals("CANCELLED", r2.requests().get(0).status());
    }
  }

  private void login()
    throws Exception
  {
    final var r =
      this.postPublicBytes(
        "/public/1/0/login",
        this.messagesPublicV1().serialize(
          new EISP1CommandLogin("someone", "12345678"))
      );
    assertEquals(200, r.statusCode());
  }
}
