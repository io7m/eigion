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
import com.io7m.eigion.server.protocol.public_api.v1.EISP1MessageType;
import com.io7m.eigion.server.protocol.public_api.v1.EISP1Messages;
import com.io7m.eigion.server.protocol.public_api.v1.EISP1ResponseError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class EISP1MessagesTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EISP1MessagesTest.class);

  private EISP1Messages messages;

  @BeforeEach
  public void setup()
  {
    this.messages = new EISP1Messages();
  }

  /**
   * Messages are correctly serialized and parsed.
   *
   * @throws Exception On errors
   */

  @TestFactory
  public Stream<DynamicTest> testRoundTrip()
  {
    return Stream.of(
      new EISP1CommandLogin("user", "pass"),
      new EISP1ResponseError(randomUUID(),"errorCode", "message")
    ).map(this::dynamicTestOfRoundTrip);
  }

  private DynamicTest dynamicTestOfRoundTrip(
    final EISP1MessageType o)
  {
    return DynamicTest.dynamicTest(
      "testRoundTrip_" + o.getClass(),
      () -> {
        final var b = this.messages.serialize(o);
        LOG.debug("{}", new String(b, UTF_8));
        assertEquals(o, this.messages.parse(b));
      }
    );
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
