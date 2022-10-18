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

package com.io7m.eigion.server.internal.sessions;

import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.server.internal.EISTelemetryService;
import com.io7m.eigion.services.api.EIServiceType;
import com.io7m.idstore.user_client.api.IdUClientType;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A service to create and manage user sessions.
 */

public final class EISUserSessionService
  implements EIServiceType, HttpSessionListener
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EISUserSessionService.class);

  private final ConcurrentHashMap<String, EISUserSession> sessions;
  private final ObservableLongGauge sessionsGauge;

  /**
   * A service to create and manage user sessions.
   *
   * @param inTelemetry The telemetry service
   */

  public EISUserSessionService(
    final EISTelemetryService inTelemetry)
  {
    this.sessions = new ConcurrentHashMap<>();

    final var meter =
      inTelemetry.openTelemetry()
        .meterBuilder(EISUserSessionService.class.getCanonicalName())
        .build();

    this.sessionsGauge =
      meter.gaugeBuilder("eigion.activeUserSessions")
        .setDescription("Active user sessions.")
        .ofLongs()
        .buildWithCallback(m -> {
          m.record(Integer.toUnsignedLong(this.sessions.size()));
        });
  }

  @Override
  public String description()
  {
    return "User session service.";
  }

  /**
   * Create or get an existing user controller.
   *
   * @param user        The user
   * @param httpSession The session ID
   * @param idClient    The ID client
   *
   * @return A user controller
   */

  public EISUserSession create(
    final EIUser user,
    final HttpSession httpSession,
    final IdUClientType idClient)
  {
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(httpSession, "session");
    Objects.requireNonNull(idClient, "idClient");

    final var id =
      "%s:%s".formatted(user.id(), httpSession.getId());
    final var userSession =
      new EISUserSession(user, httpSession, idClient);

    this.sessions.put(id, userSession);
    return userSession;
  }

  @Override
  public void sessionCreated(
    final HttpSessionEvent se)
  {

  }

  @Override
  public void sessionDestroyed(
    final HttpSessionEvent se)
  {
    final var session = se.getSession();
    final var userId = (UUID) session.getAttribute("UserID");
    final var sessionId = session.getId();

    if (userId != null) {
      this.delete(userId, sessionId);
    }
  }

  /**
   * Delete a user controller if one exists.
   *
   * @param userId    The user ID
   * @param sessionId The session ID
   */

  public void delete(
    final UUID userId,
    final String sessionId)
  {
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(sessionId, "sessionId");

    final var id =
      "%s:%s".formatted(userId, sessionId);
    final var session =
      this.sessions.remove(id);

    try {
      session.close();
    } catch (final Exception e) {
      LOG.error("[{}] could not close session: ", id, e);
    } finally {
      LOG.debug(
        "[{}] delete session ({} now active)",
        id,
        Integer.valueOf(this.sessions.size())
      );
    }
  }

  @Override
  public String toString()
  {
    return "[EISUserSessionService 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }

  /**
   * Find the given user session.
   *
   * @param userId    The user ID
   * @param sessionId The session ID
   *
   * @return A user session
   */

  public Optional<EISUserSession> find(
    final UUID userId,
    final String sessionId)
  {
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(sessionId, "sessionId");

    final var id = "%s:%s".formatted(userId, sessionId);
    return Optional.ofNullable(this.sessions.get(id));
  }
}
