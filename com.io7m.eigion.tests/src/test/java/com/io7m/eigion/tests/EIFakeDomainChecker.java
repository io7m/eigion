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

import com.io7m.eigion.domaincheck.api.EIDomainCheckerType;
import com.io7m.eigion.model.EIGroupCreationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class EIFakeDomainChecker implements EIDomainCheckerType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIFakeDomainChecker.class);

  private final ConcurrentLinkedDeque<CompletableFuture<EIGroupCreationRequest>> futureQueue;

  public EIFakeDomainChecker()
  {
    this.futureQueue = new ConcurrentLinkedDeque<>();
  }

  public void enqueue(
    final CompletableFuture<EIGroupCreationRequest> future)
  {
    this.futureQueue.add(future);
  }

  @Override
  public CompletableFuture<EIGroupCreationRequest> check(
    final EIGroupCreationRequest request)
  {
    LOG.debug("checking: {}", request);

    final var next =
      this.futureQueue.poll();

    if (next == null) {
      LOG.debug("checking: no queued future for {}!", request);
      return CompletableFuture.failedFuture(
        new IllegalStateException("No queued check!")
      );
    }

    return next;
  }

  @Override
  public void close()
    throws Exception
  {

  }
}
