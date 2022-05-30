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


package com.io7m.eigion.server.protocol.versions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.io7m.dixmont.core.DmJsonRestrictedDeserializers;
import com.io7m.eigion.server.protocol.api.EIServerProtocolException;
import com.io7m.eigion.server.protocol.api.EIServerProtocolMessagesType;
import com.io7m.eigion.server.protocol.versions.dto.EISVProtocolSupportedJSON;
import com.io7m.eigion.server.protocol.versions.dto.EISVProtocolsJSON;
import com.io7m.eigion.services.api.EIServiceType;

import java.io.IOException;
import java.math.BigInteger;

import static com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS;
import static com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;
import static com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS;

/**
 * The Versioning protocol.
 */

public final class EISVMessages
  implements EIServerProtocolMessagesType<EISVMessageType>,
  EIServiceType
{
  /**
   * The JSON schema identifier for the protocol.
   */

  private static final String SCHEMA_ID =
    "https://www.io7m.com/eigion/versions-1.json";

  /**
   * The content type for the protocol.
   */

  private static final String CONTENT_TYPE =
    "application/eigion_versions+json";
  private final SimpleDeserializers serializers;
  private final JsonMapper mapper;

  /**
   * The Versioning protocol.
   */

  public EISVMessages()
  {
    this.serializers =
      DmJsonRestrictedDeserializers.builder()
        .allowClass(EISVProtocolSupportedJSON.class)
        .allowClass(EISVProtocolsJSON.class)
        .allowClass(String.class)
        .allowClass(BigInteger.class)
        .allowClassName(
          "java.util.List<com.io7m.eigion.server.protocol.versions.dto.EISVProtocolSupportedJSON>")
        .build();

    this.mapper =
      JsonMapper.builder()
        .enable(USE_BIG_INTEGER_FOR_INTS)
        .enable(ORDER_MAP_ENTRIES_BY_KEYS)
        .enable(SORT_PROPERTIES_ALPHABETICALLY)
        .build();

    final var simpleModule = new SimpleModule();
    simpleModule.setDeserializers(this.serializers);
    this.mapper.registerModule(simpleModule);
  }

  /**
   * @return The JSON schema identifier for the protocol.
   */

  public static String schemaId()
  {
    return SCHEMA_ID;
  }

  /**
   * @return The content type for the protocol.
   */

  public static String contentType()
  {
    return CONTENT_TYPE;
  }

  private static EISVProtocolSupported parseSupported(
    final EISVProtocolSupportedJSON p)
  {
    return new EISVProtocolSupported(
      p.id(),
      p.versionMajor(),
      p.versionMinor(),
      p.endpointPath()
    );
  }

  private static EISVProtocolSupportedJSON serializeProtocol(
    final EISVProtocolSupported supported)
  {
    return new EISVProtocolSupportedJSON(
      supported.id(),
      supported.versionMajor(),
      supported.versionMinor(),
      supported.endpointPath()
    );
  }

  @Override
  public EISVMessageType parse(
    final byte[] data)
    throws EIServerProtocolException
  {
    final EISVProtocolsJSON m;
    try {
      m = this.mapper.readValue(data, EISVProtocolsJSON.class);
    } catch (final IOException e) {
      throw new EIServerProtocolException(e.getMessage(), e);
    }

    return new EISVProtocols(
      m.protocols().stream()
        .map(EISVMessages::parseSupported)
        .toList()
    );
  }

  @Override
  public byte[] serialize(
    final EISVMessageType message)
    throws EIServerProtocolException
  {
    try {
      if (message instanceof EISVProtocols protocols) {
        return this.serializeProtocols(protocols);
      }
    } catch (final JsonProcessingException e) {
      throw new EIServerProtocolException(e.getMessage(), e);
    }

    throw new IllegalArgumentException(
      String.format("Unrecognized message: %s", message.getClass())
    );
  }

  private byte[] serializeProtocols(
    final EISVProtocols protocols)
    throws JsonProcessingException
  {
    return this.mapper.writeValueAsBytes(
      new EISVProtocolsJSON(
        protocols.protocols()
          .stream()
          .map(EISVMessages::serializeProtocol)
          .toList()
      )
    );
  }

  @Override
  public String description()
  {
    return "Versioning messages service.";
  }
}
