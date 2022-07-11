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

import com.io7m.eigion.server.protocol.public_api.v1.EISP1CommandLogin;
import com.io7m.eigion.server.protocol.public_api.v1.EISP1ResponseError;
import com.io7m.eigion.server.protocol.public_api.v1.EISP1ResponseImageCreated;
import com.io7m.eigion.server.protocol.public_api.v1.EISP1ResponseImageGet;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class EIServerPublicAPIImagesTest extends EIServerContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIServerPublicAPIImagesTest.class);

  /**
   * Creating images works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCreateImageOK()
    throws Exception
  {
    assertTrue(this.container().isRunning());
    this.server().start();

    this.createUserSomeone();

    final byte[] data =
      EITestDirectories.resourceBytesOf(
        EIServerPublicAPIImagesTest.class,
        this.directory(),
        "fruit-50.jpg"
      );

    {
      final var r =
        this.postPublicBytes(
          "/public/1/0/login",
          this.messagesV1().serialize(
            new EISP1CommandLogin("someone", "12345678"))
        );
      assertEquals(200, r.statusCode());
    }

    final var r =
      this.postPublicBytes("/public/1/0/image/create", data);
    assertEquals(200, r.statusCode());

    final var created =
      this.parsePublic(r, EISP1ResponseImageCreated.class);

    {
      final var rk =
        this.getPublic("/public/1/0/image/get?id=" + created.imageId());
      assertEquals(200, rk.statusCode());

      final var got =
        this.parsePublic(rk, EISP1ResponseImageGet.class);

      assertEquals(created.imageId(), got.imageId());
      assertEquals("SHA-256", got.hash().algorithm());
      assertEquals("5A4061BE191DA45F2A29A33C9609F88CC89FE84C13525D3A80904B0455323FFF", got.hash().hash());
    }

    {
      final var storage =
        this.storage().storages().peek();
      final var got =
        storage.get("/images/%s.jpg".formatted(created.imageId()))
          .orElseThrow();

      assertEquals("SHA-256", got.hash().algorithm());
      assertEquals("5A4061BE191DA45F2A29A33C9609F88CC89FE84C13525D3A80904B0455323FFF", got.hash().hash());
    }
  }

  /**
   * Obtaining nonexistent images fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testImageNonexistent()
    throws Exception
  {
    assertTrue(this.container().isRunning());
    this.server().start();

    this.createUserSomeone();

    {
      final var r =
        this.postPublicBytes(
          "/public/1/0/login",
          this.messagesV1().serialize(
            new EISP1CommandLogin("someone", "12345678"))
        );
      assertEquals(200, r.statusCode());
    }

    {
      final var rk =
        this.getPublic("/public/1/0/image/get?id=04f25ed5-2fb4-4d19-994c-5f623df18e8b");
      assertEquals(404, rk.statusCode());
    }
  }

  /**
   * Obtaining images without giving an ID, or an invalid ID, fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testImageNoID()
    throws Exception
  {
    assertTrue(this.container().isRunning());
    this.server().start();

    this.createUserSomeone();

    {
      final var r =
        this.postPublicBytes(
          "/public/1/0/login",
          this.messagesV1().serialize(
            new EISP1CommandLogin("someone", "12345678"))
        );
      assertEquals(200, r.statusCode());
    }

    {
      final var rk =
        this.getPublic("/public/1/0/image/get");
      assertEquals(400, rk.statusCode());
    }

    {
      final var rk =
        this.getPublic("/public/1/0/image/get?x=23");
      assertEquals(400, rk.statusCode());
    }

    {
      final var rk =
        this.getPublic("/public/1/0/image/get?id=23");
      assertEquals(400, rk.statusCode());
    }
  }

  /**
   * Only some HTTP methods are valid.
   *
   * @throws Exception On errors
   */

  @Test
  public void testImageBadMethod()
    throws Exception
  {
    assertTrue(this.container().isRunning());
    this.server().start();

    this.createUserSomeone();

    {
      final var request =
        HttpRequest.newBuilder()
          .DELETE()
          .uri(URI.create("http://localhost:40001/public/1/0/login"))
          .build();

      final var r =
        this.httpClient().send(
          request,
          HttpResponse.BodyHandlers.ofByteArray()
        );
      assertEquals(405, r.statusCode());
    }
  }

  /**
   * Creating images fails for non-JPEG data.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCreateImageNotJPEG()
    throws Exception
  {
    assertTrue(this.container().isRunning());
    this.server().start();

    this.createUserSomeone();

    final byte[] data =
      EITestDirectories.resourceBytesOf(
        EIServerPublicAPIImagesTest.class,
        this.directory(),
        "dracula.txt"
      );

    {
      final var r =
        this.postPublicBytes(
          "/public/1/0/login",
          this.messagesV1().serialize(
            new EISP1CommandLogin("someone", "12345678"))
        );
      assertEquals(200, r.statusCode());
    }

    final var r =
      this.postPublicBytes("/public/1/0/image/create", data);
    assertEquals(400, r.statusCode());

    final var error =
      this.parsePublic(r, EISP1ResponseError.class);

    assertEquals("image", error.errorCode());
  }
}
