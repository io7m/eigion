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

package com.io7m.eigion.server.service.sessions;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.server.service.idstore.EISIdstoreClientsType;
import com.io7m.idstore.user_client.api.IdUClientType;
import com.io7m.jaffirm.core.Preconditions;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

/**
 * A service to create and manage sessions.
 */

public final class EISessionService
  implements EISessionServiceType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EISessionService.class);

  private final ObservableLongMeasurement sessionsGauge;
  private final EISIdstoreClientsType clients;
  private final Cache<EISessionSecretIdentifier, EISession> sessions;
  private final ConcurrentMap<EISessionSecretIdentifier, EISession> sessionsMap;

  /**
   * A service to create and manage sessions.
   *
   * @param inClients    The idstore client service
   * @param inTelemetry  The telemetry service
   * @param inExpiration The session expiration time
   */

  public EISessionService(
    final EISIdstoreClientsType inClients,
    final OpenTelemetry inTelemetry,
    final Duration inExpiration)
  {
    this.clients =
      Objects.requireNonNull(inClients, "clients");

    this.sessions =
      Caffeine.newBuilder()
        .expireAfterAccess(inExpiration)
        .<EISessionSecretIdentifier, EISession>evictionListener(
          (key, val, removalCause) -> this.onSessionRemoved(val, removalCause))
        .build();

    this.sessionsMap =
      this.sessions.asMap();

    final var meter =
      inTelemetry.meterBuilder(EISessionService.class.getCanonicalName())
        .build();

    this.sessionsGauge =
      meter.gaugeBuilder("eigion.activeUserSessions")
        .setDescription("Active user sessions.")
        .ofLongs()
        .buildObserver();
  }

  @Override
  public String description()
  {
    return "User session service.";
  }

  @Override
  public String toString()
  {
    return "[EISessionUserService 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }

  private void onSessionRemoved(
    final EISession session,
    final RemovalCause removalCause)
  {
    closeSession(session);

    final var sizeNow = this.sessions.estimatedSize();
    LOG.debug(
      "delete session ({}) ({} now active)",
      removalCause,
      Long.toUnsignedString(sizeNow)
    );
    this.sessionsGauge.record(sizeNow);
  }

  @Override
  public Optional<EISession> findSession(
    final EISessionSecretIdentifier id)
  {
    return Optional.ofNullable(
      this.sessionsMap.get(Objects.requireNonNull(id, "id"))
    );
  }

  @Override
  public EISession createSession(
    final IdUClientType client,
    final EIUser user)
  {
    Objects.requireNonNull(client, "client");
    Objects.requireNonNull(user, "user");

    final var id =
      EISessionSecretIdentifier.generate();

    Preconditions.checkPreconditionV(
      !this.sessionsMap.containsKey(id),
      "Session ID cannot already have been used."
    );

    final var session = new EISession(user, id, client);
    this.sessions.put(id, session);
    this.sessionsGauge.record(this.sessions.estimatedSize());
    return session;
  }

  @Override
  public void deleteSession(
    final EISessionSecretIdentifier id)
  {
    Objects.requireNonNull(id, "id");

    final var session = this.sessionsMap.get(id);
    this.sessions.invalidate(id);
    closeSession(session);

    this.sessionsGauge.record(this.sessions.estimatedSize());
  }

  static void closeSession(
    final EISession session)
  {
    if (session != null) {
      try {
        session.close();
      } catch (final Exception e) {
        LOG.error("error closing session: ", e);
      }
    }
  }
}
