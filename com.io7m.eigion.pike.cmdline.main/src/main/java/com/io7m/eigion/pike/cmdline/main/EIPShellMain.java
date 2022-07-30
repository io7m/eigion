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


package com.io7m.eigion.pike.cmdline.main;

import ch.qos.logback.classic.Level;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.io7m.eigion.pike.api.EIPClientException;
import com.io7m.eigion.pike.api.EIPClientFactoryType;
import com.io7m.eigion.pike.cmdline.EIPSExitException;
import com.io7m.eigion.pike.cmdline.EIPShellCommandExecuted;
import com.io7m.eigion.pike.cmdline.EIPShellConfiguration;
import com.io7m.eigion.pike.cmdline.EIPShellFactoryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.function.Consumer;

import static java.util.Optional.empty;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;

/**
 * The main shell program.
 */

public final class EIPShellMain
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIPShellMain.class);

  private static final Consumer<EIPShellCommandExecuted> IGNORE = s -> {
  };

  private EIPShellMain()
  {

  }

  private static final class Parameters
  {
    @Parameter(
      names = "--help",
      description = "Show command-line help.",
      required = false)
    private boolean help;

    @Parameter(
      names = "--verbose",
      description = "Set the logging verbosity level.",
      required = false)
    private EIPSVerbosity level = EIPSVerbosity.INFO;

    @Parameter(
      names = "--log-classes",
      description = "Show logging class names in logs.",
      required = false)
    private boolean logClasses;

    Parameters()
    {

    }
  }

  /**
   * The main shell program.
   *
   * @param args The command-line arguments
   *
   * @return The exit code
   *
   * @throws Exception On errors
   */

  public static int mainExitless(
    final String[] args)
    throws Exception
  {
    final var parameters =
      new Parameters();

    final var commander =
      JCommander.newBuilder()
        .programName("pike")
        .addObject(parameters)
        .build();

    try {
      commander.parse(args);
    } catch (final Exception e) {
      final var text = new StringBuilder(128);
      commander.getUsageFormatter().usage(text);
      LOG.error("{}", e.getMessage());
      LOG.info("{}", text);
      return 1;
    }

    configureLogging(parameters);

    if (parameters.help) {
      final var text = new StringBuilder(128);
      commander.getUsageFormatter().usage(text);
      System.out.println(text);
      return 1;
    }

    return run();
  }

  private static int run()
    throws IOException, EIPClientException, InterruptedException
  {
    final var clients =
      loadOrFail(EIPClientFactoryType.class);
    final var shells =
      loadOrFail(EIPShellFactoryType.class);

    final var locale = Locale.getDefault();
    try (var client = clients.create(locale)) {
      final var shellConfiguration =
        new EIPShellConfiguration(client, empty(), IGNORE, locale);
      try (var shell = shells.create(shellConfiguration)) {
        shell.run();
      } catch (final EIPSExitException e) {
        return e.code();
      }
    }
    return 0;
  }

  private static void configureLogging(
    final Parameters parameters)
  {
    EIPSLoggingPatternLayout.enableLoggerDisplay(parameters.logClasses);

    final ch.qos.logback.classic.Logger root =
      (ch.qos.logback.classic.Logger)
        LoggerFactory.getLogger(ROOT_LOGGER_NAME);

    root.setLevel(
      switch (parameters.level) {
        case INFO -> Level.INFO;
        case DEBUG -> Level.DEBUG;
        case ERROR -> Level.ERROR;
        case TRACE -> Level.TRACE;
        case WARN -> Level.WARN;
      }
    );
  }

  /**
   * The main shell program.
   *
   * @param args The command-line arguments
   *
   * @throws Exception On errors
   */

  public static void main(
    final String[] args)
    throws Exception
  {
    System.exit(mainExitless(args));
  }

  private static <T> T loadOrFail(
    final Class<T> c)
  {
    return ServiceLoader.load(c)
      .findFirst()
      .orElseThrow(() -> noServices(c));
  }

  private static ServiceConfigurationError noServices(
    final Class<?> clazz)
  {
    return new ServiceConfigurationError(noServicesMessage(clazz));
  }

  private static String noServicesMessage(
    final Class<?> clazz)
  {
    return "No services available of type '%s'"
      .formatted(clazz.getCanonicalName());
  }
}
