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

import com.io7m.eigion.amberjack.EIAClients;
import com.io7m.eigion.amberjack.api.EIAClientType;
import com.io7m.eigion.amberjack.cmdline.EISExitException;
import com.io7m.eigion.amberjack.cmdline.EIShellConfiguration;
import com.io7m.eigion.amberjack.cmdline.EIShellStreams;
import com.io7m.eigion.amberjack.cmdline.EIShellType;
import com.io7m.eigion.amberjack.cmdline.EIShells;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Amberjack client tests.
 */

public final class EIAmberjackShellTest extends EIWithServerContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIAmberjackShellTest.class);

  private EIAClients clients;
  private EIAClientType client;
  private EIShells shells;
  private ByteArrayOutputStream output;
  private EIShellType shell;
  private ArrayList<String> commands;

  @BeforeEach
  public void setup()
    throws Exception
  {
    LOG.debug("setup");

    this.output = new ByteArrayOutputStream();
    this.clients = new EIAClients();
    this.client = this.clients.create(Locale.getDefault());
    this.shells = new EIShells();
    this.commands = new ArrayList<>();
  }

  @AfterEach
  public void tearDown()
    throws Exception
  {
    LOG.debug("{}", this.output.toString(UTF_8));

    LOG.debug("tearDown");
    this.client.close();
    this.shell.close();
  }

  private EIShellType createShell(
    final String text,
    final Consumer<String> executedLines)
    throws IOException
  {
    final var input =
      new ByteArrayInputStream(text.getBytes(UTF_8));

    return this.shells.create(
      new EIShellConfiguration(
        this.client,
        Optional.of(new EIShellStreams(input, this.output)),
        executedLines,
        Locale.getDefault()
      )
    );
  }

  private void onExec(
    final String command)
  {
    LOG.debug("executed {}", command);
    this.commands.add(command);
  }

  /**
   * Help works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testHelp()
    throws Exception
  {
    final Set<String> commands;
    try (var tempShell = this.createShell("", s -> {})) {
      commands = tempShell.commandsSupported();
    }

    final var buffer = new StringBuilder(256);
    for (final var command : commands) {
      buffer.append("help ");
      buffer.append(command);
      buffer.append("\n");
    }

    this.shell = this.createShell(buffer.toString(), this::onExec);
    this.shell.run();

    assertEquals(10, this.commands.size());
  }

  /**
   * Logging in works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLogin()
    throws Exception
  {
    this.serverCreateAdminInitial("someone", "12345678");

    final var buffer = new StringBuilder(256);
    buffer.append("set --exit-on-failed-command true\n");
    buffer.append(
      "login --username someone --password 12345678 --server %s\n"
        .formatted(this.serverAdminURI()));

    this.shell = this.createShell(buffer.toString(), this::onExec);
    this.shell.run();

    assertEquals(2, this.commands.size());
  }

  /**
   * Logging in fails without arguments.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginNoArguments()
    throws Exception
  {
    this.serverCreateAdminInitial("someone", "12345678");

    final var buffer = new StringBuilder(256);
    buffer.append("set --exit-on-failed-command true\n");
    buffer.append("login\n");

    this.shell = this.createShell(buffer.toString(), this::onExec);

    final var ex = assertThrows(EISExitException.class, () -> this.shell.run());
    assertEquals(1, ex.code());
    assertEquals(2, this.commands.size());
  }
}
