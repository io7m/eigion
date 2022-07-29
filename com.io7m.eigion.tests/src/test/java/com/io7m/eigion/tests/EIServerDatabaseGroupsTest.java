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

import com.io7m.eigion.model.EIGroupCreationRequest;
import com.io7m.eigion.model.EIGroupCreationRequestStatusType;
import com.io7m.eigion.model.EIGroupCreationRequestStatusType.Failed;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIPasswordException;
import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseGroupsQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseTransactionType;
import com.io7m.eigion.server.database.api.EIServerDatabaseUsersQueriesType;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.io7m.eigion.model.EIGroupRole.FOUNDER;
import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.EIGION;
import static java.time.OffsetDateTime.now;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class EIServerDatabaseGroupsTest extends EIWithDatabaseContract
{
  private static EIUser createUser(
    final EIServerDatabaseTransactionType transaction)
    throws EIServerDatabaseException, EIPasswordException
  {
    final var users =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();
    final var password =
      databaseGenerateBadPassword();

    return users.userCreate(
      reqId,
      "someone",
      "someone@example.com",
      now,
      password
    );
  }

  /**
   * Creating a user, a group, and adding the user to the group, works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreate()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var user =
      createUser(transaction);

    final var groupName =
      new EIGroupName("com.io7m.eigion.test");

    groups.groupCreate(groupName, user.id());
    groups.groupMembershipSet(groupName, user.id(), Set.of(FOUNDER));

    {
      final var m =
        groups.groupMembershipGet(groupName, user.id()).orElseThrow();
      assertEquals(Set.of(FOUNDER), m);
    }

    groups.groupMembershipSet(groupName, user.id(), Set.of());

    {
      final var m =
        groups.groupMembershipGet(groupName, user.id()).orElseThrow();
      assertEquals(Set.of(), m);
    }

    groups.groupMembershipRemove(groupName, user.id());

    checkAuditLog(
      transaction,
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
      new ExpectedEvent("USER_CREATED", user.id().toString()),
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

  /**
   * Nonexistent groups cannot be checked for user membership.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupMembershipNonexistentGroup()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var user =
      createUser(transaction);

    final var groupName =
      new EIGroupName("com.io7m.eigion.test");

    {
      final var ex =
        assertThrows(EIServerDatabaseException.class, () -> {
          groups.groupMembershipSet(groupName, user.id(), Set.of(FOUNDER));
        });

      assertEquals("group-nonexistent", ex.errorCode());
    }

    {
      final var ex =
        assertThrows(EIServerDatabaseException.class, () -> {
          groups.groupMembershipGet(groupName, user.id());
        });

      assertEquals("group-nonexistent", ex.errorCode());
    }
  }

  /**
   * Nonexistent users cannot be added to groups.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupMembershipNonexistentUser()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var user =
      createUser(transaction);

    final var groupName =
      new EIGroupName("com.io7m.eigion.test");

    groups.groupCreate(groupName, user.id());

    {
      final var ex =
        assertThrows(EIServerDatabaseException.class, () -> {
          groups.groupMembershipSet(groupName, randomUUID(), Set.of(FOUNDER));
        });

      assertEquals("user-nonexistent", ex.errorCode());
    }

    {
      final var ex =
        assertThrows(EIServerDatabaseException.class, () -> {
          groups.groupMembershipGet(groupName, randomUUID());
        });

      assertEquals("user-nonexistent", ex.errorCode());
    }
  }

  /**
   * Creating a group that already exists, fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreateDuplicate()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var user =
      createUser(transaction);

    final var groupName =
      new EIGroupName("com.io7m.eigion.test");

    groups.groupCreate(groupName, user.id());

    final var ex =
      assertThrows(EIServerDatabaseException.class, () -> {
        groups.groupCreate(groupName, user.id());
      });

    assertEquals("group-duplicate", ex.errorCode());
  }

  /**
   * Trying to create a group with a nonexistent user, fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreateUserNonexistent()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var groupName =
      new EIGroupName("com.io7m.eigion.test");

    final var ex =
      assertThrows(EIServerDatabaseException.class, () -> {
        groups.groupCreate(groupName, UUID.randomUUID());
      });

    assertEquals("user-nonexistent", ex.errorCode());
  }

  /**
   * Group requests work.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreateRequestSuccessFinish()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var user = EIServerDatabaseGroupsTest.createUser(transaction);

    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var groupName =
      new EIGroupName("com.io7m.eigion.test");

    final var request =
      new EIGroupCreationRequest(
        groupName,
        user.id(),
        new EIToken(
          "5891B5B522D5DF086D0FF0B110FBD9D21BB4FC7163AF34D08286A2E846F6BE03"),
        empty()
      );

    groups.groupCreationRequestStart(request);

    assertEquals(
      List.of(request),
      groups.groupCreationRequestsForUser(user.id())
    );

    assertEquals(
      Optional.of(request),
      groups.groupCreationRequest(request.token())
    );

    groups.groupCreationRequestCompleteSuccessfully(request);

    {
      final var after0 =
        groups.groupCreationRequestsForUser(user.id()).get(0);
      final var after1 =
        groups.groupCreationRequest(request.token())
            .orElseThrow();

      assertEquals(
        EIGroupCreationRequestStatusType.Succeeded.class,
        after0.status().orElseThrow().getClass()
      );
      assertEquals(after0, after1);
    }

    assertTrue(groups.groupExists(groupName));

    checkAuditLog(
      transaction,
      new ExpectedEvent(
        "ADMIN_CREATED", adminId.toString()),
      new ExpectedEvent(
        "USER_CREATED", user.id().toString()),
      new ExpectedEvent(
        "GROUP_CREATION_REQUESTED",
        "%s|%s|%s".formatted(groupName, user.id(), request.token())),
      new ExpectedEvent(
        "GROUP_CREATION_REQUEST_SUCCEEDED",
        "%s|%s|%s".formatted(groupName, user.id(), request.token())),
      new ExpectedEvent(
        "GROUP_CREATED", groupName.value())
    );
  }

  /**
   * Group requests fail for broken tokens.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreateRequestSuccessFinishBadToken()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var user = EIServerDatabaseGroupsTest.createUser(transaction);

    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var groupName =
      new EIGroupName("com.io7m.eigion.test");

    final var request0 =
      new EIGroupCreationRequest(
        groupName,
        user.id(),
        new EIToken(
          "5891B5B522D5DF086D0FF0B110FBD9D21BB4FC7163AF34D08286A2E846F6BE03"),
        empty()
      );

    final var request1 =
      new EIGroupCreationRequest(
        groupName,
        user.id(),
        new EIToken(
          "71573B922A87ABC3FD1A957F2CFA09D9E16998567DD878A85E12166112751806"),
        empty()
      );

    groups.groupCreationRequestStart(request0);

    final var ex =
      assertThrows(EIServerDatabaseException.class, () -> {
        groups.groupCreationRequestCompleteSuccessfully(request1);
      });

    assertEquals("group-request-token", ex.errorCode());
  }

  /**
   * Group requests fail for nonexistent groups.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreateRequestSuccessFinishNonexistent()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var user = EIServerDatabaseGroupsTest.createUser(transaction);

    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var groupName =
      new EIGroupName("com.io7m.eigion.test");

    final var request0 =
      new EIGroupCreationRequest(
        groupName,
        user.id(),
        new EIToken(
          "5891B5B522D5DF086D0FF0B110FBD9D21BB4FC7163AF34D08286A2E846F6BE03"),
        empty()
      );

    final var ex =
      assertThrows(EIServerDatabaseException.class, () -> {
        groups.groupCreationRequestCompleteSuccessfully(request0);
      });

    assertEquals("group-request-nonexistent", ex.errorCode());
  }

  /**
   * Group requests fail for groups that already exist.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreateRequestFinishAlreadyExists()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var user = EIServerDatabaseGroupsTest.createUser(transaction);

    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var groupName =
      new EIGroupName("com.io7m.eigion.test");

    final var request0 =
      new EIGroupCreationRequest(
        groupName,
        user.id(),
        new EIToken(
          "5891B5B522D5DF086D0FF0B110FBD9D21BB4FC7163AF34D08286A2E846F6BE03"),
        empty()
      );

    groups.groupCreationRequestStart(request0);
    groups.groupCreate(groupName, user.id());

    {
      final var ex =
        assertThrows(EIServerDatabaseException.class, () -> {
          groups.groupCreationRequestCompleteSuccessfully(request0);
        });
      assertEquals("group-duplicate", ex.errorCode());
    }
  }

  /**
   * Group requests fail for groups that already exist.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreateRequestStartAlreadyExists()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var user = EIServerDatabaseGroupsTest.createUser(transaction);

    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var groupName =
      new EIGroupName("com.io7m.eigion.test");

    final var request0 =
      new EIGroupCreationRequest(
        groupName,
        user.id(),
        new EIToken(
          "5891B5B522D5DF086D0FF0B110FBD9D21BB4FC7163AF34D08286A2E846F6BE03"),
        empty()
      );

    groups.groupCreate(groupName, user.id());

    final var ex =
      assertThrows(EIServerDatabaseException.class, () -> {
        groups.groupCreationRequestStart(request0);
      });

    assertEquals("group-duplicate", ex.errorCode());
  }

  /**
   * Duplicate group requests fail.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreateRequestStartDuplicate()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var user = EIServerDatabaseGroupsTest.createUser(transaction);

    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var groupName =
      new EIGroupName("com.io7m.eigion.test");

    final var request0 =
      new EIGroupCreationRequest(
        groupName,
        user.id(),
        new EIToken(
          "5891B5B522D5DF086D0FF0B110FBD9D21BB4FC7163AF34D08286A2E846F6BE03"),
        empty()
      );

    groups.groupCreationRequestStart(request0);

    final var ex =
      assertThrows(EIServerDatabaseException.class, () -> {
        groups.groupCreationRequestStart(request0);
      });

    assertEquals("group-request-duplicate", ex.errorCode());
  }

  /**
   * Group request failures work.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreateRequestFailureFinish()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var user = EIServerDatabaseGroupsTest.createUser(transaction);

    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var groupName =
      new EIGroupName("com.io7m.eigion.test");

    final var request =
      new EIGroupCreationRequest(
        groupName,
        user.id(),
        new EIToken(
          "5891B5B522D5DF086D0FF0B110FBD9D21BB4FC7163AF34D08286A2E846F6BE03"),
        empty()
      );

    groups.groupCreationRequestStart(request);

    assertEquals(
      List.of(request),
      groups.groupCreationRequestsForUser(user.id())
    );

    groups.groupCreationRequestCompleteFailed(request, "This failed.");

    {
      final var after =
        groups.groupCreationRequestsForUser(user.id()).get(0);

      final var status = (Failed) after.status().orElseThrow();
      assertEquals("This failed.", status.message());
    }

    assertFalse(groups.groupExists(groupName));

    checkAuditLog(
      transaction,
      new ExpectedEvent(
        "ADMIN_CREATED", adminId.toString()),
      new ExpectedEvent(
        "USER_CREATED", user.id().toString()),
      new ExpectedEvent(
        "GROUP_CREATION_REQUESTED",
        "%s|%s|%s".formatted(groupName, user.id(), request.token())),
      new ExpectedEvent(
        "GROUP_CREATION_REQUEST_FAILED",
        "%s|%s|%s".formatted(groupName, user.id(), request.token()))
    );
  }

  /**
   * Group requests fail for broken tokens.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreateRequestFailureFinishBadToken()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var user = EIServerDatabaseGroupsTest.createUser(transaction);

    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var groupName =
      new EIGroupName("com.io7m.eigion.test");

    final var request0 =
      new EIGroupCreationRequest(
        groupName,
        user.id(),
        new EIToken(
          "5891B5B522D5DF086D0FF0B110FBD9D21BB4FC7163AF34D08286A2E846F6BE03"),
        empty()
      );

    final var request1 =
      new EIGroupCreationRequest(
        groupName,
        user.id(),
        new EIToken(
          "71573B922A87ABC3FD1A957F2CFA09D9E16998567DD878A85E12166112751806"),
        empty()
      );

    groups.groupCreationRequestStart(request0);

    final var ex =
      assertThrows(EIServerDatabaseException.class, () -> {
        groups.groupCreationRequestCompleteFailed(request1, "irrelevant");
      });

    assertEquals("group-request-token", ex.errorCode());
  }

  /**
   * Group requests fail for nonexistent groups.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreateRequestFailureFinishNonexistent()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var user = EIServerDatabaseGroupsTest.createUser(transaction);

    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var groupName =
      new EIGroupName("com.io7m.eigion.test");

    final var request0 =
      new EIGroupCreationRequest(
        groupName,
        user.id(),
        new EIToken(
          "5891B5B522D5DF086D0FF0B110FBD9D21BB4FC7163AF34D08286A2E846F6BE03"),
        empty()
      );

    final var ex =
      assertThrows(EIServerDatabaseException.class, () -> {
        groups.groupCreationRequestCompleteFailed(request0, "irrelevant");
      });

    assertEquals("group-request-nonexistent", ex.errorCode());
  }
}
