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

import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseAdminCreate;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseAdminGet;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseError;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;

import static com.io7m.eigion.model.EIAdminPermission.ADMIN_CREATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class EIServerAdminAPIAdminTest extends EIServerContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIServerAdminAPIAdminTest.class);

  /**
   * Creating admins works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCreateAdmin()
    throws Exception
  {
    assertTrue(this.container().isRunning());
    this.server().start();

    this.createAdminInitial("someone", "12345678");
    this.doLoginAdmin("someone", "12345678");

    final var rCreate =
      this.postAdminText("/admin/1/0/command", """
        {
          "%Type": "CommandAdminCreate",
          "Name": "someone-3",
          "Email": "someone-3@example.com",
          "Password": {
            "Algorithm": "PBKDF2WithHmacSHA256:10000:256",
            "Hash": "094D4284F4F6B65760AAA85E5720E084FD4F14F9A654F551F47651F16463FA09",
            "Salt": "10203040"
          },
          "Permissions": [
            "USER_WRITE"
          ]
        }""");

    assertEquals(200, rCreate.statusCode());

    final var rm =
      this.parseAdmin(rCreate, EISA1ResponseAdminCreate.class);

    {
      final var rGet =
        this.postAdminText("/admin/1/0/command", """
          {
            "%s": "CommandAdminGet",
            "ID": "%s"
          }""".formatted("%Type", rm.admin().id()));

      final var rg =
        this.parseAdmin(rGet, EISA1ResponseAdminGet.class);

      final var rgu = rg.admin();
      final var rgp = rgu.password();
      assertEquals("someone-3", rgu.name());
      assertEquals("someone-3@example.com", rgu.email());
      assertEquals("PBKDF2WithHmacSHA256:10000:256", rgp.algorithm());
      assertEquals(
        "094D4284F4F6B65760AAA85E5720E084FD4F14F9A654F551F47651F16463FA09",
        rgp.hash());
      assertEquals("10203040", rgp.salt());
    }

    {
      final var rGet =
        this.postAdminText("/admin/1/0/command", """
          {
            "%s": "CommandAdminGetByName",
            "Name": "%s"
          }""".formatted("%Type", rm.admin().name()));

      final var rg =
        this.parseAdmin(rGet, EISA1ResponseAdminGet.class);

      final var rgu = rg.admin();
      final var rgp = rgu.password();
      assertEquals("someone-3", rgu.name());
      assertEquals("someone-3@example.com", rgu.email());
      assertEquals("PBKDF2WithHmacSHA256:10000:256", rgp.algorithm());
      assertEquals(
        "094D4284F4F6B65760AAA85E5720E084FD4F14F9A654F551F47651F16463FA09",
        rgp.hash());
      assertEquals("10203040", rgp.salt());
    }

    {
      final var rGet =
        this.postAdminText("/admin/1/0/command", """
          {
            "%s": "CommandAdminGetByEmail",
            "Email": "%s"
          }""".formatted("%Type", rm.admin().email()));

      final var rg =
        this.parseAdmin(rGet, EISA1ResponseAdminGet.class);

      final var rgu = rg.admin();
      final var rgp = rgu.password();
      assertEquals("someone-3", rgu.name());
      assertEquals("someone-3@example.com", rgu.email());
      assertEquals("PBKDF2WithHmacSHA256:10000:256", rgp.algorithm());
      assertEquals(
        "094D4284F4F6B65760AAA85E5720E084FD4F14F9A654F551F47651F16463FA09",
        rgp.hash());
      assertEquals("10203040", rgp.salt());
    }
  }


  /**
   * An admin can't be created with a duplicate name.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCreateAdminConflict()
    throws Exception
  {
    assertTrue(this.container().isRunning());
    this.server().start();

    this.createAdminInitial("someone", "12345678");
    this.doLoginAdmin("someone", "12345678");

    final var rCreate =
      this.postAdminText("/admin/1/0/command", """
        {
          "%Type": "CommandAdminCreate",
          "Name": "someone",
          "Email": "someone-else@example.com",
          "Password": {
            "Algorithm": "PBKDF2WithHmacSHA256:10000:256",
            "Hash": "094D4284F4F6B65760AAA85E5720E084FD4F14F9A654F551F47651F16463FA09",
            "Salt": "10203040"
          },
          "Permissions": [
            "USER_WRITE"
          ]
        }""");

    assertEquals(400, rCreate.statusCode());

    final var rm =
      this.parseAdmin(rCreate, EISA1ResponseError.class);
  }

  /**
   * Creating admins without the right permission fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCreateAdminNoPermission()
    throws Exception
  {
    assertTrue(this.container().isRunning());
    this.server().start();

    final var id =
      this.createAdminInitial("someone", "12345678");

    this.createAdmin(id, "someone-else", "12345678", Set.of());
    this.doLoginAdmin("someone-else", "12345678");

    final var rCreate =
      this.postAdminText("/admin/1/0/command", """
        {
          "%Type": "CommandAdminCreate",
          "Name": "someone-else2",
          "Email": "someone-else2@example.com",
          "Password": {
            "Algorithm": "PBKDF2WithHmacSHA256:10000:256",
            "Hash": "094D4284F4F6B65760AAA85E5720E084FD4F14F9A654F551F47651F16463FA09",
            "Salt": "10203040"
          },
          "Permissions": [
            "USER_WRITE"
          ]
        }""");

    assertEquals(403, rCreate.statusCode());

    final var rm =
      this.parseAdmin(rCreate, EISA1ResponseError.class);

    assertEquals("You do not have the ADMIN_CREATE permission.", rm.message());
  }

  /**
   * Creating an admin that has more permissions than the creating admin is not
   * allowed.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCreateAdminNoElevation()
    throws Exception
  {
    assertTrue(this.container().isRunning());
    this.server().start();

    final var id =
      this.createAdminInitial("someone", "12345678");

    this.createAdmin(id, "someone-else", "12345678", Set.of(ADMIN_CREATE));
    this.doLoginAdmin("someone-else", "12345678");

    final var rCreate =
      this.postAdminText("/admin/1/0/command", """
        {
          "%Type": "CommandAdminCreate",
          "Name": "someone-else2",
          "Email": "someone-else2@example.com",
          "Password": {
            "Algorithm": "PBKDF2WithHmacSHA256:10000:256",
            "Hash": "094D4284F4F6B65760AAA85E5720E084FD4F14F9A654F551F47651F16463FA09",
            "Salt": "10203040"
          },
          "Permissions": [
            "USER_WRITE"
          ]
        }""");

    assertEquals(403, rCreate.statusCode());

    final var rm =
      this.parseAdmin(rCreate, EISA1ResponseError.class);

    assertEquals("An admin cannot be created with more permissions than the creating admin.", rm.message());
  }

  /**
   * Reading admins fails without permissions.
   *
   * @throws Exception On errors
   */

  @Test
  public void testReadAdminNoPermission()
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
          "%Type": "CommandAdminGet",
          "ID": "77b8f5db-9f5b-409b-a4c0-4d089e5a5dd8"
        }""");

    assertEquals(403, rGet.statusCode());

    final var rm =
      this.parseAdmin(rGet, EISA1ResponseError.class);

    assertEquals("You do not have the ADMIN_READ permission.", rm.message());
  }

  /**
   * Reading admins fails without permissions.
   *
   * @throws Exception On errors
   */

  @Test
  public void testReadAdminEmailNoPermission()
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
          "%Type": "CommandAdminGetByEmail",
          "Email": "someone@example.com"
        }""");

    assertEquals(403, rGet.statusCode());

    final var rm =
      this.parseAdmin(rGet, EISA1ResponseError.class);

    assertEquals("You do not have the ADMIN_READ permission.", rm.message());
  }

  /**
   * Reading admins fails without permissions.
   *
   * @throws Exception On errors
   */

  @Test
  public void testReadAdminNameNoPermission()
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
          "%Type": "CommandAdminGetByName",
          "Name": "someone"
        }""");

    assertEquals(403, rGet.statusCode());

    final var rm =
      this.parseAdmin(rGet, EISA1ResponseError.class);

    assertEquals("You do not have the ADMIN_READ permission.", rm.message());
  }

  /**
   * Searching admins fails without permissions.
   *
   * @throws Exception On errors
   */

  @Test
  public void testReadAdminSearchNoPermission()
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
          "%Type": "CommandAdminSearch",
          "Query": "someone"
        }""");

    assertEquals(403, rGet.statusCode());

    final var rm =
      this.parseAdmin(rGet, EISA1ResponseError.class);

    assertEquals("You do not have the ADMIN_READ permission.", rm.message());
  }
}
