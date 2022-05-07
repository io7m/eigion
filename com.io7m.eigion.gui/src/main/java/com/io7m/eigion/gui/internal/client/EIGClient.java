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


package com.io7m.eigion.gui.internal.client;

import com.io7m.eigion.client.api.EIClientConfiguration;
import com.io7m.eigion.client.api.EIClientNewsItem;
import com.io7m.eigion.client.api.EIClientStatusType;
import com.io7m.eigion.client.api.EIClientType;
import com.io7m.eigion.client.vanilla.EIClients;
import com.io7m.eigion.gui.EIGConfiguration;
import com.io7m.eigion.gui.internal.EIGEventBus;
import com.io7m.eigion.gui.internal.EIGPerpetualSubscriber;
import com.io7m.eigion.services.api.EIServiceType;
import com.io7m.eigion.taskrecorder.EITask;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An asynchronous client service.
 */

public final class EIGClient implements EIServiceType, Closeable
{
  private final EIClientType client;
  private final EIGEventBus eventBus;
  private final ExecutorService executor;

  /**
   * An asynchronous client service.
   *
   * @param configuration The UI configuration
   * @param inEventBus    The event bus service
   */

  public EIGClient(
    final EIGConfiguration configuration,
    final EIGEventBus inEventBus)
  {
    this.eventBus =
      Objects.requireNonNull(inEventBus, "eventBus");

    this.executor =
      Executors.newCachedThreadPool(r -> {
        final var thread = new Thread(r);
        thread.setDaemon(true);
        thread.setName(String.format(
          "com.io7m.eigion.client[%d]",
          thread.getId()));
        return thread;
      });

    this.client =
      new EIClients()
        .create(
          EIClientConfiguration.builder(
            configuration.directories(),
            configuration.serverConfiguration().baseURI()
          ).build()
        );

    this.client.status()
      .subscribe(new EIGPerpetualSubscriber<>(this::onStatus));
  }

  private void onStatus(
    final EIClientStatusType status)
  {
    this.eventBus.submit(new EIGClientStatusChanged(status));
  }

  /**
   * @param name     The username
   * @param password The password
   *
   * @return A future representing the task in progress
   *
   * @see EIClientType#login(String, String)
   */

  public CompletableFuture<EITask<Void>> login(
    final String name,
    final String password)
  {
    final var future = new CompletableFuture<EITask<Void>>();
    this.executor.execute(() -> {
      try {
        future.complete(this.client.login(name, password));
      } catch (final Exception e) {
        future.completeExceptionally(e);
      }
    });
    return future;
  }

  @Override
  public void close()
    throws IOException
  {
    this.executor.shutdown();
    this.client.close();
  }

  /**
   * @return A future representing the task in progress
   *
   * @see EIClientType#news()
   */

  public CompletableFuture<EITask<List<EIClientNewsItem>>> news()
  {
    final var future = new CompletableFuture<EITask<List<EIClientNewsItem>>>();
    this.executor.execute(() -> {
      try {
        future.complete(this.client.news());
      } catch (final Exception e) {
        future.completeExceptionally(e);
      }
    });
    return future;
  }

  @Override
  public String toString()
  {
    return String.format(
      "[EIGClient 0x%08x]",
      Integer.valueOf(this.hashCode())
    );
  }
}
