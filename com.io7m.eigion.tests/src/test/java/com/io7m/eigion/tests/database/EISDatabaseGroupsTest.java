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


package com.io7m.eigion.tests.database;

import com.io7m.eigion.model.EIGroupCreationRequest;
import com.io7m.eigion.model.EIGroupCreationRequestStatusType;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIGroupPrefix;
import com.io7m.eigion.model.EIGroupSearchByNameParameters;
import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.server.database.api.EISDatabaseException;
import com.io7m.eigion.server.database.api.EISDatabaseGroupsQueriesType;
import com.io7m.eigion.server.database.api.EISDatabaseTransactionType;
import com.io7m.eigion.server.database.api.EISDatabaseUsersQueriesType;
import com.io7m.eigion.tests.extensions.EIDatabaseExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.GROUP_DUPLICATE;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.GROUP_NONEXISTENT;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.GROUP_REQUEST_DUPLICATE;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.USER_NONEXISTENT;
import static com.io7m.eigion.model.EIGroupRole.FOUNDER;
import static com.io7m.eigion.model.EIGroupRole.USER_DISMISS;
import static com.io7m.eigion.model.EIGroupRole.USER_INVITE;
import static com.io7m.eigion.model.EIPermissionSet.empty;
import static java.time.OffsetDateTime.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@ExtendWith({EIDatabaseExtension.class})
public final class EISDatabaseGroupsTest
{
  /**
   * Duplicate groups are signalled.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupDuplicate(
    final EISDatabaseTransactionType t)
    throws Exception
  {
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
  }

  /**
   * Getting/setting users works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupPutGet(
    final EISDatabaseTransactionType t)
    throws Exception
  {
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
  }

  /**
   * Updating nonexistent groups fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupNonexistentGroup(
    final EISDatabaseTransactionType t)
    throws Exception
  {
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
  }

  /**
   * Updating groups with nonexistent users fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupNonexistentUser(
    final EISDatabaseTransactionType t)
    throws Exception
  {
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
  }

  /**
   * Creating a personal group succeeds.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreatePersonal(
    final EISDatabaseTransactionType t)
    throws Exception
  {
    final var users =
      t.queries(EISDatabaseUsersQueriesType.class);
    final var groups =
      t.queries(EISDatabaseGroupsQueriesType.class);

    final var u0 = new EIUser(UUID.randomUUID(), empty());
    users.userPut(u0);

    final var name =
      groups.groupCreatePersonal(u0.id(), new EIGroupPrefix("com.io7m."));

    assertEquals("com.io7m.u1", name.value());
  }

  /**
   * Nonexistent users cannot create group requests.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreationRequestNonexistentUser(
    final EISDatabaseTransactionType t)
    throws Exception
  {
    final var groups =
      t.queries(EISDatabaseGroupsQueriesType.class);

    final var ex =
      assertThrows(EISDatabaseException.class, () -> {
        groups.groupCreationRequestStart(new EIGroupCreationRequest(
          new EIGroupName("com.io7m.ex"),
          UUID.randomUUID(),
          EIToken.generate(),
          new EIGroupCreationRequestStatusType.InProgress(now())
        ));
      });

    assertEquals(USER_NONEXISTENT, ex.errorCode());
  }

  /**
   * Duplicate group requests (same token) cannot be created.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreationRequestDuplicate(
    final EISDatabaseTransactionType t)
    throws Exception
  {
    final var users =
      t.queries(EISDatabaseUsersQueriesType.class);
    final var groups =
      t.queries(EISDatabaseGroupsQueriesType.class);

    final var u0 = new EIUser(UUID.randomUUID(), empty());
    users.userPut(u0);

    final var token = EIToken.generate();

    groups.groupCreationRequestStart(new EIGroupCreationRequest(
      new EIGroupName("com.io7m.ex1"),
      u0.id(),
      token,
      new EIGroupCreationRequestStatusType.InProgress(now())
    ));

    final var ex =
      assertThrows(EISDatabaseException.class, () -> {
        groups.groupCreationRequestStart(new EIGroupCreationRequest(
          new EIGroupName("com.io7m.ex2"),
          u0.id(),
          token,
          new EIGroupCreationRequestStatusType.InProgress(now())
        ));
      });

    assertEquals(GROUP_REQUEST_DUPLICATE, ex.errorCode());
  }

  /**
   * Group requests can be created.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreationRequestList(
    final EISDatabaseTransactionType t)
    throws Exception
  {
    final var users =
      t.queries(EISDatabaseUsersQueriesType.class);
    final var groups =
      t.queries(EISDatabaseGroupsQueriesType.class);

    final var u0 = new EIUser(UUID.randomUUID(), empty());
    users.userPut(u0);

    groups.groupCreationRequestStart(new EIGroupCreationRequest(
      new EIGroupName("com.io7m.ex0"),
      u0.id(),
      EIToken.generate(),
      new EIGroupCreationRequestStatusType.InProgress(now())
    ));

    groups.groupCreationRequestStart(new EIGroupCreationRequest(
      new EIGroupName("com.io7m.ex1"),
      u0.id(),
      EIToken.generate(),
      new EIGroupCreationRequestStatusType.InProgress(now())
    ));

    groups.groupCreationRequestStart(new EIGroupCreationRequest(
      new EIGroupName("com.io7m.ex2"),
      u0.id(),
      EIToken.generate(),
      new EIGroupCreationRequestStatusType.InProgress(now())
    ));

    final var rs =
      groups.groupCreationRequestsForUser(u0.id());

    assertEquals(3, rs.size());
    assertEquals("com.io7m.ex0", rs.get(0).groupName().value());
    assertEquals("com.io7m.ex1", rs.get(1).groupName().value());
    assertEquals("com.io7m.ex2", rs.get(2).groupName().value());
  }

  /**
   * Nonexistent users cannot have group requests.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreationRequestListNonexistentUser(
    final EISDatabaseTransactionType t)
    throws Exception
  {
    final var groups =
      t.queries(EISDatabaseGroupsQueriesType.class);

    final var rs =
      groups.groupCreationRequestsForUser(UUID.randomUUID());

    assertEquals(0, rs.size());
  }

  /**
   * Group requests can be completed.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreationCompleteSuccess(
    final EISDatabaseTransactionType t)
    throws Exception
  {
    final var users =
      t.queries(EISDatabaseUsersQueriesType.class);
    final var groups =
      t.queries(EISDatabaseGroupsQueriesType.class);

    final var u0 = new EIUser(UUID.randomUUID(), empty());
    users.userPut(u0);

    final var token = EIToken.generate();
    final var request0 =
      new EIGroupCreationRequest(
        new EIGroupName("com.io7m.ex0"),
        u0.id(),
        token,
        new EIGroupCreationRequestStatusType.InProgress(now())
      );

    groups.groupCreationRequestStart(request0);

    final var request1 =
      new EIGroupCreationRequest(
        new EIGroupName("com.io7m.ex0"),
        u0.id(),
        token,
        new EIGroupCreationRequestStatusType.Succeeded(
          request0.status().timeStarted(), now())
      );

    groups.groupCreationRequestComplete(request1);

    final var membership =
      groups.groupRoles(new EIGroupName("com.io7m.ex0"), 1L)
        .pageCurrent(groups)
        .items();

    assertEquals(1, membership.size());
    assertEquals(u0.id(), membership.get(0).userId());
    assertTrue(membership.get(0).roles().implies(FOUNDER));
  }

  /**
   * Group requests can be failed.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreationCompleteFailed(
    final EISDatabaseTransactionType t)
    throws Exception
  {
    final var users =
      t.queries(EISDatabaseUsersQueriesType.class);
    final var groups =
      t.queries(EISDatabaseGroupsQueriesType.class);

    final var u0 = new EIUser(UUID.randomUUID(), empty());
    users.userPut(u0);

    final var token = EIToken.generate();
    final var request0 =
      new EIGroupCreationRequest(
        new EIGroupName("com.io7m.ex0"),
        u0.id(),
        token,
        new EIGroupCreationRequestStatusType.InProgress(now())
      );

    groups.groupCreationRequestStart(request0);

    final var request1 =
      new EIGroupCreationRequest(
        new EIGroupName("com.io7m.ex0"),
        u0.id(),
        token,
        new EIGroupCreationRequestStatusType.Failed(
          request0.status().timeStarted(),
          now(),
          "FAILED!"
        )
      );

    groups.groupCreationRequestComplete(request1);

    final var page =
      groups.groupSearchByName(new EIGroupSearchByNameParameters(
        Optional.of("com.io7m.ex0"),
        1L
      )).pageCurrent(groups);

    assertEquals(0, page.items().size());
  }

  /**
   * Group requests can be cancelled.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreationCompleteCancelled(
    final EISDatabaseTransactionType t)
    throws Exception
  {
    final var users =
      t.queries(EISDatabaseUsersQueriesType.class);
    final var groups =
      t.queries(EISDatabaseGroupsQueriesType.class);

    final var u0 = new EIUser(UUID.randomUUID(), empty());
    users.userPut(u0);

    final var token = EIToken.generate();
    final var request0 =
      new EIGroupCreationRequest(
        new EIGroupName("com.io7m.ex0"),
        u0.id(),
        token,
        new EIGroupCreationRequestStatusType.InProgress(now())
      );

    groups.groupCreationRequestStart(request0);

    final var request1 =
      new EIGroupCreationRequest(
        new EIGroupName("com.io7m.ex0"),
        u0.id(),
        token,
        new EIGroupCreationRequestStatusType.Cancelled(
          request0.status().timeStarted(),
          now()
        )
      );

    groups.groupCreationRequestComplete(request1);

    final var page =
      groups.groupSearchByName(new EIGroupSearchByNameParameters(
        Optional.of("com.io7m.ex0"),
        1L
      )).pageCurrent(groups);

    assertEquals(0, page.items().size());
  }
}
