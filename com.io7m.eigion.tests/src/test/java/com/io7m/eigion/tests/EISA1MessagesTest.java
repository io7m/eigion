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

import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandServicesList;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserCreate;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserGet;
import com.io7m.eigion.protocol.admin_api.v1.EISA1MessageType;
import com.io7m.eigion.protocol.admin_api.v1.EISA1Messages;
import com.io7m.eigion.protocol.admin_api.v1.EISA1Password;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseError;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseServiceList;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseUserCreate;
import com.io7m.eigion.protocol.admin_api.v1.EISA1Service;
import com.io7m.eigion.protocol.admin_api.v1.EISA1Transaction;
import com.io7m.eigion.protocol.admin_api.v1.EISA1TransactionResponse;
import com.io7m.eigion.protocol.admin_api.v1.EISA1User;
import com.io7m.eigion.protocol.api.EIProtocolException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.OffsetDateTime.now;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class EISA1MessagesTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EISA1MessagesTest.class);

  private EISA1Messages messages;

  @BeforeEach
  public void setup()
  {
    this.messages = new EISA1Messages();
  }

  /**
   * Messages are correctly serialized and parsed.
   */

  @TestFactory
  public Stream<DynamicTest> testRoundTrip()
  {
    final var pass =
      new EISA1Password("x", "A", "0");

    final var user =
      new EISA1User(
        randomUUID(),
        "x",
        "e",
        now(),
        now(),
        pass,
        empty());

    final var services =
      List.of(
        new EISA1Service("s0", "sd0"),
        new EISA1Service("s1", "sd1"),
        new EISA1Service("s2", "sd2")
      );

    final var ruc =
      new EISA1ResponseUserCreate(randomUUID(), user);

    return Stream.<EISA1MessageType>of(
      new EISA1CommandServicesList(),
      new EISA1CommandUserCreate("x", "e", pass),
      new EISA1CommandUserGet(randomUUID()),
      ruc,
      new EISA1ResponseServiceList(randomUUID(), services),
      new EISA1ResponseError(randomUUID(), "e", "m"),
      new EISA1Transaction(List.of(new EISA1CommandServicesList())),
      new EISA1TransactionResponse(randomUUID(), List.of(ruc))
    ).map(this::dynamicTestOfRoundTrip);
  }

  private DynamicTest dynamicTestOfRoundTrip(
    final EISA1MessageType o)
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
    assertThrows(EIProtocolException.class, () -> {
      this.messages.parse("{}".getBytes(UTF_8));
    });
  }
}
