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

import com.io7m.eigion.protocol.admin_api.v1.EISA1AuditEvent;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAuditGet;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandLogin;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandServicesList;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserCreate;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserGet;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserGetByEmail;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserGetByName;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserSearch;
import com.io7m.eigion.protocol.admin_api.v1.EISA1MessageType;
import com.io7m.eigion.protocol.admin_api.v1.EISA1Messages;
import com.io7m.eigion.protocol.admin_api.v1.EISA1Password;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseAuditGet;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseError;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseLogin;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseServiceList;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseUserCreate;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseUserGet;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseUserList;
import com.io7m.eigion.protocol.admin_api.v1.EISA1Service;
import com.io7m.eigion.protocol.admin_api.v1.EISA1SubsetMatch;
import com.io7m.eigion.protocol.admin_api.v1.EISA1Transaction;
import com.io7m.eigion.protocol.admin_api.v1.EISA1TransactionResponse;
import com.io7m.eigion.protocol.admin_api.v1.EISA1User;
import com.io7m.eigion.protocol.admin_api.v1.EISA1UserSummary;
import com.io7m.eigion.protocol.api.EIProtocolException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.io7m.eigion.protocol.admin_api.v1.EISA1GroupRole.FOUNDER;
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
        empty(),
        Map.ofEntries(
          Map.entry("com.example.group", Set.of(FOUNDER))
        ));

    final var services =
      List.of(
        new EISA1Service("s0", "sd0"),
        new EISA1Service("s1", "sd1"),
        new EISA1Service("s2", "sd2")
      );

    final var userSummary =
      new EISA1UserSummary(randomUUID(), "name", "email");

    return Stream.<EISA1MessageType>of(
      commandAuditGet(),
      commandLogin(),
      commandServicesList(),
      commandUserCreate(pass),
      commandUserGet(),
      commandUserGetByEmail(),
      commandUserGetByName(),
      commandUserSearch(),
      responseAuditGet(),
      responseError(),
      responseLogin(),
      responseServiceList(services),
      responseUserCreate(user),
      responseUserCreate(user),
      responseUserGet(user),
      responseUserList(userSummary),
      transaction(),
      transactionResponse(user)
    ).map(this::dynamicTestOfRoundTrip);
  }

  private static EISA1TransactionResponse transactionResponse(
    final EISA1User user)
  {
    return new EISA1TransactionResponse(
      randomUUID(),
      List.of(responseUserCreate(user)));
  }

  private static EISA1Transaction transaction()
  {
    return new EISA1Transaction(List.of(commandServicesList()));
  }

  private static EISA1ResponseUserList responseUserList(
    final EISA1UserSummary userSummary)
  {
    return new EISA1ResponseUserList(
      randomUUID(),
      List.of(userSummary, userSummary));
  }

  private static EISA1ResponseUserGet responseUserGet(
    final EISA1User user)
  {
    return new EISA1ResponseUserGet(randomUUID(), user);
  }

  private static EISA1ResponseServiceList responseServiceList(
    final List<EISA1Service> services)
  {
    return new EISA1ResponseServiceList(randomUUID(), services);
  }

  private static EISA1ResponseLogin responseLogin()
  {
    return new EISA1ResponseLogin(randomUUID());
  }

  private static EISA1ResponseError responseError()
  {
    return new EISA1ResponseError(randomUUID(), "e", "m");
  }

  private static EISA1ResponseAuditGet responseAuditGet()
  {
    return new EISA1ResponseAuditGet(
      randomUUID(),
      List.of(new EISA1AuditEvent(23,
                                  randomUUID(),
                                  now(),
                                  "type",
                                  "message")));
  }

  private static EISA1ResponseUserCreate responseUserCreate(
    final EISA1User user)
  {
    return new EISA1ResponseUserCreate(randomUUID(), user);
  }

  private static EISA1CommandUserSearch commandUserSearch()
  {
    return new EISA1CommandUserSearch("search");
  }

  private static EISA1CommandUserGetByEmail commandUserGetByEmail()
  {
    return new EISA1CommandUserGetByEmail("email");
  }

  private static EISA1CommandUserGetByName commandUserGetByName()
  {
    return new EISA1CommandUserGetByName("name");
  }

  private static EISA1CommandUserGet commandUserGet()
  {
    return new EISA1CommandUserGet(randomUUID());
  }

  private static EISA1CommandUserCreate commandUserCreate(
    final EISA1Password pass)
  {
    return new EISA1CommandUserCreate("x", "e", pass);
  }

  private static EISA1CommandServicesList commandServicesList()
  {
    return new EISA1CommandServicesList();
  }

  private static EISA1CommandLogin commandLogin()
  {
    return new EISA1CommandLogin("someone", "12345678");
  }

  private static EISA1CommandAuditGet commandAuditGet()
  {
    return new EISA1CommandAuditGet(
      now(),
      now(),
      new EISA1SubsetMatch<>("a", "b"),
      new EISA1SubsetMatch<>("c", "d"),
      new EISA1SubsetMatch<>("e", "f")
    );
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
