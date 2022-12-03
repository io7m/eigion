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


package com.io7m.eigion.server.service.telemetry.api;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

import java.util.Objects;

/**
 * A no-op telemetry service.
 */

public final class EISTelemetryNoOp implements EISTelemetryServiceType
{
  private final OpenTelemetry openTelemetry;
  private final Tracer tracer;

  private EISTelemetryNoOp(
    final OpenTelemetry inOpenTelemetry,
    final Tracer inTracer)
  {
    this.openTelemetry =
      Objects.requireNonNull(inOpenTelemetry, "openTelemetry");
    this.tracer =
      Objects.requireNonNull(inTracer, "tracer");
  }

  /**
   * @return A completely no-op service
   */

  public static EISTelemetryServiceType noop()
  {
    final var noop = OpenTelemetry.noop();
    return new EISTelemetryNoOp(
      noop,
      noop.getTracer("noop")
    );
  }

  /**
   * @return The main tracer
   */

  @Override
  public Tracer tracer()
  {
    return this.tracer;
  }

  /**
   * @return The OpenTelemetry instance
   */

  @Override
  public OpenTelemetry openTelemetry()
  {
    return this.openTelemetry;
  }

  @Override
  public String toString()
  {
    return "[EISTelemetryNoOp 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }

  @Override
  public String description()
  {
    return "Server no-op telemetry service.";
  }

}
