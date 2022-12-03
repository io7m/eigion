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


package com.io7m.eigion.tests.service.configuration;

import com.io7m.eigion.server.api.EIServerConfigurations;
import com.io7m.eigion.server.service.configuration.EISConfigurationFiles;
import com.io7m.eigion.server.service.configuration.EISConfigurationService;
import com.io7m.eigion.server.service.configuration.EISConfigurationServiceType;
import com.io7m.eigion.tests.EITestDirectories;
import com.io7m.eigion.tests.service.EIServiceContract;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Locale;

public final class EISConfigurationServiceTest
  extends EIServiceContract<EISConfigurationServiceType>
{
  private Path directory;
  private Path configFile;

  @BeforeEach
  public void setup()
    throws IOException
  {
    this.directory =
      EITestDirectories.createTempDirectory();
    this.configFile =
      EITestDirectories.resourceOf(
        EISConfigurationServiceTest.class,
        this.directory,
        "configuration.xml"
      );
  }

  @AfterEach
  public void tearDown()
    throws IOException
  {
    EITestDirectories.deleteDirectory(this.directory);
  }

  @Override
  protected EISConfigurationServiceType createInstanceA()
  {
    try {
      final var file =
        new EISConfigurationFiles()
          .parse(this.configFile);

      final var config =
        EIServerConfigurations.ofFile(
          Locale.ROOT,
          Clock.systemUTC(),
          HttpClient::newHttpClient,
          file
        );

      return new EISConfigurationService(config);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  protected EISConfigurationServiceType createInstanceB()
  {
    try {
      final var file =
        new EISConfigurationFiles()
          .parse(this.configFile);

      final var config =
        EIServerConfigurations.ofFile(
          Locale.ROOT,
          Clock.systemUTC(),
          HttpClient::newHttpClient,
          file
        );

      return new EISConfigurationService(config);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
