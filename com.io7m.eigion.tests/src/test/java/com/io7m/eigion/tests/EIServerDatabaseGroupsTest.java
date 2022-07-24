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
import com.io7m.eigion.server.database.api.EIServerDatabaseGroupsQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseUsersQueriesType;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;

import static com.io7m.eigion.model.EIGroupRole.FOUNDER;
import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.EIGION;
import static java.time.OffsetDateTime.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class EIServerDatabaseGroupsTest extends EIWithDatabaseContract
{
  /**
   * Creating a user, a group, and adding the user to the group, works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUser()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);
    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();
    final var password =
      databaseGenerateBadPassword();

    final var user =
      users.userCreate(
        reqId,
        "someone",
        "someone@example.com",
        now,
        password
      );

    final var groupName =
      new EIGroupName("com.io7m.eigion.test");

    groups.groupCreate(groupName, user.id());
    groups.groupMembershipSet(groupName, user.id(), Set.of(FOUNDER));
    groups.groupMembershipSet(groupName, user.id(), Set.of());
    groups.groupMembershipRemove(groupName, user.id());

    checkAuditLog(
      transaction,
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
      new ExpectedEvent("USER_CREATED", reqId.toString()),
      new ExpectedEvent("GROUP_CREATED", groupName.value()),
      new ExpectedEvent(
        "GROUP_USERS_ADDED",
        "%s|%s".formatted(groupName.value(), user.id())),
      new ExpectedEvent(
        "GROUP_USERS_ROLES_CHANGED",
        "%s|%s|FOUNDER".formatted(groupName.value(), user.id())),
      new ExpectedEvent(
        "GROUP_USERS_ROLES_CHANGED",
        "%s|%s|".formatted(groupName.value(), user.id())),
      new ExpectedEvent(
        "GROUP_USERS_REMOVED",
        "%s|%s".formatted(groupName.value(), user.id()))
    );
  }
}
