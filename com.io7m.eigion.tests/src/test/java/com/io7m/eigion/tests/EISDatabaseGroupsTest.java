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

import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIGroupPrefix;
import com.io7m.eigion.model.EIGroupRole;
import com.io7m.eigion.model.EIPermissionSet;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.server.database.api.EISDatabaseException;
import com.io7m.eigion.server.database.api.EISDatabaseGroupsQueriesType;
import com.io7m.eigion.server.database.api.EISDatabaseUsersQueriesType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.GROUP_DUPLICATE;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.GROUP_NONEXISTENT;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.USER_NONEXISTENT;
import static com.io7m.eigion.model.EIGroupRole.FOUNDER;
import static com.io7m.eigion.model.EIGroupRole.USER_DISMISS;
import static com.io7m.eigion.model.EIGroupRole.USER_INVITE;
import static com.io7m.eigion.model.EIPermissionSet.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers(disabledWithoutDocker = true)
public final class EISDatabaseGroupsTest
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
   * Duplicate groups are signalled.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupDuplicate()
    throws Exception
  {
    this.database.withTransaction(t -> {
      final var users =
        t.queries(EISDatabaseUsersQueriesType.class);
      final var groups =
        t.queries(EISDatabaseGroupsQueriesType.class);

      final var u0 = new EIUser(UUID.randomUUID(), empty());
      users.userPut(u0);

      final var name = new EIGroupName("com.io7m.example");
      groups.groupCreate(u0.id(), name);

      final var ex =
        assertThrows(EISDatabaseException.class, () -> {
          groups.groupCreate(u0.id(), name);
        });

      assertEquals(GROUP_DUPLICATE, ex.errorCode());
      return null;
    });
  }

  /**
   * Getting/setting users works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupPutGet()
    throws Exception
  {
    this.database.withTransaction(t -> {
      final var users =
        t.queries(EISDatabaseUsersQueriesType.class);
      final var groups =
        t.queries(EISDatabaseGroupsQueriesType.class);

      final var u0 =
        new EIUser(
          UUID.fromString("3eeb69b1-bef4-4628-ac4f-17faa2eafdab"),
          empty()
        );

      final var u1 =
        new EIUser(
          UUID.fromString("83e89633-c783-4fc6-a59d-25ee25cd12d3"),
          empty()
        );

      final var u2 =
        new EIUser(
          UUID.fromString("ac3e5593-a8ef-4783-a9db-d0d851f715ca"),
          empty()
        );

      users.userPut(u0);
      users.userPut(u1);
      users.userPut(u2);

      final var name = new EIGroupName("com.io7m.example");
      groups.groupCreate(u0.id(), name);
      groups.groupUserUpdate(name, u0.id(), Set.of(FOUNDER));
      groups.groupUserUpdate(name, u1.id(), Set.of(USER_INVITE));
      groups.groupUserUpdate(name, u2.id(), Set.of(USER_INVITE, USER_DISMISS));

      final var s =
        groups.groupRoles(name, 100L);

      final var p = s.pageCurrent(groups);
      assertEquals(1, p.pageIndex());
      assertEquals(1, p.pageCount());

      {
        final var i = p.items().get(0);
        assertEquals(u0.id(), i.userId());
        assertEquals(name, i.group());
        assertEquals(Set.of(FOUNDER), i.roles().roles());
      }

      {
        final var i = p.items().get(1);
        assertEquals(u1.id(), i.userId());
        assertEquals(name, i.group());
        assertEquals(Set.of(USER_INVITE), i.roles().roles());
      }

      {
        final var i = p.items().get(2);
        assertEquals(u2.id(), i.userId());
        assertEquals(name, i.group());
        assertEquals(Set.of(USER_INVITE, USER_DISMISS), i.roles().roles());
      }

      return null;
    });
  }

  /**
   * Updating nonexistent groups fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupNonexistentGroup()
    throws Exception
  {
    this.database.withTransaction(t -> {
      final var users =
        t.queries(EISDatabaseUsersQueriesType.class);
      final var groups =
        t.queries(EISDatabaseGroupsQueriesType.class);

      final var u0 = new EIUser(UUID.randomUUID(), empty());
      users.userPut(u0);

      final var name = new EIGroupName("com.io7m.example");
      final var ex =
        assertThrows(EISDatabaseException.class, () -> {
          groups.groupUserUpdate(name, u0.id(), Set.of(FOUNDER));
        });

      assertEquals(GROUP_NONEXISTENT, ex.errorCode());
      return null;
    });
  }

  /**
   * Updating groups with nonexistent users fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupNonexistentUser()
    throws Exception
  {
    this.database.withTransaction(t -> {
      final var users =
        t.queries(EISDatabaseUsersQueriesType.class);
      final var groups =
        t.queries(EISDatabaseGroupsQueriesType.class);

      final var u0 = new EIUser(UUID.randomUUID(), empty());
      users.userPut(u0);

      final var name = new EIGroupName("com.io7m.example");
      groups.groupCreate(u0.id(), name);

      final var ex =
        assertThrows(EISDatabaseException.class, () -> {
          groups.groupUserUpdate(name, UUID.randomUUID(), Set.of(USER_INVITE));
        });

      assertEquals(USER_NONEXISTENT, ex.errorCode());
      return null;
    });
  }

  /**
   * Creating a personal group succeeds.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreatePersonal()
    throws Exception
  {
    this.database.withTransaction(t -> {
      final var users =
        t.queries(EISDatabaseUsersQueriesType.class);
      final var groups =
        t.queries(EISDatabaseGroupsQueriesType.class);

      final var u0 = new EIUser(UUID.randomUUID(), empty());
      users.userPut(u0);

      final var name =
        groups.groupCreatePersonal(u0.id(), new EIGroupPrefix("com.io7m."));

      assertEquals("com.io7m.u1", name.value());
      return null;
    });
  }
}
