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

package com.io7m.eigion.gui;

import com.io7m.jade.api.ApplicationDirectoriesType;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * The UI configuration.
 *
 * @param locale              The application locale
 * @param directories         The application directories
 * @param logoConfiguration   The logo screen configuration, if one is to be
 *                            shown
 * @param customCSS           The URI of a custom CSS file
 * @param serverConfiguration The server configuration
 * @param iconsConfiguration  The icon configuration
 */

public record EIGConfiguration(
  Locale locale,
  ApplicationDirectoriesType directories,
  Optional<EIGLogoScreenConfiguration> logoConfiguration,
  Optional<URI> customCSS,
  EIGServerConfiguration serverConfiguration,
  EIGIconsConfiguration iconsConfiguration)
{
  /**
   * The UI configuration.
   *
   * @param locale              The application locale
   * @param directories         The application directories
   * @param logoConfiguration   The logo screen configuration, if one is to be
   *                            shown
   * @param customCSS           The URI of a custom CSS file
   * @param serverConfiguration The server configuration
   * @param iconsConfiguration  The icon configuration
   */

  public EIGConfiguration
  {
    Objects.requireNonNull(directories, "directories");
    Objects.requireNonNull(locale, "locale");
    Objects.requireNonNull(logoConfiguration, "logoConfiguration");
    Objects.requireNonNull(serverConfiguration, "serverConfiguration");
    Objects.requireNonNull(iconsConfiguration, "iconsConfiguration");
  }
}
