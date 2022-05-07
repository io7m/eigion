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

package com.io7m.eigion.gui.internal;

import com.io7m.eigion.gui.EIGConfiguration;
import com.io7m.eigion.gui.internal.client.EIGClient;
import com.io7m.eigion.gui.internal.errors.EIGErrorDialogs;
import com.io7m.eigion.gui.internal.news.EIGNewsParsers;
import com.io7m.eigion.preferences.EIPreferencesService;
import com.io7m.eigion.preferences.EIPreferencesServiceType;
import com.io7m.eigion.services.api.EIServiceDirectory;
import com.io7m.eigion.services.api.EIServiceDirectoryType;

import java.io.IOException;
import java.util.function.Function;

/**
 * The main service directory.
 */

public final class EIGServices
{
  private EIGServices()
  {

  }

  /**
   * Create a service directory.
   *
   * @param configuration The application configuration
   *
   * @return A new service directory
   *
   * @throws IOException On errors
   */

  public static EIServiceDirectoryType create(
    final EIGConfiguration configuration)
    throws IOException
  {
    final var services = new EIServiceDirectory();

    final var prefs =
      EIPreferencesService.openOrDefault(
        configuration.directories()
          .configurationDirectory()
          .resolve("preferences.xml")
      );
    services.register(EIPreferencesServiceType.class, prefs);
    prefs.update(Function.identity());

    final var icons = new EIGIcons(configuration.iconsConfiguration());
    services.register(EIGIcons.class, icons);
    final var newsParsers = new EIGNewsParsers();
    services.register(EIGNewsParsers.class, newsParsers);
    final var events = new EIGEventBus();
    services.register(EIGEventBus.class, events);
    final var client = new EIGClient(configuration, events);
    services.register(EIGClient.class, client);
    final var strings = new EIGStrings(configuration.locale());
    services.register(EIGStrings.class, strings);
    final var executor = new EIGBackgroundSchedulerService();
    services.register(EIGBackgroundSchedulerService.class, executor);
    final var errors = new EIGErrorDialogs(services, configuration);
    services.register(EIGErrorDialogs.class, errors);

    return services;
  }
}
