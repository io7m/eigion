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

import com.io7m.eigion.model.EIGroupMembership;
import com.io7m.eigion.model.EIPage;
import com.io7m.eigion.model.EIPermissionSet;
import com.io7m.eigion.protocol.pike.EIPCommandGroupsPrevious;
import com.io7m.eigion.protocol.pike.EIPResponseGroups;
import com.io7m.eigion.server.controller.command_exec.EISCommandExecutionFailure;
import com.io7m.eigion.server.controller.pike.EISPCmdGroupsPrevious;
import com.io7m.eigion.server.database.api.EISDatabaseGroupsPagedQueryType;
import com.io7m.eigion.server.database.api.EISDatabaseGroupsQueriesType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.USAGE_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public final class EISPCmdGroupsPreviousTest
  extends EISPCmdAbstractContract
{
  /**
   * Searching works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testSearchingWorks()
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

    final var page =
      new EIPage<EIGroupMembership>(List.of(), 2, 3, 1L);

    when(search.pagePrevious(any()))
      .thenReturn(page);

    final var transaction = this.transaction();
    when(transaction.queries(EISDatabaseGroupsQueriesType.class))
      .thenReturn(groups);

    context.session()
      .setGroupRolesOwnSearch(search);

    /* Act. */

    final var handler =
      new EISPCmdGroupsPrevious();
    final var response =
      handler.execute(
        context,
        new EIPCommandGroupsPrevious()
      );

    /* Assert. */

    assertEquals(
      Optional.of(search),
      context.session().groupRolesOwnSearch()
    );
    assertEquals(
      response,
      new EIPResponseGroups(context.requestId(), page)
    );

    verify(search)
      .pagePrevious(groups);
    verify(transaction)
      .queries(EISDatabaseGroupsQueriesType.class);

    verifyNoMoreInteractions(search);
    verifyNoMoreInteractions(groups);
    verifyNoMoreInteractions(transaction);
  }

  /**
   * It's not possible to request the previous page when the first page hasn't
   * been requested.
   *
   * @throws Exception On errors
   */

  @Test
  public void testSearchingRequiredStart()
    throws Exception
  {
    /* Arrange. */

    final var user0 =
      this.createUser(EIPermissionSet.empty());
    final var context =
      this.createContextAndSession(user0);

    final var groups =
      mock(EISDatabaseGroupsQueriesType.class);

    final var transaction = this.transaction();
    when(transaction.queries(EISDatabaseGroupsQueriesType.class))
      .thenReturn(groups);

    /* Act. */

    final var handler =
      new EISPCmdGroupsPrevious();
    final var ex =
      assertThrows(EISCommandExecutionFailure.class, () -> {
        handler.execute(
          context,
          new EIPCommandGroupsPrevious()
        );
      });

    /* Assert. */

    assertEquals(USAGE_ERROR, ex.errorCode());
  }
}
