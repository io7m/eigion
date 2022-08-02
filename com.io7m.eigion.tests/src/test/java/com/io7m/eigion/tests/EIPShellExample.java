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
import com.io7m.eigion.pike.cmdline.EIPSExitException;
import com.io7m.eigion.pike.cmdline.EIPShellCommandExecuted;
import com.io7m.eigion.pike.cmdline.EIPShellConfiguration;
import com.io7m.eigion.pike.cmdline.EIPShellStreams;
import com.io7m.eigion.pike.cmdline.EIPShellType;
import com.io7m.eigion.pike.cmdline.EIPShells;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class EIPShellExample implements AutoCloseable
{
  private final EIPShellType shell;
  private final ArrayList<EIPShellCommandExecuted> commands;
  private final ExecutorService executor;
  private final PipedOutputStream shellWritesTo;
  private final PipedOutputStream writeCommandsToShell;
  private final PipedInputStream shellReadsFrom;
  private final PipedInputStream readOutputFromShell;
  private final BufferedWriter shellWriter;
  private final BufferedReader shellReader;

  public EIPShellExample(
    final EIPShellType inShell,
    final ArrayList<EIPShellCommandExecuted> inCommands,
    final ExecutorService inExecutor,
    final PipedOutputStream inShellWritesTo,
    final PipedOutputStream inWriteCommandsToShell,
    final PipedInputStream inShellReadsFrom,
    final PipedInputStream inReadOutputFromShell)
  {
    this.shell =
      Objects.requireNonNull(inShell, "shell");
    this.commands =
      Objects.requireNonNull(inCommands, "commands");
    this.executor =
      Objects.requireNonNull(inExecutor, "executor");
    this.shellWritesTo =
      Objects.requireNonNull(inShellWritesTo, "shellWritesTo");
    this.writeCommandsToShell =
      Objects.requireNonNull(inWriteCommandsToShell, "writeCommandsToShell");
    this.shellReadsFrom =
      Objects.requireNonNull(inShellReadsFrom, "shellReadsFrom");
    this.readOutputFromShell =
      Objects.requireNonNull(inReadOutputFromShell, "readOutputFromShell");

    this.shellWriter =
      new BufferedWriter(
        new OutputStreamWriter(this.writeCommandsToShell, UTF_8));
    this.shellReader =
      new BufferedReader(
        new InputStreamReader(this.readOutputFromShell, UTF_8));
  }

  public static EIPShellExample create()
    throws Exception
  {
    final var shellWritesTo =
      new PipedOutputStream();
    final var writeCommandsToShell =
      new PipedOutputStream();
    final var shellReadsFrom =
      new PipedInputStream();
    final var readOutputFromShell =
      new PipedInputStream();

    readOutputFromShell.connect(shellWritesTo);
    writeCommandsToShell.connect(shellReadsFrom);

    final var commands =
      new ArrayList<EIPShellCommandExecuted>();
    final var clients =
      new EIPClients();
    final var local =
      Locale.getDefault();
    final var client =
      clients.create(local);
    final var shells =
      new EIPShells();
    final var shell =
      shells.create(new EIPShellConfiguration(
        client,
        Optional.of(
          new EIPShellStreams(
            shellReadsFrom,
            shellWritesTo
          )
        ),
        commands::add,
        local,
        true
      ));

    final var executor =
      Executors.newSingleThreadExecutor(r -> {
        final var th = new Thread(r);
        th.setName("com.io7m.eigion.shell.example[%d]".formatted(th.getId()));
        return th;
      });

    return new EIPShellExample(
      shell,
      commands,
      executor,
      shellWritesTo,
      writeCommandsToShell,
      shellReadsFrom,
      readOutputFromShell
    );
  }

  public BufferedWriter shellWriter()
  {
    return this.shellWriter;
  }

  public BufferedReader shellReader()
  {
    return this.shellReader;
  }

  public void run()
  {
    this.executor.execute(() -> {
      try {
        this.shell.run();
      } catch (final EIPSExitException e) {
        // OK
      }
    });
  }

  @Override
  public void close()
    throws Exception
  {
    this.shell.close();
    this.executor.shutdown();
    this.executor.awaitTermination(5L, TimeUnit.SECONDS);
  }
}
