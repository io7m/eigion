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

import com.io7m.eigion.amberjack.EIAJClients;
import com.io7m.eigion.amberjack.api.EIAJClientException;
import com.io7m.eigion.amberjack.api.EIAJClientType;
import com.io7m.eigion.model.EIAuditEvent;
import com.io7m.eigion.model.EIAuditSearchParameters;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIGroupSearchByNameParameters;
import com.io7m.eigion.model.EIPermission;
import com.io7m.eigion.model.EIPermissionSet;
import com.io7m.eigion.model.EITimeRange;
import com.io7m.eigion.server.database.api.EISDatabaseAuditQueriesType;
import com.io7m.eigion.server.database.api.EISDatabaseRole;
import com.io7m.eigion.server.database.api.EISDatabaseType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.OPERATION_NOT_PERMITTED;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.SECURITY_POLICY_DENIED;
import static com.io7m.eigion.model.EIPermission.AMBERJACK_ACCESS;
import static com.io7m.eigion.model.EIPermission.AUDIT_READ;
import static com.io7m.eigion.model.EIPermission.GROUP_CREATE;
import static com.io7m.eigion.model.EIPermission.GROUP_READ;
import static com.io7m.eigion.server.database.api.EISDatabaseRole.EIGION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class EIAmberjackTest extends EIWithServerContract
{
  private EIAJClients clients;
  private EIAJClientType client;

  @BeforeEach
  public void setup()
  {
    this.clients = new EIAJClients();
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
      assertThrows(EIAJClientException.class, () -> {
        this.client.login(
          "nonexistent",
          "12345678",
          this.server().baseAmberjackURI()
        );
      });

    assertEquals(AUTHENTICATION_ERROR, ex.errorCode());
  }

  /**
   * It's not possible to log in if the user has no Amberjack permissions.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginUserNotPermitted0()
    throws Exception
  {
    this.idstore()
      .createUser("noone", "12345678");

    final var ex =
      assertThrows(EIAJClientException.class, () -> {
        this.client.login(
          "noone",
          "12345678",
          this.server().baseAmberjackURI()
        );
      });

    assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
  }

  /**
   * It's not possible to log in if the user has no Amberjack permissions.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginUserNotPermitted1()
    throws Exception
  {
    final var userId =
      this.idstore()
        .createUser("noone", "12345678");

    this.server()
      .configurator()
      .userSetPermissions(userId, EIPermissionSet.empty());

    final var ex =
      assertThrows(EIAJClientException.class, () -> {
        this.client.login(
          "noone",
          "12345678",
          this.server().baseAmberjackURI()
        );
      });

    assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
  }

  /**
   * Logging in works if the user has Amberjack permissions.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginUserOK()
    throws Exception
  {
    this.setupStandardUserAndLogIn(AMBERJACK_ACCESS);
  }

  /**
   * Creating groups requires permissions.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreateDisallowed()
    throws Exception
  {
    this.setupStandardUserAndLogIn(AMBERJACK_ACCESS);

    final var ex =
      assertThrows(EIAJClientException.class, () -> {
        this.client.groupCreate(new EIGroupName("com.io7m.example"));
      });

    assertEquals(SECURITY_POLICY_DENIED, ex.errorCode());
  }

  /**
   * Creating groups works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreateOK()
    throws Exception
  {
    this.setupStandardUserAndLogIn(AMBERJACK_ACCESS, GROUP_CREATE, GROUP_READ);

    this.client.groupCreate(new EIGroupName("com.io7m.example0"));
    this.client.groupCreate(new EIGroupName("com.io7m.example1"));
    this.client.groupCreate(new EIGroupName("com.io7m.example2"));

    final var search =
      this.client.groupSearchByName(
        new EIGroupSearchByNameParameters(Optional.empty(), 2L)
      );

    {
      final var p = search.current();
      assertEquals(1, p.pageIndex());
      assertEquals(2, p.pageCount());
      final var i = p.items();
      assertEquals("com.io7m.example0", i.get(0).value());
      assertEquals("com.io7m.example1", i.get(1).value());
      assertEquals(2, i.size());
    }

    {
      final var p = search.next();
      assertEquals(2, p.pageIndex());
      assertEquals(2, p.pageCount());
      final var i = p.items();
      assertEquals("com.io7m.example2", i.get(0).value());
      assertEquals(1, i.size());
    }

    {
      final var p = search.previous();
      assertEquals(1, p.pageIndex());
      assertEquals(2, p.pageCount());
      final var i = p.items();
      assertEquals("com.io7m.example0", i.get(0).value());
      assertEquals("com.io7m.example1", i.get(1).value());
      assertEquals(2, i.size());
    }

    this.checkAuditLog(
      check("USER_LOGGED_IN", null),
      check("GROUP_CREATED", "com.io7m.example0"),
      check("GROUP_CREATED", "com.io7m.example1"),
      check("GROUP_CREATED", "com.io7m.example2")
    );
  }

  /**
   * Searching the audit log works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAuditLogSearch()
    throws Exception
  {
    final var userId =
      this.setupStandardUserAndLogIn(AMBERJACK_ACCESS, AUDIT_READ);
    final var time =
      OffsetDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"));

    final var database = this.server().database();
    try (var c = database.openConnection(EIGION)) {
      try (var t = c.openTransaction()) {
        final var q =
          t.queries(EISDatabaseAuditQueriesType.class);
        for (int index = 0; index < 100; ++index) {
          q.auditPut(
            userId,
            time,
            "AUDIT_EVENT_%03d".formatted(Integer.valueOf(index)),
            "AUDIT_MESSAGE_%03d".formatted(Integer.valueOf(index))
          );
        }
        t.commit();
      }
    }

    {
      final var search =
        this.client.auditSearch(
          new EIAuditSearchParameters(
            EITimeRange.largest(),
            Optional.empty(),
            Optional.of("AUDIT_EVENT_"),
            Optional.empty(),
            30L));

      {
        final var p = search.current();
        assertEquals(1, p.pageIndex());
        assertEquals(4, p.pageCount());

        final var items = p.items();
        for (int index = 0; index < 30; ++index) {
          final var event = items.get(index);
          final var number = index;
          assertEquals("AUDIT_EVENT_%03d".formatted(number), event.type());
          assertEquals("AUDIT_MESSAGE_%03d".formatted(number), event.message());
        }
        assertEquals(30, items.size());
      }

      {
        final var p = search.next();
        assertEquals(2, p.pageIndex());
        assertEquals(4, p.pageCount());

        final var items = p.items();
        for (int index = 0; index < 30; ++index) {
          final var event = items.get(index);
          final var number = 30 + index;
          assertEquals("AUDIT_EVENT_%03d".formatted(number), event.type());
          assertEquals("AUDIT_MESSAGE_%03d".formatted(number), event.message());
        }
        assertEquals(30, items.size());
      }

      {
        final var p = search.next();
        assertEquals(3, p.pageIndex());
        assertEquals(4, p.pageCount());

        final var items = p.items();
        for (int index = 0; index < 30; ++index) {
          final var event = items.get(index);
          final var number = 60 + index;
          assertEquals("AUDIT_EVENT_%03d".formatted(number), event.type());
          assertEquals("AUDIT_MESSAGE_%03d".formatted(number), event.message());
        }
        assertEquals(30, items.size());
      }

      {
        final var p = search.next();
        assertEquals(4, p.pageIndex());
        assertEquals(4, p.pageCount());

        final var items = p.items();
        for (int index = 0; index < 10; ++index) {
          final var event = items.get(index);
          final var number = 90 + index;
          assertEquals("AUDIT_EVENT_%03d".formatted(number), event.type());
          assertEquals("AUDIT_MESSAGE_%03d".formatted(number), event.message());
        }
        assertEquals(10, items.size());
      }

      {
        final var p = search.previous();
        assertEquals(3, p.pageIndex());
        assertEquals(4, p.pageCount());

        final var items = p.items();
        for (int index = 0; index < 30; ++index) {
          final var event = items.get(index);
          final var number = 60 + index;
          assertEquals("AUDIT_EVENT_%03d".formatted(number), event.type());
          assertEquals("AUDIT_MESSAGE_%03d".formatted(number), event.message());
        }
        assertEquals(30, items.size());
      }

      {
        final var p = search.previous();
        assertEquals(2, p.pageIndex());
        assertEquals(4, p.pageCount());

        final var items = p.items();
        for (int index = 0; index < 30; ++index) {
          final var event = items.get(index);
          final var number = 30 + index;
          assertEquals("AUDIT_EVENT_%03d".formatted(number), event.type());
          assertEquals("AUDIT_MESSAGE_%03d".formatted(number), event.message());
        }
        assertEquals(30, items.size());
      }

      {
        final var p = search.previous();
        assertEquals(1, p.pageIndex());
        assertEquals(4, p.pageCount());

        final var items = p.items();
        for (int index = 0; index < 30; ++index) {
          final var event = items.get(index);
          final var number = index;
          assertEquals("AUDIT_EVENT_%03d".formatted(number), event.type());
          assertEquals("AUDIT_MESSAGE_%03d".formatted(number), event.message());
        }
        assertEquals(30, items.size());
      }
    }
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
      this.server().baseAmberjackURI()
    );
    return userId;
  }
}
