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

import com.io7m.eigion.model.EIGroupCreationRequestStatusType;
import com.io7m.eigion.protocol.api.EIProtocolException;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupCreateBegin;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupCreateCancel;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupCreateRequests;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandLogin;
import com.io7m.eigion.protocol.public_api.v1.EISP1GroupCreationRequest;
import com.io7m.eigion.protocol.public_api.v1.EISP1MessageType;
import com.io7m.eigion.protocol.public_api.v1.EISP1Messages;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseError;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroupCreateBegin;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroupCreateCancel;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroupCreateRequests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
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
      commandGroupCreateBegin(),
      commandGroupCreateRequests(),
      commandLogin(),
      responseError(),
      responseGroupCreateBegin(),
      responseGroupCreateCancel(),
      responseGroupCreateRequests()
    ).map(this::dynamicTestOfRoundTrip);
  }

  private static EISP1ResponseGroupCreateBegin responseGroupCreateBegin()
  {
    return new EISP1ResponseGroupCreateBegin(
      randomUUID(),
      "com.io7m.ex",
      "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855",
      URI.create(
        "https://ex.io7m.com/.well-known/eigion-group-challenge/E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855.txt")
    );
  }

  private static EISP1ResponseError responseError()
  {
    return new EISP1ResponseError(randomUUID(), "errorCode", "message");
  }

  private static EISP1ResponseGroupCreateCancel responseGroupCreateCancel()
  {
    return new EISP1ResponseGroupCreateCancel(
      randomUUID()
    );
  }

  private static EISP1ResponseGroupCreateRequests responseGroupCreateRequests()
  {
    return new EISP1ResponseGroupCreateRequests(
      randomUUID(),
      List.of(
        new EISP1GroupCreationRequest(
          "com.io7m.ex",
          randomUUID(),
          "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855",
          EIGroupCreationRequestStatusType.NAME_SUCCEEDED,
          OffsetDateTime.now(),
          Optional.of(OffsetDateTime.now()),
          "Message"
        ),
        new EISP1GroupCreationRequest(
          "com.io7m.ex",
          randomUUID(),
          "EFEDCB0AB0F2FC29DB0D41FF1F29534D",
          EIGroupCreationRequestStatusType.NAME_FAILED,
          OffsetDateTime.now(),
          Optional.of(OffsetDateTime.now()),
          "Message"
        ),
        new EISP1GroupCreationRequest(
          "com.io7m.ex",
          randomUUID(),
          "E8DCD590D2B6A5E3B14D0DE4AD2A0CBA",
          EIGroupCreationRequestStatusType.NAME_IN_PROGRESS,
          OffsetDateTime.now(),
          Optional.of(OffsetDateTime.now()),
          "Message"
        )
      )
    );
  }

  private static EISP1CommandLogin commandLogin()
  {
    return new EISP1CommandLogin("user", "pass");
  }

  private static EISP1CommandGroupCreateBegin commandGroupCreateBegin()
  {
    return new EISP1CommandGroupCreateBegin("com.io7m.ex");
  }

  private static EISP1CommandGroupCreateCancel commandGroupCreateCancel()
  {
    return new EISP1CommandGroupCreateCancel("7B8851BBBB70805081396C74ED005B10");
  }

  private static EISP1CommandGroupCreateRequests commandGroupCreateRequests()
  {
    return new EISP1CommandGroupCreateRequests();
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
    assertThrows(EIProtocolException.class, () -> {
      this.messages.parse("{}".getBytes(UTF_8));
    });
  }
}
