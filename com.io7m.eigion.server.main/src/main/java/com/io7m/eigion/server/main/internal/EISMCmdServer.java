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

package com.io7m.eigion.server.main.internal;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.io7m.claypot.core.CLPAbstractCommand;
import com.io7m.claypot.core.CLPCommandContextType;
import com.io7m.eigion.server.api.EIServerConfigurations;
import com.io7m.eigion.server.api.EIServerFactoryType;
import com.io7m.eigion.server.service.configuration.EISConfigurationFiles;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Locale;
import java.util.ServiceLoader;

import static com.io7m.claypot.core.CLPCommandType.Status.SUCCESS;

/**
 * The "server" command.
 */

@Parameters(commandDescription = "Start the server.")
public final class EISMCmdServer extends CLPAbstractCommand
{
  @Parameter(
    names = "--configuration",
    description = "The configuration file",
    required = true
  )
  private Path configurationFile;

  /**
   * Construct a command.
   *
   * @param inContext The command context
   */

  public EISMCmdServer(
    final CLPCommandContextType inContext)
  {
    super(inContext);
  }

  @Override
  protected Status executeActual()
    throws Exception
  {
    System.setProperty("org.jooq.no-tips", "true");
    System.setProperty("org.jooq.no-logo", "true");

    final var file =
      new EISConfigurationFiles()
        .parse(this.configurationFile);

    final var configuration =
      EIServerConfigurations.ofFile(
        Locale.getDefault(),
        Clock.systemUTC(),
        HttpClient::newHttpClient,
        file
      );

    final var servers =
      ServiceLoader.load(EIServerFactoryType.class)
        .findFirst()
        .orElseThrow(EISMCmdServer::noService);

    try (var server = servers.createServer(configuration)) {
      server.start();

      while (true) {
        try {
          Thread.sleep(1_000L);
        } catch (final InterruptedException e) {
          break;
        }
      }
    }

    return SUCCESS;
  }

  private static IllegalStateException noService()
  {
    return new IllegalStateException(
      "No services available of %s".formatted(EIServerFactoryType.class)
    );
  }

  @Override
  public String name()
  {
    return "server";
  }
}
