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

import com.io7m.eigion.model.EIPermissionSet;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.server.database.api.EISDatabaseUsersQueriesType;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
public final class EISDatabaseUsersTest
{
  @Container
  private final PostgreSQLContainer<?> container =
    new PostgreSQLContainer<>("postgres")
      .withDatabaseName("eigion")
      .withUsername("postgres")
      .withPassword("12345678");

  private EIFakeClock clock;
  private EITestDatabase database;

  @BeforeEach
  public void setup()
    throws Exception
  {
    this.clock = new EIFakeClock();
    this.database = EITestDatabase.create(this.container, this.clock);
  }

  @AfterEach
  public void tearDown()
    throws Exception
  {
    this.database.close();
  }

  /**
   * Getting/setting users works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserPutGet()
    throws Exception
  {
    this.database.withTransaction(t -> {
      final var q =
        t.queries(EISDatabaseUsersQueriesType.class);

      final var user0 =
        new EIUser(
          UUID.randomUUID(),
          EIPermissionSet.empty(),
          Map.of()
        );

      q.userPut(user0);

      final var user1 = q.userGetRequire(user0.id());
      assertEquals(user0, user1);
      return null;
    });
  }
}
