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

package com.io7m.eigion.domaincheck.internal;

import com.io7m.eigion.domaincheck.api.EIDomainCheckerConfiguration;
import com.io7m.eigion.domaincheck.api.EIDomainCheckerType;
import com.io7m.eigion.model.EIGroupCreationRequest;
import io.opentelemetry.api.trace.Tracer;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The default domain checker implementation.
 */

public final class EIDomainChecker implements EIDomainCheckerType
{
  private final EIDomainCheckerConfiguration configuration;
  private final ExecutorService executor;
  private final Tracer tracer;

  private EIDomainChecker(
    final EIDomainCheckerConfiguration inConfiguration,
    final ExecutorService inExecutor)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.executor =
      Objects.requireNonNull(inExecutor, "executor");
    this.tracer =
      inConfiguration.openTelemetry()
        .getTracer("com.io7m.eigion.domaincheck", version());
  }

  /**
   * @param configuration The configuration
   *
   * @return A new domain checker
   */

  public static EIDomainCheckerType create(
    final EIDomainCheckerConfiguration configuration)
  {
    final var executor =
      Executors.newSingleThreadExecutor(r -> {
        final var thread = new Thread(r);
        thread.setName("com.io7m.eigion.domaincheck[%d]".formatted(thread.getId()));
        thread.setDaemon(true);
        return thread;
      });

    return new EIDomainChecker(configuration, executor);
  }

  @Override
  public CompletableFuture<EIGroupCreationRequest> check(
    final EIGroupCreationRequest request)
  {
    Objects.requireNonNull(request, "request");
    final var future = new CompletableFuture<EIGroupCreationRequest>();
    this.executor.execute(
      new EIDomainCheck(this.configuration, request, this.tracer, future)
    );
    return future;
  }

  @Override
  public void close()
    throws InterruptedException
  {
    this.executor.shutdown();
    this.executor.awaitTermination(5L, TimeUnit.SECONDS);
  }

  private static String version()
  {
    final var p =
      EIDomainChecker.class.getPackage();
    final var v =
      p.getImplementationVersion();

    if (v == null) {
      return "0.0.0";
    }
    return v;
  }
}
