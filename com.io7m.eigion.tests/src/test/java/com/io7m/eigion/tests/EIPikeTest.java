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
import com.io7m.eigion.model.EIGroupCreationRequestStatusType;
import com.io7m.eigion.model.EIGroupName;
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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.eigion.model.EIGroupRole.FOUNDER;
import static com.io7m.eigion.server.database.api.EISDatabaseRole.EIGION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class EIPikeTest extends EIWithServerContract
{
  private EIPClients clients;
  private EIPClientType client;
  private EIInterceptHttpClient httpClient;
  private EIFakeServerDomainCheck domainCheckServer;

  @BeforeEach
  public void setup()
    throws Exception
  {
    this.clients = new EIPClients();
    this.client = this.clients.create(Locale.ROOT);
    this.domainCheckServer = EIFakeServerDomainCheck.create(20000);
    EIFakeServerDomainCheckServlet.RETURN_TOKEN = Optional.empty();
  }

  @AfterEach
  public void tearDown()
    throws Exception
  {
    this.client.close();
    this.domainCheckServer.close();
  }

  private static URI replaceURI(
    final URI u)
  {
    try {
      return new URI(
        "http",
        u.getUserInfo(),
        "localhost",
        20000,
        u.getPath(),
        u.getQuery(),
        u.getFragment()
      );
    } catch (final URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  protected Supplier<HttpClient> httpClients()
  {
    this.httpClient = new EIInterceptHttpClient(
      EIPikeTest::replaceURI,
      HttpClient.newHttpClient()
    );

    return () -> this.httpClient;
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

  /**
   * Creating a group works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreationOK()
    throws Exception
  {
    this.setupStandardUserAndLogIn();

    final var groupName =
      new EIGroupName("com.example");
    final var challenge =
      this.client.groupCreateBegin(groupName);

    assertEquals(groupName, challenge.groupName());

    EIFakeServerDomainCheckServlet.RETURN_TOKEN =
      Optional.of(challenge.token().value());

    this.client.groupCreateReady(challenge.token());

    /*
     * Wait for everything to settle.
     */

    Thread.sleep(1_00L);

    final var groups =
      this.client.groups()
        .current()
        .items();

    assertEquals(groupName, groups.get(0).group());
    assertTrue(groups.get(0).roles().implies(FOUNDER));

    final var requests =
      this.client.groupCreateRequests()
        .current()
        .items();

    assertEquals(1, requests.size());
    assertEquals(EIGroupCreationRequestStatusType.Succeeded.class, requests.get(0).status().getClass());

    this.checkAuditLog(
      new AuditCheck("USER_LOGGED_IN", null),
      new AuditCheck("GROUP_CREATION_REQUESTED", null),
      new AuditCheck("GROUP_CREATION_REQUEST_SUCCEEDED", null),
      new AuditCheck("GROUP_CREATED", "com.example")
    );
  }

  /**
   * Cancelling a group creation works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreationCancel()
    throws Exception
  {
    this.setupStandardUserAndLogIn();

    final var groupName =
      new EIGroupName("com.example");
    final var challenge =
      this.client.groupCreateBegin(groupName);

    assertEquals(groupName, challenge.groupName());

    EIFakeServerDomainCheckServlet.RETURN_TOKEN =
      Optional.of(challenge.token().value());

    this.client.groupCreateCancel(challenge.token());

    /*
     * Wait a second for everything to settle.
     */

    Thread.sleep(1_00L);

    final var groups =
      this.client.groups()
        .current()
        .items();

    assertEquals(0, groups.size());

    final var requests =
      this.client.groupCreateRequests()
        .current()
        .items();

    assertEquals(1, requests.size());
    assertEquals(EIGroupCreationRequestStatusType.Cancelled.class, requests.get(0).status().getClass());

    this.checkAuditLog(
      new AuditCheck("USER_LOGGED_IN", null),
      new AuditCheck("GROUP_CREATION_REQUESTED", null),
      new AuditCheck("GROUP_CREATION_REQUEST_CANCELLED", null)
    );
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
                .formatted(
                  Integer.valueOf(index),
                  check.message,
                  event.message())
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
