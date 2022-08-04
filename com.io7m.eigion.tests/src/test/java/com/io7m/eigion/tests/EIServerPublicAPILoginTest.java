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

import com.io7m.eigion.protocol.public_api.v1.EISP1CommandLogin;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseError;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.HTTP_METHOD_ERROR;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.HTTP_SIZE_LIMIT;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.PROTOCOL_ERROR;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
public final class EIServerPublicAPILoginTest extends EIServerContract
{
  /**
   * Sending garbage fails a login.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginNonsense()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var response =
      this.msgSendPublicText("/public/1/0/login", "{}");

    assertEquals(400, response.statusCode());

    final var error =
      this.msgParsePublic(response, EISP1ResponseError.class);

    assertEquals(PROTOCOL_ERROR.id(), error.errorCode());
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
    this.serverStartIfNecessary();

    final var adminId =
      this.createAdminInitial("someone", "12345678");

    this.createUserSomeone(adminId);

    final var response =
      this.msgSendPublicBytes(
        "/public/1/0/login",
        this.messagesPublicV1().serialize(
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
    this.serverStartIfNecessary();

    final var adminId =
      this.createAdminInitial("someone", "12345678");

    this.createUserSomeone(adminId);

    final var response =
      this.msgSendPublicBytes(
        "/public/1/0/login",
        this.messagesPublicV1().serialize(
          new EISP1CommandLogin("someonex", "12345678"))
      );

    assertEquals(401, response.statusCode());

    final var error =
      this.msgParsePublic(response, EISP1ResponseError.class);

    assertEquals(AUTHENTICATION_ERROR.id(), error.errorCode());
    assertEquals(0, this.cookies().getCookieStore().getCookies().size());
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
    this.serverStartIfNecessary();

    final var adminId =
      this.createAdminInitial("someone", "12345678");

    this.createUserSomeone(adminId);

    final var response =
      this.msgSendPublicBytes(
        "/public/1/0/login",
        this.messagesPublicV1().serialize(
          new EISP1CommandLogin("someone", "12345678x"))
      );

    assertEquals(401, response.statusCode());

    final var error =
      this.msgParsePublic(response, EISP1ResponseError.class);

    assertEquals(AUTHENTICATION_ERROR.id(), error.errorCode());
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
    this.serverStartIfNecessary();

    final var adminId =
      this.createAdminInitial("someone", "12345678");

    this.createUserSomeone(adminId);

    final var response =
      this.msgSendPublicBytes(
        "/public/1/0/login",
        this.messagesPublicV1().serialize(
          new EISP1ResponseError(randomUUID(), "x", "e")
        )
      );

    assertEquals(400, response.statusCode());

    final var error =
      this.msgParsePublic(response, EISP1ResponseError.class);

    assertEquals(PROTOCOL_ERROR.id(), error.errorCode());
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
    this.serverStartIfNecessary();

    final var response =
      this.msgSendPublicBytes(
        "/public/1/0/login",
        this.messagesPublicV1().serialize(
          new EISP1CommandLogin(
            new String(new char[1024]),
            new String(new char[1024])
          )
        )
      );

    assertEquals(413, response.statusCode());

    final var error =
      this.msgParsePublic(response, EISP1ResponseError.class);

    assertEquals(HTTP_SIZE_LIMIT.id(), error.errorCode());
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
    this.serverStartIfNecessary();

    final var adminId =
      this.createAdminInitial("someone", "12345678");

    this.createUserSomeone(adminId);

    final var response =
      this.msgGetPublic("/public/1/0/login");

    assertEquals(405, response.statusCode());

    final var error =
      this.msgParsePublic(response, EISP1ResponseError.class);

    assertEquals(HTTP_METHOD_ERROR.id(), error.errorCode());
    assertEquals(0, this.cookies().getCookieStore().getCookies().size());
  }
}
