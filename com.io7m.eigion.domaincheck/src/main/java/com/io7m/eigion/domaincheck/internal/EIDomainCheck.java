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
import com.io7m.eigion.model.EIGroupCreationRequest;
import com.io7m.eigion.model.EIGroupCreationRequestStatusType.Failed;
import com.io7m.eigion.model.EIGroupCreationRequestStatusType.Succeeded;
import com.io7m.jdeferthrow.core.ExceptionTracker;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A single domain check operation.
 */

public final class EIDomainCheck implements Runnable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIDomainCheck.class);

  private final EIDomainCheckerConfiguration configuration;
  private final EIGroupCreationRequest request;
  private final Tracer tracer;
  private final CompletableFuture<EIGroupCreationRequest> future;

  /**
   * A single domain check operation.
   *
   * @param inConfiguration The configuration
   * @param inRequest       The request
   * @param inTracer        The telemetry tracer
   * @param inFuture        The future representing the operation in progress
   */

  public EIDomainCheck(
    final EIDomainCheckerConfiguration inConfiguration,
    final EIGroupCreationRequest inRequest,
    final Tracer inTracer,
    final CompletableFuture<EIGroupCreationRequest> inFuture)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "inConfiguration");
    this.request =
      Objects.requireNonNull(inRequest, "request");
    this.tracer =
      Objects.requireNonNull(inTracer, "tracer");
    this.future =
      Objects.requireNonNull(inFuture, "future");
  }

  @Override
  public void run()
  {
    try {
      this.future.complete(this.check());
    } catch (final Throwable e) {
      this.future.completeExceptionally(e);
    }
  }

  private EIGroupCreationRequest check()
    throws InterruptedException
  {
    final var span =
      this.tracer.spanBuilder("DomainCheck")
        .setSpanKind(SpanKind.CLIENT)
        .setAttribute("DOMAIN_CHECK_GROUP", this.request.groupName().value())
        .setAttribute("DOMAIN_CHECK_TOKEN", this.request.token().value())
        .startSpan();

    try (var ignored = span.makeCurrent()) {
      return this.checkAll(span);
    } finally {
      span.end();
    }
  }

  private EIGroupCreationRequest checkAll(
    final Span span)
    throws InterruptedException
  {
    try {
      MDC.put("group-check-group", this.request.groupName().value());
      MDC.put("group-check-token", this.request.token().value());

      final var client =
        this.configuration.httpClient();
      final var tokenExpected =
        this.request.token().value().getBytes(UTF_8);
      final var tokenReceived =
        new byte[tokenExpected.length];

      final var exceptions = new ExceptionTracker<Exception>();
      for (final var requestURI : this.request.verificationURIs()) {
        final var success =
          this.checkOneURI(
            client,
            tokenExpected,
            tokenReceived,
            exceptions,
            requestURI
          );
        if (success != null) {
          return success;
        }
      }

      try {
        exceptions.throwIfNecessary();
      } catch (final Exception e) {
        span.recordException(e);
        return this.failedDueToException(e);
      }

      return this.failedDueToException(new IllegalStateException());
    } finally {
      MDC.remove("group-check-domain");
      MDC.remove("group-check-domain");
      MDC.remove("group-check-token");
    }
  }

  private EIGroupCreationRequest checkOneURI(
    final HttpClient client,
    final byte[] tokenExpected,
    final byte[] tokenReceived,
    final ExceptionTracker<Exception> exceptions,
    final URI requestURI)
    throws InterruptedException
  {
    final var span =
      this.tracer.spanBuilder("DomainCheck.URI")
        .setSpanKind(SpanKind.CLIENT)
        .setAttribute("DOMAIN_CHECK_URI", requestURI.toString())
        .startSpan();

    try (var ignored = span.makeCurrent()) {
      try {
        MDC.put("group-check-domain", requestURI.toString());

        final var httpRequest =
          HttpRequest.newBuilder(requestURI)
            .GET()
            .build();

        LOG.debug("sending request");
        final var response =
          client.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

        final var statusCode = response.statusCode();
        LOG.debug("status {}", Integer.valueOf(statusCode));
        if (statusCode >= 400) {
          throw new IOException("%s: %d".formatted(requestURI, statusCode));
        }

        try (var stream = response.body()) {
          stream.readNBytes(tokenReceived, 0, tokenReceived.length);
        }

        LOG.debug("checking");
        if (Arrays.equals(tokenExpected, tokenReceived)) {
          return this.success();
        }

        return this.failedDueToTokenMismatch(tokenExpected, tokenReceived);
      } catch (final IOException e) {
        span.recordException(e);
        exceptions.addException(e);
      }
      return null;
    } finally {
      span.end();
    }
  }

  private EIGroupCreationRequest failedDueToException(
    final Exception e)
  {
    final var message = new StringBuilder(128);
    final var writer = new StringWriter();
    try (var pWriter = new PrintWriter(writer)) {
      e.printStackTrace(pWriter);
      pWriter.flush();
    }
    message.append(writer);

    return new EIGroupCreationRequest(
      this.request.groupName(),
      this.request.userFounder(),
      this.request.token(),
      new Failed(
        this.request.status().timeStarted(),
        OffsetDateTime.now(this.configuration.clock()),
        message.toString()
      )
    );
  }

  private EIGroupCreationRequest failedDueToTokenMismatch(
    final byte[] tokenExpected,
    final byte[] tokenReceived)
  {
    final var lineSeparator = System.lineSeparator();
    final var message = new StringBuilder(128);
    message.append("Token did not match.");
    message.append(lineSeparator);
    message.append("  Expected: ");
    message.append(new String(tokenExpected, UTF_8));
    message.append(lineSeparator);
    message.append("  Received: ");
    message.append(new String(tokenReceived, UTF_8));
    message.append(lineSeparator);

    return new EIGroupCreationRequest(
      this.request.groupName(),
      this.request.userFounder(),
      this.request.token(),
      new Failed(
        this.request.status().timeStarted(),
        OffsetDateTime.now(this.configuration.clock()),
        message.toString()
      )
    );
  }

  private EIGroupCreationRequest success()
  {
    return new EIGroupCreationRequest(
      this.request.groupName(),
      this.request.userFounder(),
      this.request.token(),
      new Succeeded(
        this.request.status().timeStarted(),
        OffsetDateTime.now(this.configuration.clock())
      )
    );
  }
}
