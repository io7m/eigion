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

import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseServiceList;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class EIServerAdminAPIServicesTest extends EIServerContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIServerAdminAPIServicesTest.class);

  /**
   * Getting services works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGetServices()
    throws Exception
  {
    assertTrue(this.container().isRunning());
    this.server().start();

    this.createAdminInitial("someone", "12345678");
    this.doLoginAdmin("someone", "12345678");

    final var rCreate =
      this.postAdminText("/admin/1/0/command", """
{
  "%Type": "CommandServicesList"
}""");

    assertEquals(200, rCreate.statusCode());

    final var rm =
      this.parseAdmin(rCreate, EISA1ResponseServiceList.class);

    assertNotEquals(0, rm.services().size());
  }
}
