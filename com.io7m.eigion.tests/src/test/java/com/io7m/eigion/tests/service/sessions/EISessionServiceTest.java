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


package com.io7m.eigion.tests.service.sessions;

import com.io7m.eigion.model.EIPermissionSet;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.server.api.EIServerIdstoreConfiguration;
import com.io7m.eigion.server.service.idstore.EISIdstoreClients;
import com.io7m.eigion.server.service.sessions.EISessionService;
import com.io7m.eigion.server.service.sessions.EISessionServiceType;
import com.io7m.eigion.tests.service.EIServiceContract;
import com.io7m.idstore.user_client.api.IdUClientType;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

public final class EISessionServiceTest
  extends EIServiceContract<EISessionServiceType>
{
  private IdUClientType client;

  @Override
  protected EISessionServiceType createInstanceA()
  {
    return new EISessionService(
      EISIdstoreClients.create(
        Locale.ROOT,
        new EIServerIdstoreConfiguration(
          URI.create("urn:x"),
          URI.create("urn:y"))),
      OpenTelemetry.noop(),
      Duration.ofDays(1L)
    );
  }

  @Override
  protected EISessionServiceType createInstanceB()
  {
    return new EISessionService(
      EISIdstoreClients.create(
        Locale.ROOT,
        new EIServerIdstoreConfiguration(
          URI.create("urn:x"),
          URI.create("urn:y"))),
      OpenTelemetry.noop(),
      Duration.ofDays(1L)
    );
  }

  private static EISessionService createWithExpiration(
    final Duration time)
  {
    return new EISessionService(
      EISIdstoreClients.create(
        Locale.ROOT,
        new EIServerIdstoreConfiguration(
          URI.create("urn:x"),
          URI.create("urn:y"))),
      OpenTelemetry.noop(),
      time
    );
  }

  @BeforeEach
  public void setup()
  {
    this.client = mock(IdUClientType.class);
  }

  /**
   * Sessions have unique identifiers.
   */

  @Test
  public void testSessionCreateUnique()
  {
    final var sessions =
      this.createInstanceA();
    final var user =
      new EIUser(UUID.randomUUID(), EIPermissionSet.of());
    final var session0 =
      sessions.createSession(this.client, user);
    final var session1 =
      sessions.createSession(this.client, user);

    assertNotEquals(session0.id(), session1.id());
  }

  /**
   * Sessions expire.
   *
   * @throws Exception On errors
   */

  @Test
  public void testSessionExpires()
    throws Exception
  {
    final var sessions =
      createWithExpiration(Duration.ofMillis(5L));
    final var user =
      new EIUser(UUID.randomUUID(), EIPermissionSet.of());
    final var session0 =
      sessions.createSession(this.client, user);
    final var session1 =
      sessions.findSession(session0.id())
        .orElseThrow();

    Thread.sleep(10L);

    assertEquals(
      Optional.empty(),
      sessions.findSession(session0.id())
    );
  }

  /**
   * Sessions can be deleted.
   *
   * @throws Exception On errors
   */

  @Test
  public void testSessionDeleted()
    throws Exception
  {
    final var sessions =
      this.createInstanceA();
    final var user =
      new EIUser(UUID.randomUUID(), EIPermissionSet.of());
    final var session0 =
      sessions.createSession(this.client, user);

    sessions.deleteSession(session0.id());

    assertEquals(
      Optional.empty(),
      sessions.findSession(session0.id())
    );
  }
}
