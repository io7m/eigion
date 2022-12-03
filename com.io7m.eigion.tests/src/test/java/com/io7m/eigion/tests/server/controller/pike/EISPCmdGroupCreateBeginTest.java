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

import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIPermissionSet;
import com.io7m.eigion.protocol.pike.EIPCommandGroupCreateBegin;
import com.io7m.eigion.protocol.pike.EIPResponseGroupCreateBegin;
import com.io7m.eigion.server.controller.pike.EISPCmdGroupCreateBegin;
import com.io7m.eigion.server.database.api.EISDatabaseGroupsPagedQueryType;
import com.io7m.eigion.server.database.api.EISDatabaseGroupsQueriesType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public final class EISPCmdGroupCreateBeginTest
  extends EISPCmdAbstractContract
{
  /**
   * Creating a new group works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCreateBeginWorks()
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

    when(groups.groupCreationRequestsForUser(any()))
      .thenReturn(List.of());

    final var transaction = this.transaction();
    when(transaction.queries(EISDatabaseGroupsQueriesType.class))
      .thenReturn(groups);

    /* Act. */

    final var handler =
      new EISPCmdGroupCreateBegin();
    final var response =
      handler.execute(
        context,
        new EIPCommandGroupCreateBegin(groupName)
      );

    /* Assert. */

    final var responseT =
      assertInstanceOf(
        EIPResponseGroupCreateBegin.class,
        response
      );

    assertTrue(
      responseT.location()
        .toString()
        .contains(responseT.token().value())
    );
    assertTrue(
      responseT.location()
        .toString()
        .contains("https://example.com/.well-known/eigion-group-challenge/")
    );

    verify(groups, this.once())
      .groupCreationRequestsForUser(user0.id());
    verify(groups, this.once())
      .groupCreationRequestStart(
        argThat(argument -> {
          final var groupMatches =
            argument.groupName().equals(groupName);
          final var userMatches =
            argument.userFounder().equals(user0.id());
          return groupMatches && userMatches;
        })
      );

    verify(transaction, this.once())
      .queries(EISDatabaseGroupsQueriesType.class);

    verifyNoMoreInteractions(search);
    verifyNoMoreInteractions(groups);
    verifyNoMoreInteractions(transaction);
  }
}
