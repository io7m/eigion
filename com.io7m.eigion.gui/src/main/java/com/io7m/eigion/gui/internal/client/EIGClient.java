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
import com.io7m.eigion.client.api.EIClientLoginStatusType;
import com.io7m.eigion.client.api.EIClientNewsItem;
import com.io7m.eigion.client.api.EIClientOnline;
import com.io7m.eigion.client.api.EIClientType;
import com.io7m.eigion.client.vanilla.EIClients;
import com.io7m.eigion.gui.EIGConfiguration;
import com.io7m.eigion.services.api.EIServiceType;
import com.io7m.eigion.taskrecorder.EITask;
import com.io7m.jattribute.core.AttributeReadableType;
import com.io7m.jattribute.core.AttributeSubscriptionType;
import com.io7m.jattribute.core.AttributeType;
import com.io7m.jattribute.core.Attributes;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jmulticlose.core.ClosingResourceFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.io7m.eigion.client.api.EIClientLoggedIn.CLIENT_LOGGED_IN;
import static com.io7m.eigion.client.api.EIClientOnline.CLIENT_OFFLINE;
import static com.io7m.eigion.client.api.EIClientOnline.CLIENT_ONLINE;
import static com.io7m.eigion.gui.internal.client.EIGNewsStatusFetching.NEWS_STATUS_FETCHING;
import static com.io7m.eigion.gui.internal.client.EIGNewsStatusInitial.NEWS_STATUS_INITIAL;
import static com.io7m.eigion.gui.internal.client.EIGNewsStatusOffline.NEWS_STATUS_OFFLINE;

/**
 * An asynchronous client service.
 */

public final class EIGClient implements EIServiceType, AutoCloseable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIGClient.class);

  private final EIClientType client;
  private final ExecutorService executor;
  private final AttributeSubscriptionType onlineSubscription;
  private final CloseableCollectionType<ClosingResourceFailedException> resources;
  private final AttributeType<EIGClientNewsStatusType> newsStatus;
  private final AttributeSubscriptionType loginSubscription;

  /**
   * An asynchronous client service.
   *
   * @param inConfiguration The UI configuration
   */

  public EIGClient(
    final EIGConfiguration inConfiguration)
  {

    this.resources = CloseableCollection.create();

    this.executor =
      Executors.newCachedThreadPool(r -> {
        final var thread = new Thread(r);
        thread.setDaemon(true);
        thread.setName(String.format(
          "com.io7m.eigion.client[%d]",
          thread.getId())
        );
        return thread;
      });

    this.resources.add(this.executor::shutdown);

    this.client =
      new EIClients()
        .create(
          EIClientConfiguration.builder(
            inConfiguration.directories(),
            inConfiguration.serverConfiguration().baseURI()
          ).build()
        );

    this.resources.add(this.client);

    final var attributes =
      Attributes.create(e -> LOG.error("subscriber exception: ", e));

    this.newsStatus =
      attributes.create(NEWS_STATUS_INITIAL);

    this.onlineSubscription =
      this.client.onlineStatus()
        .subscribe(this::onOnlineStatusChanged);

    this.resources.add(this.onlineSubscription);

    this.loginSubscription =
      this.client.loginStatus()
        .subscribe(this::onLoginStatusChanged);

    this.resources.add(this.loginSubscription);
  }

  private void onLoginStatusChanged(
    final EIClientLoginStatusType oldValue,
    final EIClientLoginStatusType newValue)
  {
    if (newValue == CLIENT_LOGGED_IN) {
      this.news();
      return;
    }
  }

  private void onOnlineStatusChanged(
    final EIClientOnline oldValue,
    final EIClientOnline newValue)
  {
    if (oldValue == CLIENT_ONLINE && newValue == CLIENT_OFFLINE) {
      this.newsStatus.set(NEWS_STATUS_OFFLINE);
      return;
    }
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
        final var task = this.client.login(name, password);
        future.complete(task);
      } catch (final Exception e) {
        future.completeExceptionally(e);
      }
    });
    return future;
  }

  @Override
  public void close()
    throws Exception
  {
    this.resources.close();
  }

  /**
   * @return A future representing the task in progress
   *
   * @see EIClientType#news()
   */

  public CompletableFuture<EITask<List<EIClientNewsItem>>> news()
  {
    this.newsStatus.set(NEWS_STATUS_FETCHING);

    final var future = new CompletableFuture<EITask<List<EIClientNewsItem>>>();
    this.executor.execute(() -> {
      try {
        final var news = this.client.news();
        this.newsStatus.set(
          new EIGNewsStatusAvailable(news.result().orElseThrow()));
        future.complete(news);
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

  /**
   * @return The online/offline status
   *
   * @see EIClientType#onlineStatus()
   */

  public AttributeReadableType<EIClientOnline> onlineStatus()
  {
    return this.client.onlineStatus();
  }

  /**
   * @return The news status
   */

  public AttributeReadableType<EIGClientNewsStatusType> newsStatus()
  {
    return this.newsStatus;
  }

  /**
   * @return The login status
   */

  public AttributeReadableType<EIClientLoginStatusType> loginStatus()
  {
    return this.client.loginStatus();
  }

  /**
   * @param mode The online/offline mode
   *
   * @see EIClientType#onlineSet(EIClientOnline)
   */

  public void onlineSet(
    final EIClientOnline mode)
  {
    this.client.onlineSet(mode);
  }
}
