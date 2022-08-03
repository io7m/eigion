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

import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseError;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
public final class EIServerAdminAPIGroupsTest extends EIServerContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIServerAdminAPIGroupsTest.class);

  /**
   * Searching group invites fails without permissions.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupInviteReadNoPermission()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var id =
      this.createAdminInitial("someone", "12345678");
    this.createAdmin(id, "someone-else", "12345678", Set.of());

    this.doLoginAdmin("someone-else", "12345678");

    final var rGet =
      this.msgSendAdminText("/admin/1/0/command", """
        {
          "%Type": "CommandGroupInvites",
          "Since": "1980-01-01T00:00:00+00:00"
        }""");

    assertEquals(403, rGet.statusCode());

    final var rm =
      this.msgParseAdmin(rGet, EISA1ResponseError.class);

    assertEquals("You do not have the GROUP_INVITES_READ permission.", rm.message());
  }

  /**
   * Modifying group invites fails without permissions.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupInviteWriteNoPermission()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var id =
      this.createAdminInitial("someone", "12345678");
    this.createAdmin(id, "someone-else", "12345678", Set.of());

    this.doLoginAdmin("someone-else", "12345678");

    final var rGet =
      this.msgSendAdminText("/admin/1/0/command", """
        {
          "%Type": "CommandGroupInviteSetStatus",
          "Token": "01BA4719C80B6FE911B091A7C05124B64EEECE964E09C0",
          "Status": "CANCELLED"
        }""");

    assertEquals(403, rGet.statusCode());

    final var rm =
      this.msgParseAdmin(rGet, EISA1ResponseError.class);

    assertEquals("You do not have the GROUP_INVITES_WRITE permission.", rm.message());
  }
}
