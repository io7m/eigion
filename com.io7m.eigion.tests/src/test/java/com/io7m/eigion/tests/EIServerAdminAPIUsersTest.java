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

import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseUserCreate;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseUserGet;
import com.io7m.eigion.protocol.versions.EISVProtocols;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class EIServerAdminAPIUsersTest extends EIServerContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIServerAdminAPIUsersTest.class);

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
   * Creating users works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCreateUser()
    throws Exception
  {
    assertTrue(this.container().isRunning());
    this.server().start();

    this.createAdminInitial("someone", "12345678");
    this.doLoginAdmin("someone", "12345678");

    final var rCreate =
      this.postAdminText("/admin/1/0/command", """
        {
          "%Type": "CommandUserCreate",
          "Name": "someone-3",
          "Email": "someone-3@example.com",
          "Password": {
            "Algorithm": "PBKDF2WithHmacSHA256:10000:256",
            "Hash": "094D4284F4F6B65760AAA85E5720E084FD4F14F9A654F551F47651F16463FA09",
            "Salt": "10203040"
          }
        }""");

    assertEquals(200, rCreate.statusCode());

    final var rm =
      this.parseAdmin(rCreate, EISA1ResponseUserCreate.class);

    final var rGet =
      this.postAdminText("/admin/1/0/command", """
        {
          "%s": "CommandUserGet",
          "ID": "%s"
        }""".formatted("%Type", rm.user().id()));

    final var rg =
      this.parseAdmin(rGet, EISA1ResponseUserGet.class);

    final var rgu = rg.user();
    final var rgp = rgu.password();
    assertEquals("someone-3", rgu.name());
    assertEquals("someone-3@example.com", rgu.email());
    assertEquals(Optional.empty(), rgu.ban());
    assertEquals("PBKDF2WithHmacSHA256:10000:256", rgp.algorithm());
    assertEquals(
      "094D4284F4F6B65760AAA85E5720E084FD4F14F9A654F551F47651F16463FA09",
      rgp.hash());
    assertEquals("10203040", rgp.salt());
  }

  /**
   * Garbage is rejected.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCreateUserGarbage0()
    throws Exception
  {
    assertTrue(this.container().isRunning());
    this.server().start();

    this.createAdminInitial("someone", "12345678");
    this.doLoginAdmin("someone", "12345678");

    final var rCreate =
      this.postAdminText("/admin/1/0/command", """
        {
          "%Type": "CommandUserCreate",
          "Name": "someone-3",
          "Email": "someone-3@example.com",
          "Password": {
            "What": "What?"
          }
        }""");

    assertEquals(400, rCreate.statusCode());
  }

  /**
   * Garbage is rejected.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCreateUserGarbage1()
    throws Exception
  {
    assertTrue(this.container().isRunning());
    this.server().start();

    this.createAdminInitial("someone", "12345678");
    this.doLoginAdmin("someone", "12345678");

    final var rCreate =
      this.postAdminText("/admin/1/0/command", """
        {
          "%Type": "ResponseServiceList",
          "RequestID": "ab69efc8-31f4-4460-bb57-3351c5110c01",
          "Services": []
        }""");

    assertEquals(400, rCreate.statusCode());
  }

  /**
   * Garbage is rejected.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCreateUserGarbage2()
    throws Exception
  {
    assertTrue(this.container().isRunning());
    this.server().start();

    this.createAdminInitial("someone", "12345678");
    this.doLoginAdmin("someone", "12345678");

    final var rCreate =
      this.postAdminText("/admin/1/0/command", """
        {
          "%Type": "CommandUserCreate",
          "Name": "someone-3",
          "Email": "someone-3@example.com",
          "Password": {
            "Algorithm": "UnknownAlgorithm",
            "Hash": "094D4284F4F6B65760AAA85E5720E084FD4F14F9A654F551F47651F16463FA09",
            "Salt": "10203040"
          }
        }""");

    assertEquals(400, rCreate.statusCode());
  }

  /**
   * Nonexistent users don't exist.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGetUserNonexistent()
    throws Exception
  {
    assertTrue(this.container().isRunning());
    this.server().start();

    this.createAdminInitial("someone", "12345678");
    this.doLoginAdmin("someone", "12345678");

    final var rGet =
      this.postAdminText("/admin/1/0/command", """
        {
          "%Type": "CommandUserGet",
          "ID": "77b8f5db-9f5b-409b-a4c0-4d089e5a5dd8"
        }""");

    assertEquals(404, rGet.statusCode());
  }
}
