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


package com.io7m.eigion.protocol.public_api.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.io7m.dixmont.core.DmJsonRestrictedDeserializers;
import com.io7m.eigion.protocol.api.EIProtocolException;
import com.io7m.eigion.protocol.api.EIProtocolMessagesType;
import com.io7m.eigion.services.api.EIServiceType;

import java.io.IOException;
import java.math.BigInteger;
import java.util.UUID;

import static com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS;
import static com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;
import static com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS;

/**
 * The Public API v1 message protocol.
 */

public final class EISP1Messages
  implements EIProtocolMessagesType<EISP1MessageType>,
  EIServiceType
{
  /**
   * The JSON schema identifier for the protocol.
   */

  public static final String SCHEMA_ID =
    "https://www.io7m.com/eigion/public-1.json";

  /**
   * The content type for the protocol.
   */

  public static final String CONTENT_TYPE =
    "application/eigion_public+json";

  private final SimpleDeserializers serializers;
  private final JsonMapper mapper;

  private static String listOf(
    final Class<?> clazz)
  {
    return "java.util.List<%s>".formatted(clazz.getCanonicalName());
  }

  private static String setOf(
    final Class<?> clazz)
  {
    return "java.util.Set<%s>".formatted(clazz.getCanonicalName());
  }

  private static String mapOf(
    final Class<?> keyClazz,
    final String valClazz)
  {
    return "java.util.Map<%s,%s>"
      .formatted(keyClazz.getCanonicalName(), valClazz);
  }

  /**
   * The Public API v1 message protocol.
   */

  public EISP1Messages()
  {
    this.serializers =
      DmJsonRestrictedDeserializers.builder()
        .allowClass(BigInteger.class)
        .allowClass(EISP1CommandLogin.class)
        .allowClass(EISP1Hash.class)
        .allowClass(EISP1ProductSummary.class)
        .allowClass(EISP1ResponseError.class)
        .allowClass(EISP1ResponseImageCreated.class)
        .allowClass(EISP1ResponseImageGet.class)
        .allowClass(EISP1ResponseProductList.class)
        .allowClass(EISP1MessageType.class)
        .allowClass(String.class)
        .allowClass(UUID.class)
        .allowClassName(listOf(EISP1ProductSummary.class))
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


  @Override
  public EISP1MessageType parse(
    final byte[] data)
    throws EIProtocolException
  {
    try {
      return this.mapper.readValue(data, EISP1MessageType.class);
    } catch (final IOException e) {
      throw new EIProtocolException(e.getMessage(), e);
    }
  }

  @Override
  public byte[] serialize(
    final EISP1MessageType message)
    throws EIProtocolException
  {
    try {
      return this.mapper.writeValueAsBytes(message);
    } catch (final JsonProcessingException e) {
      throw new EIProtocolException(e.getMessage(), e);
    }
  }

  @Override
  public String description()
  {
    return "Public API 1.0 messages.";
  }

  @Override
  public String toString()
  {
    return "[EISP1Messages 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode()));
  }
}
