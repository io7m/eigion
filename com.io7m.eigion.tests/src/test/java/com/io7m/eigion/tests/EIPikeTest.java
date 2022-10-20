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

import com.io7m.eigion.model.EIAuditSearchParameters;
import com.io7m.eigion.model.EIPermission;
import com.io7m.eigion.model.EIPermissionSet;
import com.io7m.eigion.model.EITimeRange;
import com.io7m.eigion.pike.EIPClients;
import com.io7m.eigion.pike.api.EIPClientException;
import com.io7m.eigion.pike.api.EIPClientType;
import com.io7m.eigion.server.database.api.EISDatabaseAuditQueriesType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.OPERATION_NOT_PERMITTED;
import static com.io7m.eigion.server.database.api.EISDatabaseRole.EIGION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class EIPikeTest extends EIWithServerContract
{
  private EIPClients clients;
  private EIPClientType client;

  @BeforeEach
  public void setup()
  {
    this.clients = new EIPClients();
    this.client = this.clients.create(Locale.ROOT);
  }

  @AfterEach
  public void tearDown()
    throws IOException
  {
    this.client.close();
  }

  /**
   * It's not possible to log in if the user does not exist.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginNoSuchUser()
    throws Exception
  {
    final var ex =
      assertThrows(EIPClientException.class, () -> {
        this.client.login(
          "nonexistent",
          "12345678",
          this.server().basePikeURI()
        );
      });

    assertEquals(AUTHENTICATION_ERROR, ex.errorCode());
  }

  /**
   * Logging in works if the user has Pike permissions.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginUserOK()
    throws Exception
  {
    this.setupStandardUserAndLogIn();
  }

  private void checkAuditLog(
    final AuditCheck... auditCheck)
    throws Exception
  {
    final var database = this.server().database();
    try (var c = database.openConnection(EIGION)) {
      try (var t = c.openTransaction()) {
        final var q =
          t.queries(EISDatabaseAuditQueriesType.class);

        final var search =
          q.auditEventsSearch(new EIAuditSearchParameters(
            EITimeRange.largest(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            1000L
          ));

        final var events =
          search.pageCurrent(q).items();

        for (int index = 0; index < auditCheck.length; ++index) {
          final var check = auditCheck[index];
          final var event = events.get(index);
          assertEquals(
            check.type,
            event.type(),
            "[%d] type %s == event type %s"
              .formatted(Integer.valueOf(index), check.type, event.type())
          );
          if (check.message != null) {
            assertEquals(
              check.message,
              event.message(),
              "[%d] message %s == event message %s"
                .formatted(Integer.valueOf(index), check.message, event.message())
            );
          }
        }
      }
    }
  }

  private static AuditCheck check(
    final String type,
    final String message)
  {
    return new AuditCheck(type, message);
  }

  private record AuditCheck(
    String type,
    String message)
  {

  }

  private UUID setupStandardUserAndLogIn(
    final EIPermission... permissions)
    throws Exception
  {
    final var userId =
      this.idstore()
        .createUser("noone", "12345678");

    this.server()
      .configurator()
      .userSetPermissions(userId, EIPermissionSet.of(permissions));

    this.client.login(
      "noone",
      "12345678",
      this.server().basePikeURI()
    );
    return userId;
  }
}
