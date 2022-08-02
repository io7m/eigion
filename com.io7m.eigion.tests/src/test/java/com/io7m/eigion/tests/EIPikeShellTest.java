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

import com.io7m.eigion.pike.EIPClients;
import com.io7m.eigion.pike.api.EIPClientType;
import com.io7m.eigion.pike.cmdline.EIPSExitException;
import com.io7m.eigion.pike.cmdline.EIPShellCommandExecuted;
import com.io7m.eigion.pike.cmdline.EIPShellConfiguration;
import com.io7m.eigion.pike.cmdline.EIPShellStreams;
import com.io7m.eigion.pike.cmdline.EIPShellType;
import com.io7m.eigion.pike.cmdline.EIPShells;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
 * Pike shell tests.
 */

public final class EIPikeShellTest extends EIWithServerContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIPikeShellTest.class);

  private EIPClients clients;
  private EIPClientType client;
  private EIPShells shells;
  private ByteArrayOutputStream output;
  private EIPShellType shell;
  private ArrayList<EIPShellCommandExecuted> commands;

  @BeforeEach
  public void setup()
    throws Exception
  {
    LOG.debug("setup");

    this.output = new ByteArrayOutputStream();
    this.clients = new EIPClients();
    this.client = this.clients.create(Locale.getDefault());
    this.shells = new EIPShells();
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

  private EIPShellType createShell(
    final String text,
    final Consumer<EIPShellCommandExecuted> executedLines)
    throws IOException
  {
    final var input =
      new ByteArrayInputStream(text.getBytes(UTF_8));

    return this.shells.create(
      new EIPShellConfiguration(
        this.client,
        Optional.of(new EIPShellStreams(input, this.output)),
        executedLines,
        Locale.getDefault(),
        false
      )
    );
  }

  private void onExec(
    final EIPShellCommandExecuted executed)
  {
    LOG.debug("executed {}", executed);
    this.commands.add(executed);
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
    try (var tempShell = this.createShell("", s -> {
    })) {
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

    assertEquals(14, this.commands.size());
    assertTrue(this.commands.stream().allMatch(EIPShellCommandExecuted::succeeded));
  }

  /**
   * Help works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testHelpSelf()
    throws Exception
  {
    final var buffer = new StringBuilder(256);
    buffer.append("help\n");

    this.shell = this.createShell(buffer.toString(), this::onExec);
    this.shell.run();

    assertEquals(1, this.commands.size());
    assertTrue(this.commands.stream().allMatch(EIPShellCommandExecuted::succeeded));
  }

  /**
   * Help rejects nonexistent commands.
   *
   * @throws Exception On errors
   */

  @Test
  public void testHelpNonexistent()
    throws Exception
  {
    final var buffer = new StringBuilder(256);
    buffer.append("help nonexistent\n");

    this.shell = this.createShell(buffer.toString(), this::onExec);
    this.shell.run();

    assertEquals(1, this.commands.size());
    assertTrue(this.commands.stream().noneMatch(EIPShellCommandExecuted::succeeded));
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
    this.serverCreateUser(
      this.serverCreateAdminInitial("someone", "12345678"),
      "someone");

    final var buffer = new StringBuilder(256);
    buffer.append("set --exit-on-failed-command true\n");
    buffer.append(
      "login --username someone --password 12345678 --server %s\n"
        .formatted(this.serverPublicURI()));

    this.shell = this.createShell(buffer.toString(), this::onExec);
    this.shell.run();

    assertEquals(2, this.commands.size());
    assertTrue(this.commands.stream().allMatch(EIPShellCommandExecuted::succeeded));
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
    this.serverCreateUser(
      this.serverCreateAdminInitial("someone", "12345678"),
      "someone");

    final var buffer = new StringBuilder(256);
    buffer.append("set --exit-on-failed-command true\n");
    buffer.append("login\n");

    this.shell = this.createShell(buffer.toString(), this::onExec);

    final var ex = assertThrows(
      EIPSExitException.class,
      () -> this.shell.run());
    assertEquals(1, ex.code());
    assertEquals(2, this.commands.size());
  }

  /**
   * Exiting works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testExit()
    throws Exception
  {
    final var buffer = new StringBuilder(256);
    buffer.append("exit --code 1\n");

    this.shell = this.createShell(buffer.toString(), this::onExec);

    final var ex =
      assertThrows(EIPSExitException.class, () -> this.shell.run());
    assertEquals(1, ex.code());
  }

  /**
   * Listing groups works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroups()
    throws Exception
  {
    this.serverCreateUser(
      this.serverCreateAdminInitial("someone", "12345678"),
      "someone");

    final var buffer = new StringBuilder(256);
    buffer.append("set --exit-on-failed-command true\n");
    buffer.append(
      "login --username someone --password 12345678 --server %s\n"
        .formatted(this.serverPublicURI()));
    buffer.append("groups\n");

    this.shell = this.createShell(buffer.toString(), this::onExec);
    this.shell.run();

    assertEquals(3, this.commands.size());
    assertTrue(this.commands.stream().allMatch(EIPShellCommandExecuted::succeeded));
  }

  /**
   * Showing the application version works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testVersion()
    throws Exception
  {
    this.serverCreateUser(
      this.serverCreateAdminInitial("someone", "12345678"),
      "someone");

    final var buffer = new StringBuilder(256);
    buffer.append("version\n");

    this.shell = this.createShell(buffer.toString(), this::onExec);
    this.shell.run();

    assertEquals(1, this.commands.size());
    assertTrue(this.commands.stream().allMatch(EIPShellCommandExecuted::succeeded));
  }
}
