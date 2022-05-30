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

import com.io7m.eigion.server.protocol.api.EIServerProtocolException;
import com.io7m.eigion.server.protocol.public_api.v1.EISP1CommandLogin;
import com.io7m.eigion.server.protocol.public_api.v1.EISP1ResponseError;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.http.HttpResponse;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class EIServerPublicAPILoginTest extends EIServerContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIServerPublicAPILoginTest.class);

  /**
   * Sending garbage fails a login.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginNonsense()
    throws Exception
  {
    assertTrue(this.container().isRunning());
    this.server().start();

    final var response =
      this.postPublicText("/public/1/0/login", "{}");

    assertEquals(400, response.statusCode());

    final var error =
      this.parse(response, EISP1ResponseError.class);

    assertEquals("protocol", error.errorCode());
    assertEquals(0, this.cookies().getCookieStore().getCookies().size());
  }

  /**
   * Logging in works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginOK()
    throws Exception
  {
    assertTrue(this.container().isRunning());
    this.server().start();

    this.createUserSomeone();

    final var response =
      this.postPublicBytes(
        "/public/1/0/login",
        this.messagesV1().serialize(
          new EISP1CommandLogin("someone", "12345678"))
      );

    assertEquals(200, response.statusCode());

    {
      final var c =
        this.cookies().getCookieStore().getCookies().get(0);
      assertEquals("JSESSIONID", c.getName());
      assertEquals("/", c.getPath());
    }
  }

  /**
   * Logging in with a bad user fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginBadUser()
    throws Exception
  {
    assertTrue(this.container().isRunning());
    this.server().start();

    this.createUserSomeone();

    final var response =
      this.postPublicBytes(
        "/public/1/0/login",
        this.messagesV1().serialize(
          new EISP1CommandLogin("someonex", "12345678"))
      );

    assertEquals(401, response.statusCode());

    final var error =
      this.parse(response, EISP1ResponseError.class);

    assertEquals("authentication", error.errorCode());
    assertEquals(0, this.cookies().getCookieStore().getCookies().size());
  }

  private <T> T parse(
    final HttpResponse<byte[]> response,
    final Class<T> clazz)
    throws EIServerProtocolException
  {
    final var bodyText = response.body();
    LOG.debug("received: {}", new String(bodyText, UTF_8));
    return clazz.cast(this.messagesV1().parse(bodyText));
  }

  /**
   * Logging in with a bad password fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginBadPassword()
    throws Exception
  {
    assertTrue(this.container().isRunning());
    this.server().start();

    this.createUserSomeone();

    final var response =
      this.postPublicBytes(
        "/public/1/0/login",
        this.messagesV1().serialize(
          new EISP1CommandLogin("someone", "12345678x"))
      );

    assertEquals(401, response.statusCode());

    final var error =
      this.parse(response, EISP1ResponseError.class);

    assertEquals("authentication", error.errorCode());
    assertEquals(0, this.cookies().getCookieStore().getCookies().size());
  }

  /**
   * Sending a nonsense message instead of a login fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginNotCommand()
    throws Exception
  {
    assertTrue(this.container().isRunning());
    this.server().start();

    this.createUserSomeone();

    final var response =
      this.postPublicBytes(
        "/public/1/0/login",
        this.messagesV1().serialize(
          new EISP1ResponseError(randomUUID(), "x", "e")
        )
      );

    assertEquals(400, response.statusCode());

    final var error =
      this.parse(response, EISP1ResponseError.class);

    assertEquals("protocol", error.errorCode());
    assertEquals(0, this.cookies().getCookieStore().getCookies().size());
  }

  /**
   * Sending a huge message fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginOversized()
    throws Exception
  {
    assertTrue(this.container().isRunning());
    this.server().start();

    final var response =
      this.postPublicBytes(
        "/public/1/0/login",
        this.messagesV1().serialize(
          new EISP1CommandLogin(
            new String(new char[1024]),
            new String(new char[1024])
          )
        )
      );

    assertEquals(413, response.statusCode());

    final var error =
      this.parse(response, EISP1ResponseError.class);

    assertEquals("limit", error.errorCode());
    assertEquals(0, this.cookies().getCookieStore().getCookies().size());
  }

  /**
   * POST is required to log in.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginGET()
    throws Exception
  {
    assertTrue(this.container().isRunning());
    this.server().start();

    this.createUserSomeone();

    final var response =
      this.getPublic("/public/1/0/login");

    assertEquals(405, response.statusCode());

    final var error =
      this.parse(response, EISP1ResponseError.class);

    assertEquals("http", error.errorCode());
    assertEquals(0, this.cookies().getCookieStore().getCookies().size());
  }
}
