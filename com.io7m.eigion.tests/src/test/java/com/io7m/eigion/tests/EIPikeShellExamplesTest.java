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

import com.io7m.eigion.model.EIGroupCreationRequest;
import com.io7m.eigion.model.EIGroupCreationRequestStatusType.Succeeded;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIToken;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;

public final class EIPikeShellExamplesTest extends EIWithServerContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIPikeShellExamplesTest.class);

  private EIPShellExample shell;
  private BufferedWriter writer;
  private BufferedReader reader;

  @BeforeEach
  public void setup()
    throws Exception
  {
    this.shell = EIPShellExample.create();
    this.shell.run();
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
   * Group invitation cancellations work.
   *
   * @throws Exception On errors
   */

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  public void testGroupInvitationsCancellation()
    throws Exception
  {
    final var admin =
      this.serverCreateAdminInitial("someone", "12345678");
    final var user0 =
      this.serverCreateUser(admin, "some0");
    final var user1 =
      this.serverCreateUser(admin, "some1");

    /*
     * Log in.
     */

    this.writer.write("login --username some0 --password 12345678 --server " + this.serverPublicURI());
    this.writer.newLine();
    this.writer.flush();
    this.readUntilFileSeparator("login");

    /*
     * Request the creation of a group.
     */

    this.writer.write("group-creation-begin --name com.io7m.ex");
    this.writer.newLine();
    this.writer.flush();

    final var token =
      new EIToken(this.readUntilMatching(
        "group-creation-begin",
        "^  [A-F0-9]+$").trim());
    this.readUntilFileSeparator("group-creation-begin");

    this.domainCheckers()
      .enqueue(CompletableFuture.completedFuture(
        new EIGroupCreationRequest(
          new EIGroupName("com.io7m.ex"),
          user0,
          token,
          new Succeeded(timeNow(), timeNow())
        )
      ));

    /*
     * List the available group requests.
     */

    this.writer.write("group-creation-requests");
    this.writer.newLine();
    this.writer.flush();
    this.readUntilExact(
      "group-creation-requests",
      "Request.Token: %s".formatted(token.value())
    );
    this.readUntilFileSeparator("group-creation-requests");

    /*
     * Tell the server the group is ready to check.
     */

    this.writer.write("group-creation-ready --token %s".formatted(token));
    this.writer.newLine();
    this.writer.flush();
    this.readUntilFileSeparator("group-creation-ready");

    /*
     * Invite the other user into this group.
     */

    this.writer.write("group-invite --group com.io7m.ex --user-name some1");
    this.writer.newLine();
    this.writer.flush();
    this.readUntilFileSeparator("group-invite");

    /*
     * List the sent invites.
     */

    this.writer.write("group-invites-sent");
    this.writer.newLine();
    this.writer.flush();

    final var inviteToken =
      this.readUntilMatching(
          "group-invites-sent",
          "^Invite\\.Token: [A-F0-9]+$")
        .replace("Invite.Token: ", "")
        .trim();

    this.readUntilFileSeparator("group-invites-sent");

    /*
     * Log in as the other user.
     */

    this.writer.write("login --username some1 --password 12345678 --server " + this.serverPublicURI());
    this.writer.newLine();
    this.writer.flush();
    this.readUntilFileSeparator("login");

    /*
     * List invites.
     */

    this.writer.write("group-invites-received");
    this.writer.newLine();
    this.writer.flush();

    this.readUntilExact(
      "group-invites-received",
      "Invite.Token: %s".formatted(inviteToken)
    );

    this.readUntilFileSeparator("group-invites-received");

    /*
     * Log in as the other user again.
     */

    this.writer.write("login --username some0 --password 12345678 --server " + this.serverPublicURI());
    this.writer.newLine();
    this.writer.flush();
    this.readUntilFileSeparator("login");

    /*
     * Cancel the invite.
     */

    this.writer.write("group-invite-cancel --token %s".formatted(inviteToken));
    this.writer.newLine();
    this.writer.flush();
    this.readUntilFileSeparator("group-invite-cancel");

    /*
     * Log in as the other user again.
     */

    this.writer.write("login --username some1 --password 12345678 --server " + this.serverPublicURI());
    this.writer.newLine();
    this.writer.flush();
    this.readUntilFileSeparator("login");

    /*
     * The invite is now cancelled.
     */

    this.writer.write("group-invites-received");
    this.writer.newLine();
    this.writer.flush();

    this.readUntilExact("group-invites-received", "Invite.Status: CANCELLED");
    this.readUntilFileSeparator("group-invites-received");
  }

  /**
   * Group invitations work.
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

    /*
     * Log in.
     */

    this.writer.write("login --username some0 --password 12345678 --server " + this.serverPublicURI());
    this.writer.newLine();
    this.writer.flush();
    this.readUntilFileSeparator("login");

    /*
     * Request the creation of a group.
     */

    this.writer.write("group-creation-begin --name com.io7m.ex");
    this.writer.newLine();
    this.writer.flush();

    final var token =
      new EIToken(this.readUntilMatching(
        "group-creation-begin",
        "^  [A-F0-9]+$").trim());
    this.readUntilFileSeparator("group-creation-begin");

    this.domainCheckers()
      .enqueue(CompletableFuture.completedFuture(
        new EIGroupCreationRequest(
          new EIGroupName("com.io7m.ex"),
          user0,
          token,
          new Succeeded(timeNow(), timeNow())
        )
      ));

    /*
     * List the available group requests.
     */

    this.writer.write("group-creation-requests");
    this.writer.newLine();
    this.writer.flush();
    this.readUntilExact(
      "group-creation-requests",
      "Request.Token: %s".formatted(token.value())
    );
    this.readUntilFileSeparator("group-creation-requests");

    /*
     * Tell the server the group is ready to check.
     */

    this.writer.write("group-creation-ready --token %s".formatted(token));
    this.writer.newLine();
    this.writer.flush();
    this.readUntilFileSeparator("group-creation-ready");

    /*
     * Invite the other user into this group.
     */

    this.writer.write("group-invite --group com.io7m.ex --user-name some1");
    this.writer.newLine();
    this.writer.flush();
    this.readUntilFileSeparator("group-invite");

    /*
     * List the sent invites.
     */

    this.writer.write("group-invites-sent");
    this.writer.newLine();
    this.writer.flush();

    final var inviteToken =
      this.readUntilMatching(
          "group-invites-sent",
          "^Invite\\.Token: [A-F0-9]+$")
        .replace("Invite.Token: ", "")
        .trim();

    this.readUntilFileSeparator("group-invites-sent");

    /*
     * Log in as the other user.
     */

    this.writer.write("login --username some1 --password 12345678 --server " + this.serverPublicURI());
    this.writer.newLine();
    this.writer.flush();
    this.readUntilFileSeparator("login");

    /*
     * List invites.
     */

    this.writer.write("group-invites-received");
    this.writer.newLine();
    this.writer.flush();

    this.readUntilExact(
      "group-invites-received",
      "Invite.Token: %s".formatted(inviteToken)
    );

    this.readUntilFileSeparator("group-invites-received");

    /*
     * Accept the invite.
     */

    this.writer.write("group-invite-respond --token %s --accept true".formatted(inviteToken));
    this.writer.newLine();
    this.writer.flush();
    this.readUntilFileSeparator("group-invite-respond");

    /*
     * The invite is accepted.
     */

    this.writer.write("group-invites-received");
    this.writer.newLine();
    this.writer.flush();

    this.readUntilExact("group-invites-received", "Invite.Status: ACCEPTED");
    this.readUntilFileSeparator("group-invites-received");

    /*
     * The user is now in the group.
     */

    this.writer.write("groups");
    this.writer.newLine();
    this.writer.flush();

    this.readUntilExact("groups", "com.io7m.ex: []");
    this.readUntilFileSeparator("groups");
  }

  /**
   * Group invitation rejections work.
   *
   * @throws Exception On errors
   */

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  public void testGroupInvitationRejection()
    throws Exception
  {
    final var admin =
      this.serverCreateAdminInitial("someone", "12345678");
    final var user0 =
      this.serverCreateUser(admin, "some0");
    final var user1 =
      this.serverCreateUser(admin, "some1");

    /*
     * Log in.
     */

    this.writer.write("login --username some0 --password 12345678 --server " + this.serverPublicURI());
    this.writer.newLine();
    this.writer.flush();
    this.readUntilFileSeparator("login");

    /*
     * Request the creation of a group.
     */

    this.writer.write("group-creation-begin --name com.io7m.ex");
    this.writer.newLine();
    this.writer.flush();

    final var token =
      new EIToken(this.readUntilMatching(
        "group-creation-begin",
        "^  [A-F0-9]+$").trim());
    this.readUntilFileSeparator("group-creation-begin");

    this.domainCheckers()
      .enqueue(CompletableFuture.completedFuture(
        new EIGroupCreationRequest(
          new EIGroupName("com.io7m.ex"),
          user0,
          token,
          new Succeeded(timeNow(), timeNow())
        )
      ));

    /*
     * List the available group requests.
     */

    this.writer.write("group-creation-requests");
    this.writer.newLine();
    this.writer.flush();
    this.readUntilExact(
      "group-creation-requests",
      "Request.Token: %s".formatted(token.value())
    );
    this.readUntilFileSeparator("group-creation-requests");

    /*
     * Tell the server the group is ready to check.
     */

    this.writer.write("group-creation-ready --token %s".formatted(token));
    this.writer.newLine();
    this.writer.flush();
    this.readUntilFileSeparator("group-creation-ready");

    /*
     * Invite the other user into this group.
     */

    this.writer.write("group-invite --group com.io7m.ex --user-name some1");
    this.writer.newLine();
    this.writer.flush();
    this.readUntilFileSeparator("group-invite");

    /*
     * List the sent invites.
     */

    this.writer.write("group-invites-sent");
    this.writer.newLine();
    this.writer.flush();

    final var inviteToken =
      this.readUntilMatching(
          "group-invites-sent",
          "^Invite\\.Token: [A-F0-9]+$")
        .replace("Invite.Token: ", "")
        .trim();

    this.readUntilFileSeparator("group-invites-sent");

    /*
     * Log in as the other user.
     */

    this.writer.write("login --username some1 --password 12345678 --server " + this.serverPublicURI());
    this.writer.newLine();
    this.writer.flush();
    this.readUntilFileSeparator("login");

    /*
     * List invites.
     */

    this.writer.write("group-invites-received");
    this.writer.newLine();
    this.writer.flush();

    this.readUntilExact(
      "group-invites-received",
      "Invite.Token: %s".formatted(inviteToken)
    );

    this.readUntilFileSeparator("group-invites-received");

    /*
     * Accept the invite.
     */

    this.writer.write("group-invite-respond --token %s --accept false".formatted(inviteToken));
    this.writer.newLine();
    this.writer.flush();
    this.readUntilFileSeparator("group-invite-respond");

    /*
     * The invite is accepted.
     */

    this.writer.write("group-invites-received");
    this.writer.newLine();
    this.writer.flush();

    this.readUntilExact("group-invites-received", "Invite.Status: REJECTED");
    this.readUntilFileSeparator("group-invites-received");

    /*
     * The user is not in the group.
     */

    this.writer.write("groups");
    this.writer.newLine();
    this.writer.flush();

    final var lines = this.readUntilFileSeparator("groups");
    assertFalse(lines.stream().anyMatch(s -> s.contains("com.io7m.ex")));
  }

  private String readUntilExact(
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
      final var line = this.reader.readLine();
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

  private String readUntilMatching(
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
      final var line = this.reader.readLine();
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

  private List<String> readUntilFileSeparator(
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
      final var line = this.reader.readLine();
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
