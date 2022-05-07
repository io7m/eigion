/*
 * Copyright © 2021 Mark Raynsford <code@io7m.com> http://io7m.com
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

package com.io7m.eigion.preferences;

import com.io7m.eigion.services.api.EIServiceType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * The preferences service.
 */

public interface EIPreferencesServiceType extends EIServiceType
{
  /**
   * @return The current preferences
   */

  EIPreferences preferences();

  /**
   * Save preferences to persistent storage.
   *
   * @param newPreferences The new preferences
   *
   * @throws IOException On I/O errors
   */

  void save(
    EIPreferences newPreferences)
    throws IOException;

  /**
   * Update the current preferences.
   *
   * @param updater The updater function
   *
   * @throws IOException On I/O errors
   */

  default void update(
    final Function<EIPreferences, EIPreferences> updater)
    throws IOException
  {
    Objects.requireNonNull(updater, "updater");
    this.save(updater.apply(this.preferences()));
  }

  /**
   * Add a recent file to the current preferences.
   *
   * @param file The file
   *
   * @return The file
   *
   * @throws IOException On I/O errors
   */

  default Path addRecentFile(
    final Path file)
    throws IOException
  {
    Objects.requireNonNull(file, "file");

    this.update(p -> {
      final var newFiles = new HashSet<>(p.recentFiles());
      newFiles.add(file.toAbsolutePath());

      return new EIPreferences(
        p.installationId(),
        p.debuggingEnabled(),
        List.copyOf(newFiles)
      );
    });
    return file;
  }
}
