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

import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseAuditGet;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseError;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class EIServerAdminAPIAuditTest extends EIServerContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIServerAdminAPIAuditTest.class);

  /**
   * Getting audit events works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAuditGet()
    throws Exception
  {
    assertTrue(this.container().isRunning());
    this.server().start();

    this.createAdminInitial("someone", "12345678");
    this.doLoginAdmin("someone", "12345678");

    final var rGet =
      this.postAdminText("/admin/1/0/command", """
{
  "%Type": "CommandAuditGet",
  "FromInclusive": "2022-07-01T00:00:00Z",
  "ToInclusive": "2100-01-01T00:00:00Z",
  "Owners": {
    "Include": "",
    "Exclude": ""
  },
  "Types": {
    "Include": "",
    "Exclude": ""
  },
  "Messages": {
    "Include": "",
    "Exclude": ""
  }
}""");

    assertEquals(200, rGet.statusCode());

    final var rm =
      this.parseAdmin(rGet, EISA1ResponseAuditGet.class);

    assertEquals(2, rm.events().size());
  }

  /**
   * Getting audit events requires permissions.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAuditGetNotPermitted()
    throws Exception
  {
    assertTrue(this.container().isRunning());
    this.server().start();

    final var id =
      this.createAdminInitial("someone", "12345678");

    this.createAdmin(id, "someone-else", "12345678", Set.of());
    this.doLoginAdmin("someone-else", "12345678");

    final var rGet =
      this.postAdminText("/admin/1/0/command", """
{
  "%Type": "CommandAuditGet",
  "FromInclusive": "2022-07-01T00:00:00Z",
  "ToInclusive": "2100-01-01T00:00:00Z",
  "Owners": {
    "Include": "",
    "Exclude": ""
  },
  "Types": {
    "Include": "",
    "Exclude": ""
  },
  "Messages": {
    "Include": "",
    "Exclude": ""
  }
}""");

    assertEquals(403, rGet.statusCode());

    final var rm =
      this.parseAdmin(rGet, EISA1ResponseError.class);

    assertEquals("You do not have the AUDIT_READ permission.", rm.message());
  }
}
