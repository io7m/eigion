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

package com.io7m.eigion.server.service.maintenance;

import com.io7m.eigion.server.database.api.EISDatabaseMaintenanceQueriesType;
import com.io7m.eigion.server.database.api.EISDatabaseType;
import com.io7m.eigion.server.service.clock.EISClock;
import com.io7m.eigion.server.service.telemetry.api.EISTelemetryServiceType;
import com.io7m.eigion.services.api.EIServiceType;
import io.opentelemetry.api.trace.SpanKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.io7m.eigion.server.database.api.EISDatabaseRole.EIGION;

/**
 * A service that performs nightly database maintenance.
 */

public final class EISMaintenanceService
  implements EIServiceType, AutoCloseable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EISMaintenanceService.class);

  private final ScheduledExecutorService executor;
  private final EISTelemetryServiceType telemetry;
  private final EISDatabaseType database;

  private EISMaintenanceService(
    final ScheduledExecutorService inExecutor,
    final EISTelemetryServiceType inTelemetry,
    final EISDatabaseType inDatabase)
  {
    this.executor =
      Objects.requireNonNull(inExecutor, "executor");
    this.telemetry =
      Objects.requireNonNull(inTelemetry, "telemetry");
    this.database =
      Objects.requireNonNull(inDatabase, "database");
  }

  /**
   * A service that performs nightly database maintenance.
   *
   * @param clock     The clock
   * @param telemetry The telemetry service
   * @param database  The database
   *
   * @return The service
   */

  public static EISMaintenanceService create(
    final EISClock clock,
    final EISTelemetryServiceType telemetry,
    final EISDatabaseType database)
  {
    Objects.requireNonNull(clock, "clock");
    Objects.requireNonNull(telemetry, "telemetry");
    Objects.requireNonNull(database, "database");

    final var executor =
      Executors.newSingleThreadScheduledExecutor(r -> {
        final var thread = new Thread(r);
        thread.setDaemon(true);
        thread.setName(
          "com.io7m.eigion.server.service.maintenance.EISMaintenanceService[%d]".formatted(
            thread.getId()));
        return thread;
      });

    final var maintenanceService =
      new EISMaintenanceService(executor, telemetry, database);

    final var timeNow =
      clock.now();
    final var timeNextMidnight =
      timeNow.withHour(0)
        .withMinute(0)
        .withSecond(0)
        .plusDays(1L);

    final var initialDelay =
      Duration.between(timeNow, timeNextMidnight).toSeconds();

    final var period =
      Duration.of(1L, ChronoUnit.DAYS)
        .toSeconds();

    /*
     * Run maintenance as soon as the service starts.
     */

    executor.submit(maintenanceService::runMaintenance);

    /*
     * Schedule maintenance to run at each midnight.
     */

    executor.scheduleAtFixedRate(
      maintenanceService::runMaintenance,
      initialDelay,
      period,
      TimeUnit.SECONDS
    );

    return maintenanceService;
  }

  private void runMaintenance()
  {
    LOG.info("maintenance task starting");

    final var span =
      this.telemetry.tracer()
        .spanBuilder("Maintenance")
        .setSpanKind(SpanKind.INTERNAL)
        .startSpan();

    try (var ignored = span.makeCurrent()) {
      try (var connection =
             this.database.openConnection(EIGION)) {
        try (var transaction =
               connection.openTransaction()) {
          final var queries =
            transaction.queries(EISDatabaseMaintenanceQueriesType.class);
          queries.runMaintenance();
          transaction.commit();
          LOG.info("maintenance task completed successfully");
        }
      }
    } catch (final Exception e) {
      LOG.error("maintenance task failed: ", e);
      span.recordException(e);
    } finally {
      span.end();
    }
  }

  @Override
  public String description()
  {
    return "Server maintenance service.";
  }

  @Override
  public void close()
    throws Exception
  {
    this.executor.shutdown();
  }

  @Override
  public String toString()
  {
    return "[EISMaintenanceService 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }
}
