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

package com.io7m.eigion.protocol.versions.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.io7m.eigion.protocol.versions.EISVMessages;

import java.util.List;
import java.util.Objects;

// CHECKSTYLE:OFF

@JsonDeserialize
@JsonSerialize
public final class EISVProtocolsJSON
{
  @JsonProperty(value = "%Schema", required = false)
  private final String schema = EISVMessages.schemaId();
  @JsonProperty(value = "Protocols", required = true)
  private final List<EISVProtocolSupportedJSON> protocols;

  @JsonCreator
  public EISVProtocolsJSON(
    @JsonProperty(value = "Protocols", required = true) final List<EISVProtocolSupportedJSON> inSupported)
  {
    this.protocols =
      Objects.requireNonNull(inSupported, "supported");
  }

  public String schema()
  {
    return this.schema;
  }

  public List<EISVProtocolSupportedJSON> protocols()
  {
    return this.protocols;
  }
}
