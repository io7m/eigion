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

package com.io7m.eigion.tests.server.controller.amberjack;

import com.io7m.eigion.model.EIAuditEvent;
import com.io7m.eigion.model.EIAuditSearchParameters;
import com.io7m.eigion.model.EIPage;
import com.io7m.eigion.model.EIPermission;
import com.io7m.eigion.model.EIPermissionSet;
import com.io7m.eigion.model.EITimeRange;
import com.io7m.eigion.protocol.amberjack.EIAJCommandAuditSearchBegin;
import com.io7m.eigion.protocol.amberjack.EIAJResponseAuditSearch;
import com.io7m.eigion.server.controller.amberjack.EISAJCmdAuditSearchBegin;
import com.io7m.eigion.server.controller.command_exec.EISCommandExecutionFailure;
import com.io7m.eigion.server.database.api.EISDatabaseAuditEventsSearchType;
import com.io7m.eigion.server.database.api.EISDatabaseAuditQueriesType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.SECURITY_POLICY_DENIED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public final class EISAJCmdAuditSearchBeginTest
  extends EISAJCmdAbstractContract
{
  /**
   * Searching requires AUDIT_READ.
   *
   * @throws Exception On errors
   */

  @Test
  public void testNotAllowed0()
    throws Exception
  {
    /* Arrange. */

    final var user0 =
      this.createUser(EIPermissionSet.empty());
    final var context =
      this.createContextAndSession(user0);

    /* Act. */

    final var handler = new EISAJCmdAuditSearchBegin();
    final var ex =
      assertThrows(EISCommandExecutionFailure.class, () -> {
        handler.execute(
          context,
          new EIAJCommandAuditSearchBegin(
            new EIAuditSearchParameters(
              EITimeRange.largest(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              100
            )
          )
        );
      });

    /* Assert. */

    assertEquals(SECURITY_POLICY_DENIED, ex.errorCode());
  }

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
      this.createUser(EIPermissionSet.of(EIPermission.AUDIT_READ));
    final var context =
      this.createContextAndSession(user0);

    final var audits =
      mock(EISDatabaseAuditQueriesType.class);
    final var search =
      mock(EISDatabaseAuditEventsSearchType.class);

    final var page =
      new EIPage<EIAuditEvent>(List.of(), 1, 1, 0L);

    when(search.pageCurrent(any()))
      .thenReturn(page);
    when(audits.auditEventsSearch(any()))
      .thenReturn(search);

    final var transaction = this.transaction();
    when(transaction.queries(EISDatabaseAuditQueriesType.class))
      .thenReturn(audits);

    final var parameters =
      new EIAuditSearchParameters(
        EITimeRange.largest(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        100
      );

    /* Act. */

    final var handler =
      new EISAJCmdAuditSearchBegin();
    final var response =
      handler.execute(
        context,
        new EIAJCommandAuditSearchBegin(
          parameters
        )
      );

    /* Assert. */

    assertEquals(
      Optional.of(search),
      context.session().auditSearch()
    );
    assertEquals(
      response,
      new EIAJResponseAuditSearch(context.requestId(), page)
    );

    verify(audits)
      .auditEventsSearch(parameters);
    verify(search)
      .pageCurrent(audits);
    verify(transaction)
      .queries(EISDatabaseAuditQueriesType.class);

    verifyNoMoreInteractions(search);
    verifyNoMoreInteractions(audits);
    verifyNoMoreInteractions(transaction);
  }
}
