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

import com.io7m.eigion.protocol.versions.EISVProtocols;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class EIServerAdminAPITransactionTest extends EIServerContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIServerAdminAPITransactionTest.class);

  /**
   * Touching the base URL works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGetBase()
    throws Exception
  {
    assertTrue(this.container().isRunning());
    this.server().start();

    final var response =
      this.getAdmin("/admin/1/0");

    assertEquals(200, response.statusCode());

    final var message =
      this.parseV(response, EISVProtocols.class);
  }

  /**
   * If a command fails in a transaction, it's as if none of the commands
   * executed.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTransactionUserCreateFailsAtomically()
    throws Exception
  {
    assertTrue(this.container().isRunning());
    this.server().start();

    this.createAdminInitial("someone", "12345678");
    this.doLoginAdmin("someone", "12345678");

    final var rCreate =
      this.postAdminText("/admin/1/0/transaction", """
{
  "%Type": "Transaction",
  "%Schema":"https://www.io7m.com/eigion/admin-1.json",
  "Commands": [
    {
      "%Type": "CommandUserCreate",
      "Name": "someone-3",
      "Email": "someone-3@example.com",
      "Password": {
        "Algorithm": "PBKDF2WithHmacSHA256:10000:256",
        "Hash": "094D4284F4F6B65760AAA85E5720E084FD4F14F9A654F551F47651F16463FA09",
        "Salt": "10203040"
      }
    },
    {
      "%Type": "CommandUserCreate",
      "Name": "someone-3",
      "Email": "someone-3@example.com",
      "Password": {
        "Algorithm": "PBKDF2WithHmacSHA256:10000:256",
        "Hash": "094D4284F4F6B65760AAA85E5720E084FD4F14F9A654F551F47651F16463FA09",
        "Salt": "10203040"
      }
    }
  ]
}
""");

    assertEquals(400, rCreate.statusCode());

    final var rGet =
      this.postAdminText("/admin/1/0/command", """
{
  "%Type": "CommandUserGetByName",
  "Name": "someone-3"
}
""");

    assertEquals(404, rGet.statusCode());
  }

  /**
   * If a command fails in a transaction, it's as if none of the commands
   * executed.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTransactionUserCreate()
    throws Exception
  {
    assertTrue(this.container().isRunning());
    this.server().start();

    this.createAdminInitial("someone", "12345678");
    this.doLoginAdmin("someone", "12345678");

    final var rCreate =
      this.postAdminText("/admin/1/0/transaction", """
{
  "%Type": "Transaction",
  "%Schema":"https://www.io7m.com/eigion/admin-1.json",
  "Commands": [
    {
      "%Type": "CommandUserCreate",
      "Name": "someone-3",
      "Email": "someone-3@example.com",
      "Password": {
        "Algorithm": "PBKDF2WithHmacSHA256:10000:256",
        "Hash": "094D4284F4F6B65760AAA85E5720E084FD4F14F9A654F551F47651F16463FA09",
        "Salt": "10203040"
      }
    }
  ]
}
""");

    assertEquals(200, rCreate.statusCode());

    final var rGet =
      this.postAdminText("/admin/1/0/command", """
{
  "%Type": "CommandUserGetByName",
  "Name": "someone-3"
}
""");

    assertEquals(200, rGet.statusCode());
  }
}
