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
import com.io7m.eigion.model.EIGroupCreationRequestStatusType.InProgress;
import com.io7m.eigion.model.EIGroupCreationRequestStatusType.Succeeded;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIGroupRole;
import com.io7m.eigion.model.EIGroupRoles;
import com.io7m.eigion.model.EIPasswordException;
import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.model.EIUserDisplayName;
import com.io7m.eigion.model.EIUserEmail;
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
import static java.lang.Thread.sleep;
import static java.time.OffsetDateTime.now;
import static java.util.EnumSet.allOf;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class EIServerDatabaseGroupsTest extends EIWithDatabaseContract
{
  private static EIUser createUserWithName(
    final EIServerDatabaseTransactionType transaction,
    final String name)
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
      new EIUserDisplayName(name),
      new EIUserEmail(name + "@example.com"),
      now,
      password
    );
  }

  private static EIUser createUser(
    final EIServerDatabaseTransactionType transaction)
    throws EIServerDatabaseException, EIPasswordException
  {
    return createUserWithName(transaction, "someone");
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
      final var m = groups.groupMembershipGet(user.id());
      assertEquals(List.of(new EIGroupRoles(groupName, Set.of(FOUNDER))), m);
    }

    groups.groupMembershipSet(groupName, user.id(), Set.of());

    {
      final var m = groups.groupMembershipGet(user.id());
      assertEquals(List.of(new EIGroupRoles(groupName, Set.of())), m);
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
          groups.groupMembershipGet(randomUUID());
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

    final var user = createUser(transaction);

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
        new InProgress(timeNow())
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

    final var requestSucceeded =
      new EIGroupCreationRequest(
        request.groupName(),
        request.userFounder(),
        request.token(),
        new Succeeded(
          request.status().timeStarted(),
          timeNow())
      );

    groups.groupCreationRequestComplete(requestSucceeded);

    {
      final var after0 =
        groups.groupCreationRequestsForUser(user.id()).get(0);
      final var after1 =
        groups.groupCreationRequest(request.token())
          .orElseThrow();

      assertEquals(
        Succeeded.class,
        after0.status().getClass()
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
        "%s|%s".formatted(groupName, request.token())),
      new ExpectedEvent(
        "GROUP_CREATION_REQUEST_SUCCEEDED",
        "%s|%s".formatted(groupName, request.token())),
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

    final var user = createUser(transaction);

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
        new InProgress(timeNow())
      );

    final var request1 =
      new EIGroupCreationRequest(
        groupName,
        user.id(),
        new EIToken(
          "71573B922A87ABC3FD1A957F2CFA09D9E16998567DD878A85E12166112751806"),
        new InProgress(timeNow())
      );

    groups.groupCreationRequestStart(request0);

    final var ex =
      assertThrows(EIServerDatabaseException.class, () -> {
        groups.groupCreationRequestComplete(request1);
      });

    assertEquals("group-request-nonexistent", ex.errorCode());
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

    final var user = createUser(transaction);

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
        new InProgress(timeNow())
      );

    final var ex =
      assertThrows(EIServerDatabaseException.class, () -> {
        groups.groupCreationRequestComplete(request0);
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

    final var user = createUser(transaction);

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
        new InProgress(timeNow())
      );

    final var requestSucceeded =
      new EIGroupCreationRequest(
        request0.groupName(),
        request0.userFounder(),
        request0.token(),
        new Succeeded(
          request0.status().timeStarted(),
          timeNow())
      );

    groups.groupCreationRequestStart(request0);
    groups.groupCreate(groupName, user.id());

    {
      final var ex =
        assertThrows(EIServerDatabaseException.class, () -> {
          groups.groupCreationRequestComplete(requestSucceeded);
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

    final var user = createUser(transaction);

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
        new InProgress(timeNow())
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

    final var user = createUser(transaction);

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
        new InProgress(timeNow())
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

    final var user = createUser(transaction);

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
        new InProgress(timeNow())
      );

    final var requestFailed =
      new EIGroupCreationRequest(
        request.groupName(),
        request.userFounder(),
        request.token(),
        new EIGroupCreationRequestStatusType.Failed(
          request.status().timeStarted(),
          timeNow(),
          "This failed.")
      );

    groups.groupCreationRequestStart(request);

    assertEquals(
      List.of(request),
      groups.groupCreationRequestsForUser(user.id())
    );

    groups.groupCreationRequestComplete(requestFailed);

    {
      final var after =
        groups.groupCreationRequestsForUser(user.id()).get(0);

      final var status = (Failed) after.status();
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
        "%s|%s".formatted(groupName, request.token())),
      new ExpectedEvent(
        "GROUP_CREATION_REQUEST_FAILED",
        "%s|%s".formatted(groupName, request.token()))
    );
  }

  /**
   * Group requests can be created multiple times.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreateRequestMultiple()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var user = createUser(transaction);

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
        new InProgress(timeNow())
      );

    final var request1 =
      new EIGroupCreationRequest(
        groupName,
        user.id(),
        new EIToken(
          "71573B922A87ABC3FD1A957F2CFA09D9E16998567DD878A85E12166112751806"),
        new InProgress(timeNow())
      );

    final var request2 =
      new EIGroupCreationRequest(
        groupName,
        user.id(),
        new EIToken(
          "D9CD63F187DB2DAEA1371289508C63A7A24C46316F15AC61F030A7D6EA423915"),
        new InProgress(timeNow())
      );

    final var request3 =
      new EIGroupCreationRequest(
        groupName,
        user.id(),
        new EIToken(
          "74284D9DCBCC09928CA5D7D6187270A62AC1B58CCDC4A44B81E47257FFA53B9E"),
        new InProgress(timeNow())
      );

    final var request4 =
      new EIGroupCreationRequest(
        groupName,
        user.id(),
        new EIToken(
          "8FE215591C32391AD99DCF732BE3CC4B6FC9AF3A5E92EC4F7C62F19D9B9683AA"),
        new InProgress(timeNow())
      );

    final var request5 =
      new EIGroupCreationRequest(
        new EIGroupName("com.io7m.other"),
        user.id(),
        new EIToken(
          "73CB3858A687A8494CA3323053016282F3DAD39D42CF62CA4E79DDA2AAC7D9AC"),
        new InProgress(timeNow())
      );

    groups.groupCreationRequestStart(request0);
    sleep(1000L);
    groups.groupCreationRequestStart(request1);
    sleep(1000L);
    groups.groupCreationRequestStart(request2);
    sleep(1000L);
    groups.groupCreationRequestStart(request3);
    sleep(1000L);
    groups.groupCreationRequestStart(request4);
    sleep(1000L);
    groups.groupCreationRequestStart(request5);
    sleep(1000L);

    transaction.commit();

    {
      final var requests =
        groups.groupCreationRequestsActive();

      assertEquals(request0.token(), requests.get(0).token());
      assertEquals(request1.token(), requests.get(1).token());
      assertEquals(request2.token(), requests.get(2).token());
      assertEquals(request3.token(), requests.get(3).token());
      assertEquals(request4.token(), requests.get(4).token());
      assertEquals(request5.token(), requests.get(5).token());
    }

    final var requestAfter0 =
      request0.withStatus(new Failed(timeNow(), timeNow(), "Failed 0"));
    final var requestAfter1 =
      request1.withStatus(new Failed(timeNow(), timeNow(), "Failed 0"));
    final var requestAfter2 =
      request2.withStatus(new Succeeded(timeNow(), timeNow()));

    groups.groupCreationRequestComplete(requestAfter0);
    groups.groupCreationRequestComplete(requestAfter1);
    groups.groupCreationRequestComplete(requestAfter2);
    assertTrue(groups.groupExists(groupName));

    {
      final var requests =
        groups.groupCreationRequestsForUser(user.id());

      assertEquals(request0.token(), requests.get(0).token());
      assertEquals(request1.token(), requests.get(1).token());
      assertEquals(request2.token(), requests.get(2).token());
      assertEquals(request3.token(), requests.get(3).token());
      assertEquals(request4.token(), requests.get(4).token());
      assertEquals(request5.token(), requests.get(5).token());
      assertEquals(6, requests.size());
    }

    transaction.commit();

    {
      final var requests =
        groups.groupCreationRequestsActive();

      assertEquals(request5.token(), requests.get(0).token());
      assertEquals(1, requests.size());
    }

    {
      final var requests =
        groups.groupCreationRequestsObsolete();

      assertEquals(request3.token(), requests.get(0).token());
      assertEquals(request4.token(), requests.get(1).token());
      assertEquals(2, requests.size());
    }
  }

  /**
   * Group invites can be created.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupInvites()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var user0 =
      createUserWithName(transaction, "someone0");
    final var user1 =
      createUserWithName(transaction, "someone1");

    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var groupName =
      new EIGroupName("com.io7m.eigion.test");

    groups.groupCreate(groupName, user0.id());
    groups.groupMembershipSet(groupName, user0.id(), allOf(EIGroupRole.class));

    transaction.userIdSet(user0.id());
    transaction.commit();
    transaction.userIdSet(user0.id());

    final var invitesBefore =
      groups.groupInvitesCreatedByUser();
    assertEquals(List.of(), invitesBefore);

    final var invite0 =
      groups.groupInvite(groupName, user1.id());
    final var invite1 =
      groups.groupInvite(groupName, user1.id());
    final var invites =
      groups.groupInvitesCreatedByUser();

    assertEquals(invite0, invite1);
    assertEquals(List.of(invite0), invites);
  }

  /**
   * A user cannot invite itself.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupInvitesUsersSame()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial("someone", "12345678");

    final var transaction =
      this.transactionOf(EIGION);

    transaction.adminIdSet(adminId);

    final var user0 =
      createUserWithName(transaction, "someone0");

    final var groups =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    final var groupName =
      new EIGroupName("com.io7m.eigion.test");

    groups.groupCreate(groupName, user0.id());
    groups.groupMembershipSet(groupName, user0.id(), allOf(EIGroupRole.class));

    transaction.userIdSet(user0.id());

    final var ex =
      assertThrows(EIServerDatabaseException.class, () -> {
        groups.groupInvite(groupName, user0.id());
      });
    assertEquals("group-inviter-invitee", ex.errorCode());
  }
}
