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

package com.io7m.eigion.tests.server.controller.pike;

import com.io7m.eigion.model.EIGroupCreationRequest;
import com.io7m.eigion.model.EIGroupCreationRequestStatusType.InProgress;
import com.io7m.eigion.model.EIGroupCreationRequestStatusType.Succeeded;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIPermissionSet;
import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.protocol.pike.EIPCommandGroupCreateReady;
import com.io7m.eigion.protocol.pike.EIPResponseGroupCreateReady;
import com.io7m.eigion.server.controller.command_exec.EISCommandExecutionFailure;
import com.io7m.eigion.server.controller.pike.EISPCmdGroupCreateReady;
import com.io7m.eigion.server.database.api.EISDatabaseGroupsPagedQueryType;
import com.io7m.eigion.server.database.api.EISDatabaseGroupsQueriesType;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.GROUP_REQUEST_NONEXISTENT;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.GROUP_REQUEST_WRONG_STATE;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.SECURITY_POLICY_DENIED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public final class EISPCmdGroupCreateReadyTest
  extends EISPCmdAbstractContract
{
  /**
   * Readyling an existing group creation works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testReadyOK()
    throws Exception
  {
    /* Arrange. */

    final var user0 =
      this.createUser(EIPermissionSet.empty());
    final var context =
      this.createContextAndSession(user0);

    final var groups =
      mock(EISDatabaseGroupsQueriesType.class);
    final var search =
      mock(EISDatabaseGroupsPagedQueryType.class);

    final var groupName =
      new EIGroupName("com.example");
    final var token =
      EIToken.generate();

    final var timeStart =
      this.timeStart();

    final var request =
      new EIGroupCreationRequest(
        groupName,
        user0.id(),
        token,
        new InProgress(timeStart)
      );

    when(groups.groupCreationRequest(any()))
      .thenReturn(Optional.of(request));

    final var transaction = this.transaction();
    when(transaction.queries(EISDatabaseGroupsQueriesType.class))
      .thenReturn(groups);

    final var domainChecking = this.domainChecking();

    /* Act. */

    final var handler =
      new EISPCmdGroupCreateReady();
    final var response =
      handler.execute(
        context,
        new EIPCommandGroupCreateReady(token)
      );

    /* Assert. */

    assertEquals(
      new EIPResponseGroupCreateReady(context.requestId()),
      response
    );

    verify(groups, this.once())
      .groupCreationRequest(token);

    verify(transaction, this.once())
      .queries(EISDatabaseGroupsQueriesType.class);

    verify(domainChecking)
      .check(request);

    verifyNoMoreInteractions(domainChecking);
    verifyNoMoreInteractions(search);
    verifyNoMoreInteractions(groups);
    verifyNoMoreInteractions(transaction);
  }

  /**
   * Nonexistent group creation requests cannot be completed.
   *
   * @throws Exception On errors
   */

  @Test
  public void testReadyNonexistent()
    throws Exception
  {
    /* Arrange. */

    final var user0 =
      this.createUser(EIPermissionSet.empty());
    final var context =
      this.createContextAndSession(user0);

    final var groups =
      mock(EISDatabaseGroupsQueriesType.class);
    final var search =
      mock(EISDatabaseGroupsPagedQueryType.class);

    final var token =
      EIToken.generate();

    when(groups.groupCreationRequest(any()))
      .thenReturn(Optional.empty());

    final var transaction = this.transaction();
    when(transaction.queries(EISDatabaseGroupsQueriesType.class))
      .thenReturn(groups);

    /* Act. */

    final var handler =
      new EISPCmdGroupCreateReady();

    final var ex =
      assertThrows(EISCommandExecutionFailure.class, () -> {
        handler.execute(
          context,
          new EIPCommandGroupCreateReady(token)
        );
      });

    assertEquals(GROUP_REQUEST_NONEXISTENT, ex.errorCode());

    /* Assert. */

    verify(groups, this.once())
      .groupCreationRequest(token);

    verify(transaction, this.once())
      .queries(EISDatabaseGroupsQueriesType.class);

    verifyNoMoreInteractions(search);
    verifyNoMoreInteractions(groups);
    verifyNoMoreInteractions(transaction);
  }

  /**
   * Group creation requests cannot be completed if they are not in progress.
   *
   * @throws Exception On errors
   */

  @Test
  public void testReadyNotInProgress()
    throws Exception
  {
    /* Arrange. */

    final var user0 =
      this.createUser(EIPermissionSet.empty());
    final var context =
      this.createContextAndSession(user0);

    final var groups =
      mock(EISDatabaseGroupsQueriesType.class);
    final var search =
      mock(EISDatabaseGroupsPagedQueryType.class);

    final var groupName =
      new EIGroupName("com.example");
    final var token =
      EIToken.generate();

    final var timeStart =
      this.timeStart();
    final var timeNext0 =
      timeStart.plusSeconds(1L);

    final var request =
      new EIGroupCreationRequest(
        groupName,
        user0.id(),
        token,
        new Succeeded(timeStart, timeNext0)
      );

    when(groups.groupCreationRequest(any()))
      .thenReturn(Optional.of(request));

    final var transaction = this.transaction();
    when(transaction.queries(EISDatabaseGroupsQueriesType.class))
      .thenReturn(groups);

    /* Act. */

    final var handler =
      new EISPCmdGroupCreateReady();

    final var ex =
      assertThrows(EISCommandExecutionFailure.class, () -> {
        handler.execute(
          context,
          new EIPCommandGroupCreateReady(token)
        );
      });

    assertEquals(GROUP_REQUEST_WRONG_STATE, ex.errorCode());

    /* Assert. */

    verify(groups, this.once())
      .groupCreationRequest(token);

    verify(transaction, this.once())
      .queries(EISDatabaseGroupsQueriesType.class);

    verifyNoMoreInteractions(search);
    verifyNoMoreInteractions(groups);
    verifyNoMoreInteractions(transaction);
  }

  /**
   * Group creation requests cannot be completed by different users.
   *
   * @throws Exception On errors
   */

  @Test
  public void testReadyNotOwner()
    throws Exception
  {
    /* Arrange. */

    final var user0 =
      this.createUser(EIPermissionSet.empty());
    final var user1 =
      this.createUser(EIPermissionSet.empty());
    final var context =
      this.createContextAndSession(user0);

    final var groups =
      mock(EISDatabaseGroupsQueriesType.class);
    final var search =
      mock(EISDatabaseGroupsPagedQueryType.class);

    final var groupName =
      new EIGroupName("com.example");
    final var token =
      EIToken.generate();

    final var timeStart =
      this.timeStart();

    final var request =
      new EIGroupCreationRequest(
        groupName,
        user1.id(),
        token,
        new InProgress(timeStart)
      );

    when(groups.groupCreationRequest(any()))
      .thenReturn(Optional.of(request));

    final var transaction = this.transaction();
    when(transaction.queries(EISDatabaseGroupsQueriesType.class))
      .thenReturn(groups);

    /* Act. */

    final var handler =
      new EISPCmdGroupCreateReady();

    final var ex =
      assertThrows(EISCommandExecutionFailure.class, () -> {
        handler.execute(
          context,
          new EIPCommandGroupCreateReady(token)
        );
      });

    assertEquals(SECURITY_POLICY_DENIED, ex.errorCode());

    /* Assert. */

    verify(groups, this.once())
      .groupCreationRequest(token);

    verify(transaction, this.once())
      .queries(EISDatabaseGroupsQueriesType.class);

    verifyNoMoreInteractions(search);
    verifyNoMoreInteractions(groups);
    verifyNoMoreInteractions(transaction);
  }
}