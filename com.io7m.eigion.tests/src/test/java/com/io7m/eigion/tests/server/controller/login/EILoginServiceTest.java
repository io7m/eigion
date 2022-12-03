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


package com.io7m.eigion.tests.server.controller.login;

import com.io7m.eigion.server.api.EIServerIdstoreConfiguration;
import com.io7m.eigion.server.controller.EISStrings;
import com.io7m.eigion.server.controller.login.EILoginService;
import com.io7m.eigion.server.controller.login.EILoginServiceType;
import com.io7m.eigion.server.service.clock.EISClock;
import com.io7m.eigion.server.service.idstore.EISIdstoreClients;
import com.io7m.eigion.server.service.sessions.EISessionService;
import com.io7m.eigion.tests.service.EIServiceContract;
import io.opentelemetry.api.OpenTelemetry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;

public final class EILoginServiceTest
  extends EIServiceContract<EILoginServiceType>
{
  @Override
  protected EILoginServiceType createInstanceA()
  {
    try {
      final var clients =
        EISIdstoreClients.create(
          Locale.ROOT,
          new EIServerIdstoreConfiguration(
            URI.create("urn:ex"),
            URI.create("urn:ex"))
        );

      return new EILoginService(
        new EISClock(Clock.systemUTC()),
        new EISStrings(Locale.ROOT),
        new EISessionService(clients, OpenTelemetry.noop(), Duration.ofDays(1L)),
        clients
      );
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  protected EILoginServiceType createInstanceB()
  {
    try {
      final var clients =
        EISIdstoreClients.create(
          Locale.ROOT,
          new EIServerIdstoreConfiguration(
            URI.create("urn:ex"),
            URI.create("urn:ex"))
        );

      return new EILoginService(
        new EISClock(Clock.systemUTC()),
        new EISStrings(Locale.ROOT),
        new EISessionService(clients, OpenTelemetry.noop(), Duration.ofDays(1L)),
        clients
      );
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
