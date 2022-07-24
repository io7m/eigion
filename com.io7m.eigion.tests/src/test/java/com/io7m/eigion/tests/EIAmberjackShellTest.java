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
import com.io7m.eigion.amberjack.cmdline.EIShellCommandExecuted;
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
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
  private ArrayList<EIShellCommandExecuted> commands;

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
    final Consumer<EIShellCommandExecuted> executedLines)
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
    final EIShellCommandExecuted executed)
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

    assertEquals(10, this.commands.size());
    assertTrue(this.commands.stream().allMatch(EIShellCommandExecuted::succeeded));
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
    assertTrue(this.commands.stream().allMatch(EIShellCommandExecuted::succeeded));
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
    assertTrue(this.commands.stream().noneMatch(EIShellCommandExecuted::succeeded));
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
    assertTrue(this.commands.stream().allMatch(EIShellCommandExecuted::succeeded));
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

  /**
   * Almost all commands will fail if not logged in.
   *
   * @throws Exception On errors
   */

  @Test
  public void testNotLoggedInExhaustive()
    throws Exception
  {
    this.serverCreateAdminInitial("someone", "12345678");

    final var buffer = new StringBuilder(256);
    buffer.append("audit --dateLower 2022-07-01\n");
    buffer.append(
      "user-create --name someone --email someone@example.com --password 12345678\n");
    buffer.append("user-get --name someone\n");
    buffer.append("user-get --email someone@example.com\n");
    buffer.append("user-get --id 97fdf688-d0fd-44e9-804e-4100c30d0ce1\n");
    buffer.append("user-search --query someone\n");
    buffer.append("services\n");

    this.shell = this.createShell(buffer.toString(), this::onExec);
    this.shell.run();

    assertEquals(7, this.commands.size());
    assertTrue(this.commands.stream().noneMatch(EIShellCommandExecuted::succeeded));
  }

  /**
   * Creating and retrieving a user works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserCreateGet()
    throws Exception
  {
    this.serverCreateAdminInitial("someone", "12345678");

    final var buffer = new StringBuilder(256);
    buffer.append("set --exit-on-failed-command true\n");
    buffer.append(
      "login --username someone --password 12345678 --server %s\n"
        .formatted(this.serverAdminURI()));
    buffer.append(
      "user-create --name u --email ex@example.com --password 12345678\n"
    );
    buffer.append(
      "user-get --name u\n"
    );
    buffer.append(
      "user-get --email ex@example.com\n"
    );
    buffer.append(
      "user-search --query ex@example.com\n"
    );

    this.shell = this.createShell(buffer.toString(), this::onExec);
    this.shell.run();

    assertEquals(6, this.commands.size());
    assertTrue(this.commands.stream().allMatch(EIShellCommandExecuted::succeeded));
  }

  /**
   * Retrieving a user requires an argument.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserGetNoArguments()
    throws Exception
  {
    this.serverCreateAdminInitial("someone", "12345678");

    final var buffer = new StringBuilder(256);
    buffer.append("user-get\n");

    this.shell = this.createShell(buffer.toString(), this::onExec);
    this.shell.run();

    assertEquals(1, this.commands.size());
    assertFalse(this.commands.get(0).succeeded());
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
      assertThrows(EISExitException.class, () -> this.shell.run());
    assertEquals(1, ex.code());
  }

  /**
   * Fetching audit records works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAuditGet()
    throws Exception
  {
    this.serverCreateAdminInitial("someone", "12345678");

    final var buffer = new StringBuilder(256);
    buffer.append("set --exit-on-failed-command true\n");
    buffer.append(
      "login --username someone --password 12345678 --server %s\n"
        .formatted(this.serverAdminURI()));
    buffer.append(
      "user-create --name u --email ex@example.com --password 12345678\n"
    );
    buffer.append(
      "audit --dateLower 2022-07-01\n"
    );

    this.shell = this.createShell(buffer.toString(), this::onExec);
    this.shell.run();

    assertEquals(4, this.commands.size());
    assertTrue(this.commands.stream().allMatch(EIShellCommandExecuted::succeeded));
  }

  /**
   * Fetching services works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testServicesGet()
    throws Exception
  {
    this.serverCreateAdminInitial("someone", "12345678");

    final var buffer = new StringBuilder(256);
    buffer.append("set --exit-on-failed-command true\n");
    buffer.append(
      "login --username someone --password 12345678 --server %s\n"
        .formatted(this.serverAdminURI()));
    buffer.append(
      "services\n"
    );

    this.shell = this.createShell(buffer.toString(), this::onExec);
    this.shell.run();

    assertEquals(3, this.commands.size());
    assertTrue(this.commands.stream().allMatch(EIShellCommandExecuted::succeeded));
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
    this.serverCreateAdminInitial("someone", "12345678");

    final var buffer = new StringBuilder(256);
    buffer.append("version\n");

    this.shell = this.createShell(buffer.toString(), this::onExec);
    this.shell.run();

    assertEquals(1, this.commands.size());
    assertTrue(this.commands.stream().allMatch(EIShellCommandExecuted::succeeded));
  }
}
