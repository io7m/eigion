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

package com.io7m.eigion.server.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Objects;
import java.util.Optional;

/**
 * The server configuration file.
 *
 * @param httpConfiguration     The HTTP configuration
 * @param databaseConfiguration The database configuration
 * @param openTelemetry         The OpenTelemetry configuration
 */

@JsonDeserialize
@JsonSerialize
public record EIServerConfigurationFile(
  @JsonProperty(value = "HTTP", required = true)
  EIServerHTTPConfiguration httpConfiguration,
  @JsonProperty(value = "Database", required = true)
  EIServerDatabaseConfiguration databaseConfiguration,
  @JsonProperty(value = "OpenTelemetry")
  Optional<EIServerOpenTelemetryConfiguration> openTelemetry)
  implements EIServerJSONConfigurationElementType
{
  /**
   * The server configuration file.
   *
   * @param httpConfiguration     The HTTP configuration
   * @param databaseConfiguration The database configuration
   * @param openTelemetry         The OpenTelemetry configuration
   */

  @JsonCreator
  public EIServerConfigurationFile
  {
    Objects.requireNonNull(httpConfiguration, "httpConfiguration");
    Objects.requireNonNull(databaseConfiguration, "databaseConfiguration");
    Objects.requireNonNull(openTelemetry, "openTelemetry");
  }
}
