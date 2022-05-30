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
import com.io7m.eigion.server.protocol.versions.EISVProtocols;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.http.HttpResponse;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class EIServerAdminAPIVersionsTest extends EIServerContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIServerAdminAPIVersionsTest.class);

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
      this.parse(response, EISVProtocols.class);
  }

  private <T> T parse(
    final HttpResponse<byte[]> response,
    final Class<T> clazz)
    throws EIServerProtocolException
  {
    final var bodyText = response.body();
    LOG.debug("received: {}", new String(bodyText, UTF_8));
    return clazz.cast(this.messagesV().parse(bodyText));
  }
}
