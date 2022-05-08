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

package com.io7m.eigion.client.api;

import com.io7m.jade.api.ApplicationDirectoriesType;

import java.net.URI;
import java.util.Objects;

/**
 * Configuration information for the client.
 */

public final class EIClientConfiguration
{
  private final ApplicationDirectoriesType directories;
  private final boolean isLoginRequired;
  private final URI baseURI;

  private EIClientConfiguration(
    final ApplicationDirectoriesType inDirectories,
    final boolean inIsLoginRequired,
    final URI inBaseURI)
  {
    this.directories =
      Objects.requireNonNull(inDirectories, "directories");
    this.isLoginRequired =
      inIsLoginRequired;
    this.baseURI =
      Objects.requireNonNull(inBaseURI, "baseURI");
  }

  /**
   * Create a new client configuration builder.
   *
   * @param directories The application directories
   * @param baseURI     The server base URI
   *
   * @return A new client configuration builder.
   */

  public static Builder builder(
    final ApplicationDirectoriesType directories,
    final URI baseURI)
  {
    return new Builder(directories, baseURI);
  }

  /**
   * @return The client's application directories
   */

  public ApplicationDirectoriesType directories()
  {
    return this.directories;
  }

  /**
   * @return The server base URI
   */

  public URI baseURI()
  {
    return this.baseURI;
  }

  /**
   * @return {@code true} if the server requires a login
   */

  public boolean isLoginRequired()
  {
    return this.isLoginRequired;
  }

  /**
   * A mutable client configuration builder.
   */

  public static final class Builder
  {
    private final ApplicationDirectoriesType directories;
    private final URI baseURI;

    private Builder(
      final ApplicationDirectoriesType inDirectories,
      final URI inBaseURI)
    {
      this.directories =
        Objects.requireNonNull(inDirectories, "directories");
      this.baseURI =
        Objects.requireNonNull(inBaseURI, "baseURI");
    }

    /**
     * @return A new client configuration
     */

    public EIClientConfiguration build()
    {
      return new EIClientConfiguration(this.directories, true, this.baseURI);
    }
  }
}
