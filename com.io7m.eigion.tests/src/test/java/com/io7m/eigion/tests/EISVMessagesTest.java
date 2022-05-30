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
import com.io7m.eigion.server.protocol.versions.EISVMessages;
import com.io7m.eigion.server.protocol.versions.EISVProtocolSupported;
import com.io7m.eigion.server.protocol.versions.EISVProtocols;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.TWO;
import static java.math.BigInteger.ZERO;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class EISVMessagesTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EISVMessagesTest.class);

  private EISVMessages messages;

  @BeforeEach
  public void setup()
  {
    this.messages = new EISVMessages();
  }

  /**
   * Messages are correctly serialized and parsed.
   *
   * @throws Exception On errors
   */

  @Test
  public void testSupported()
    throws Exception
  {
    final var v =
      new EISVProtocols(
        List.of(
          new EISVProtocolSupported(
            "com.io7m.example", ONE, ZERO, "/ex/1/0"
          ),
          new EISVProtocolSupported(
            "com.io7m.example", TWO, ZERO, "/ex/2/0"
          ),
          new EISVProtocolSupported(
            "com.io7m.other", TEN, ONE, "/ex/10/1"
          )
        )
      );

    final var b = this.messages.serialize(v);
    LOG.debug("{}", new String(b, UTF_8));
    assertEquals(v, this.messages.parse(b));
  }

  /**
   * Invalid messages aren't parsed.
   *
   * @throws Exception On errors
   */

  @Test
  public void testInvalid()
    throws Exception
  {
    assertThrows(EIServerProtocolException.class, () -> {
      this.messages.parse("{}".getBytes(UTF_8));
    });
  }
}
