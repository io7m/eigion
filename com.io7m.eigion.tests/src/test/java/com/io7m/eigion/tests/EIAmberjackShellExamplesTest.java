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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class EIAmberjackShellExamplesTest extends EIWithServerContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIAmberjackShellExamplesTest.class);

  private EIAShellExample shell;
  private BufferedWriter writer;
  private BufferedReader reader;
  private EIPShellExample pike;
  private BufferedWriter pWriter;
  private BufferedReader pReader;

  @BeforeEach
  public void setup()
    throws Exception
  {
    this.pike = EIPShellExample.create();
    this.pike.run();

    this.shell = EIAShellExample.create();
    this.shell.run();

    this.pWriter = this.pike.shellWriter();
    this.pReader = this.pike.shellReader();

    this.writer = this.shell.shellWriter();
    this.reader = this.shell.shellReader();
  }

  @AfterEach
  public void tearDown()
    throws Exception
  {
    this.shell.close();
  }

  /**
   * The version command produces the expected output.
   *
   * @throws Exception On errors
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  public void testVersion()
    throws Exception
  {
    this.writer.write("version");
    this.writer.newLine();
    this.writer.flush();

    this.readUntilMatching("version", "^[0-9]+\\.[0-9]+\\.[0-9]+.*");
  }

  /**
   * Group invitation listings work.
   *
   * @throws Exception On errors
   */

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  public void testGroupInvitations()
    throws Exception
  {
    final var admin =
      this.serverCreateAdminInitial("someone", "12345678");
    final var user0 =
      this.serverCreateUser(admin, "some0");
    final var user1 =
      this.serverCreateUser(admin, "some1");

    this.groupCreate(user0, "com.io7m.ex");

    /*
     * Log in.
     */

    this.pWriter.write("login --username some0 --password 12345678 --server " + this.serverPublicURI());
    this.pWriter.newLine();
    this.pWriter.flush();
    this.readPikeUntilFileSeparator("login");

    /*
     * Invite the other user into this group.
     */

    this.pWriter.write("group-invite --group com.io7m.ex --user-name some1");
    this.pWriter.newLine();
    this.pWriter.flush();
    this.readPikeUntilFileSeparator("group-invite");

    /*
     * List the sent invites.
     */

    this.pWriter.write("group-invites-sent");
    this.pWriter.newLine();
    this.pWriter.flush();

    final var inviteToken =
      this.readPikeUntilMatching(
          "group-invites-sent",
          "^Invite\\.Token: [A-F0-9]+$")
        .replace("Invite.Token: ", "")
        .trim();

    this.readPikeUntilFileSeparator("group-invites-sent");

    /*
     * Now check the admin can see them.
     */

    this.writer.write("login --username someone --password 12345678 --server " + this.serverAdminURI());
    this.writer.newLine();
    this.writer.flush();
    this.readUntilFileSeparator("login");

    this.writer.write("group-invites --since 1980-01-01");
    this.writer.newLine();
    this.writer.flush();

    this.readUntilExact("group-invites", "Invite.Token: " + inviteToken);
    this.readUntilFileSeparator("group-invites");
  }

  private String readPikeUntilExact(
    final String command,
    final String exact)
    throws IOException
  {
    return readUntilExactGeneral(this.pReader, command, exact);
  }

  private String readUntilExact(
    final String command,
    final String exact)
    throws IOException
  {
    return readUntilExactGeneral(this.reader, command, exact);
  }

  private static String readUntilExactGeneral(
    final BufferedReader reader,
    final String command,
    final String exact)
    throws IOException
  {
    try {
      Thread.sleep(10L);
    } catch (final InterruptedException e) {
      // OK
    }

    while (true) {
      final var line = reader.readLine();
      LOG.debug("LINE ({}): {}", command, line);
      if (Objects.equals(line, exact)) {
        return line;
      }
      if (line.length() > 0) {
        if (line.codePointAt(0) == 0x1c) {
          Assertions.fail("Didn't find a line matching %s".formatted(exact));
        }
      }
    }
  }

  private String readPikeUntilMatching(
    final String command,
    final String exact)
    throws IOException
  {
    return readUntilMatchingGeneral(this.pReader, command, exact);
  }

  private String readUntilMatching(
    final String command,
    final String exact)
    throws IOException
  {
    return readUntilMatchingGeneral(this.reader, command, exact);
  }

  private static String readUntilMatchingGeneral(
    final BufferedReader reader,
    final String command,
    final String pattern)
    throws IOException
  {
    try {
      Thread.sleep(100L);
    } catch (final InterruptedException e) {
      // OK
    }

    while (true) {
      final var line = reader.readLine();
      LOG.debug("LINE ({}): {}", command, line);
      if (line.matches(pattern)) {
        return line;
      }
      if (line.length() > 0) {
        if (line.codePointAt(0) == 0x1c) {
          Assertions.fail("Didn't find a line matching %s".formatted(pattern));
        }
      }
    }
  }

  private List<String> readPikeUntilFileSeparator(
    final String command)
    throws IOException
  {
    return readUntilFileSeparatorGeneral(this.pReader, command);
  }

  private List<String> readUntilFileSeparator(
    final String command)
    throws IOException
  {
    return readUntilFileSeparatorGeneral(this.reader, command);
  }

  private static List<String> readUntilFileSeparatorGeneral(
    final BufferedReader reader,
    final String command)
    throws IOException
  {
    try {
      Thread.sleep(100L);
    } catch (final InterruptedException e) {
      // OK
    }

    final var lines = new ArrayList<String>();
    while (true) {
      final var line = reader.readLine();
      LOG.debug("LINE ({}): {}", command, line);
      if (line.length() > 0) {
        if (line.codePointAt(0) == 0x1c) {
          return List.copyOf(lines);
        }
      }
      lines.add(line);
    }
  }
}
