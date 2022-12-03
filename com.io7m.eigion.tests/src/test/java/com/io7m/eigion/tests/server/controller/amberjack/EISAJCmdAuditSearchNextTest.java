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
import com.io7m.eigion.model.EIPage;
import com.io7m.eigion.model.EIPermission;
import com.io7m.eigion.model.EIPermissionSet;
import com.io7m.eigion.protocol.amberjack.EIAJCommandAuditSearchNext;
import com.io7m.eigion.protocol.amberjack.EIAJResponseAuditSearch;
import com.io7m.eigion.server.controller.amberjack.EISAJCmdAuditSearchNext;
import com.io7m.eigion.server.controller.command_exec.EISCommandExecutionFailure;
import com.io7m.eigion.server.database.api.EISDatabaseAuditEventsSearchType;
import com.io7m.eigion.server.database.api.EISDatabaseAuditQueriesType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.SECURITY_POLICY_DENIED;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.USAGE_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public final class EISAJCmdAuditSearchNextTest
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

    final var handler = new EISAJCmdAuditSearchNext();
    final var ex =
      assertThrows(EISCommandExecutionFailure.class, () -> {
        handler.execute(
          context,
          new EIAJCommandAuditSearchNext()
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
      new EIPage<EIAuditEvent>(List.of(), 2, 3, 1L);

    when(search.pageNext(any()))
      .thenReturn(page);

    final var transaction = this.transaction();
    when(transaction.queries(EISDatabaseAuditQueriesType.class))
      .thenReturn(audits);

    context.session()
      .setAuditSearch(search);

    /* Act. */

    final var handler =
      new EISAJCmdAuditSearchNext();
    final var response =
      handler.execute(
        context,
        new EIAJCommandAuditSearchNext()
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

    verify(search)
      .pageNext(audits);
    verify(transaction)
      .queries(EISDatabaseAuditQueriesType.class);

    verifyNoMoreInteractions(search);
    verifyNoMoreInteractions(audits);
    verifyNoMoreInteractions(transaction);
  }

  /**
   * It's not possible to request the next page when the first page hasn't been requested.
   *
   * @throws Exception On errors
   */

  @Test
  public void testSearchingRequiredStart()
    throws Exception
  {
    /* Arrange. */

    final var user0 =
      this.createUser(EIPermissionSet.of(EIPermission.AUDIT_READ));
    final var context =
      this.createContextAndSession(user0);

    final var audits =
      mock(EISDatabaseAuditQueriesType.class);

    final var transaction = this.transaction();
    when(transaction.queries(EISDatabaseAuditQueriesType.class))
      .thenReturn(audits);

    /* Act. */

    final var handler =
      new EISAJCmdAuditSearchNext();
    final var ex =
      assertThrows(EISCommandExecutionFailure.class, () -> {
        handler.execute(
          context,
          new EIAJCommandAuditSearchNext()
        );
      });

    /* Assert. */

    assertEquals(USAGE_ERROR, ex.errorCode());
  }
}
