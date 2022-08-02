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

package com.io7m.eigion.pike.cmdline;

import com.io7m.eigion.pike.api.EIPClientType;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The configuration for a shell.
 *
 * @param client             The client
 * @param streams            The streams to use in place of the default
 *                           controlling terminal
 * @param executedLines      A function that receives each line when executing
 *                           it has completed
 * @param locale             The locale
 * @param writeFileSeparator {@code true} if an ASCII file separator (0x1c)
 *                           should be written after the output of every
 *                           command
 */

public record EIPShellConfiguration(
  EIPClientType client,
  Optional<EIPShellStreams> streams,
  Consumer<EIPShellCommandExecuted> executedLines,
  Locale locale,
  boolean writeFileSeparator)
{
  /**
   * The configuration for a shell.
   *
   * @param client             The client
   * @param streams            The streams to use in place of the default
   *                           controlling terminal
   * @param executedLines      A function that receives each line when executing
   *                           it has completed
   * @param locale             The locale
   * @param writeFileSeparator {@code true} if an ASCII file separator (0x1c)
   *                           should be written after the output of every
   *                           command
   */

  public EIPShellConfiguration
  {
    Objects.requireNonNull(client, "client");
    Objects.requireNonNull(streams, "streams");
    Objects.requireNonNull(locale, "locale");
    Objects.requireNonNull(executedLines, "executedLines");
  }
}
