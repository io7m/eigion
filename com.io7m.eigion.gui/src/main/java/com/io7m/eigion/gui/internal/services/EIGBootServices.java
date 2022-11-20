/*
 * Copyright © 2021 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

package com.io7m.eigion.gui.internal.services;

import com.io7m.eigion.gui.EIGConfiguration;
import com.io7m.eigion.gui.internal.EIGBackgroundSchedulerService;
import com.io7m.eigion.gui.internal.EIGEventBus;
import com.io7m.eigion.gui.internal.EIGStrings;
import com.io7m.eigion.gui.internal.client.EIGClient;
import com.io7m.eigion.gui.internal.database.EIGDatabase;
import com.io7m.eigion.gui.internal.errors.EIGErrorDialogs;
import com.io7m.eigion.gui.internal.news.EIGNewsParsers;
import com.io7m.eigion.preferences.EIPreferencesService;
import com.io7m.eigion.preferences.EIPreferencesServiceType;
import com.io7m.eigion.services.api.EIServiceDirectory;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import com.io7m.eigion.services.api.EIServiceType;
import com.io7m.eigion.taskrecorder.EITask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The main service directory.
 */

public final class EIGBootServices
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIGBootServices.class);

  private EIGBootServices()
  {

  }

  /**
   * Create a service directory.
   *
   * @param configuration The application configuration
   * @param strings       The strings
   * @param bootEvents    A receiver of boot events
   *
   * @return A new service directory
   */

  public static CompletableFuture<EITask<EIServiceDirectoryType>> create(
    final EIGConfiguration configuration,
    final EIGStrings strings,
    final Consumer<EIBootEvent> bootEvents)
  {
    final var future =
      new CompletableFuture<EITask<EIServiceDirectoryType>>();

    final var thread = new Thread(() -> {
      try {
        future.complete(createServices(configuration, strings, bootEvents));
      } catch (final Throwable e) {
        future.completeExceptionally(e);
      }
    });

    thread.setName("com.io7m.eigion.boot[%d]".formatted(thread.getId()));
    thread.setDaemon(true);
    thread.start();
    return future;
  }

  private static EITask<EIServiceDirectoryType> createServices(
    final EIGConfiguration configuration,
    final EIGStrings strings,
    final Consumer<EIBootEvent> bootEvents)
    throws Exception
  {
    final var services =
      new EIServiceDirectory();
    final var creators =
      new ArrayList<EIBootService<? extends EIServiceType>>();

    creators.add(new EIBootService<>(
      "Loading string resources...",
      EIGStrings.class,
      () -> strings
    ));

    creators.add(new EIBootService<>(
      "Loading preferences...",
      EIPreferencesServiceType.class,
      () -> {
        final var prefs =
          EIPreferencesService.openOrDefault(
            configuration.directories()
              .configurationDirectory()
              .resolve("preferences.xml")
          );
        prefs.update(Function.identity());
        return prefs;
      }
    ));

    creators.add(new EIBootService<>(
      "Loading news parsers...",
      EIGNewsParsers.class,
      EIGNewsParsers::new
    ));

    creators.add(new EIBootService<>(
      "Loading event bus...",
      EIGEventBus.class,
      EIGEventBus::new
    ));

    creators.add(new EIBootService<>(
      "Loading client...",
      EIGClient.class,
      () -> new EIGClient(configuration)
    ));

    creators.add(new EIBootService<>(
      "Loading background scheduler service...",
      EIGBackgroundSchedulerService.class,
      EIGBackgroundSchedulerService::new
    ));

    creators.add(new EIBootService<>(
      "Loading error dialogs...",
      EIGErrorDialogs.class,
      () -> new EIGErrorDialogs(strings, configuration)
    ));

    creators.add(new EIBootService<>(
      "Loading database...",
      EIGDatabase.class,
      () -> EIGDatabase.create(configuration)
    ));

    final var recorder =
      EITask.<EIServiceDirectoryType>create(
        LOG, "Booting application...");

    final var size = creators.size();
    for (var index = 0; index < size; ++index) {
      final var creator = creators.get(index);
      final var progress = (double) index / (double) size;

      recorder.beginStep(creator.message);
      bootEvents.accept(new EIBootEvent(creator.message(), progress));

      try {
        final var clazz = (Class<EIServiceType>) creator.clazz;
        final var service = creator.creator.create();
        services.register(clazz, service);
      } catch (final Exception e) {
        recorder.setFailed(e.getMessage(), Optional.of(e));
        throw e;
      }
    }

    if (debugFailBoot()) {
      recorder.setFailed("Failed due to debug option!");
      return recorder;
    }

    bootEvents.accept(new EIBootEvent("Boot completed.", 1.0));
    recorder.setResult(services);
    return recorder;
  }

  private static boolean debugFailBoot()
    throws IOException
  {
    final var property =
      System.getProperty("com.io7m.eigion.debug.boot_fail", "FALSE")
        .toUpperCase(Locale.ROOT);

    if ("EXCEPTION".equals(property)) {
      throw new IOException("Failed due to debug option!");
    }

    return Objects.equals(property, "TASK");
  }

  private interface EIBootServiceCreatorType<T extends EIServiceType>
  {
    T create()
      throws Exception;
  }

  private record EIBootService<T extends EIServiceType>(
    String message,
    Class<T> clazz,
    EIBootServiceCreatorType<T> creator)
  {

  }
}
