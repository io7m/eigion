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

package com.io7m.eigion.protocol.admin_api.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

/**
 * The Admin API v1 message protocol.
 */

public final class EISA1Messages
  implements EIProtocolMessagesType<EISA1MessageType>,
  EIServiceType
{
  /**
   * The JSON schema identifier for the protocol.
   */

  public static final String SCHEMA_ID =
    "https://www.io7m.com/eigion/admin-1.json";

  /**
   * The content type for the protocol.
   */

  public static final String CONTENT_TYPE =
    "application/eigion_admin+json";

  private final SimpleDeserializers serializers;
  private final JsonMapper mapper;

  /**
   * The Admin API v1 message protocol.
   */

  public EISA1Messages()
  {
    this.serializers =
      DmJsonRestrictedDeserializers.builder()
        .allowClass(BigInteger.class)
        .allowClass(EISA1AuditEvent.class)
        .allowClass(EISA1CommandAuditGet.class)
        .allowClass(EISA1CommandLogin.class)
        .allowClass(EISA1CommandServicesList.class)
        .allowClass(EISA1CommandType.class)
        .allowClass(EISA1CommandUserCreate.class)
        .allowClass(EISA1CommandUserGet.class)
        .allowClass(EISA1CommandUserGetByEmail.class)
        .allowClass(EISA1CommandUserGetByName.class)
        .allowClass(EISA1CommandUserSearch.class)
        .allowClass(EISA1MessageType.class)
        .allowClass(EISA1Password.class)
        .allowClass(EISA1ResponseAuditGet.class)
        .allowClass(EISA1ResponseError.class)
        .allowClass(EISA1ResponseLogin.class)
        .allowClass(EISA1ResponseServiceList.class)
        .allowClass(EISA1ResponseType.class)
        .allowClass(EISA1ResponseUserCreate.class)
        .allowClass(EISA1ResponseUserGet.class)
        .allowClass(EISA1ResponseUserList.class)
        .allowClass(EISA1Service.class)
        .allowClass(EISA1Transaction.class)
        .allowClass(EISA1TransactionResponse.class)
        .allowClass(EISA1User.class)
        .allowClass(EISA1UserBan.class)
        .allowClass(EISA1UserSummary.class)
        .allowClass(String.class)
        .allowClass(UUID.class)
        .allowClass(long.class)
        .allowClassName("java.util.List<com.io7m.eigion.protocol.admin_api.v1.EISA1AuditEvent>")
        .allowClassName("java.util.List<com.io7m.eigion.protocol.admin_api.v1.EISA1CommandType>")
        .allowClassName("java.util.List<com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseType>")
        .allowClassName("java.util.List<com.io7m.eigion.protocol.admin_api.v1.EISA1Service>")
        .allowClassName("java.util.List<com.io7m.eigion.protocol.admin_api.v1.EISA1User>")
        .allowClassName("java.util.List<com.io7m.eigion.protocol.admin_api.v1.EISA1UserSummary>")
        .build();

    this.mapper =
      JsonMapper.builder()
        .enable(USE_BIG_INTEGER_FOR_INTS)
        .enable(ORDER_MAP_ENTRIES_BY_KEYS)
        .enable(SORT_PROPERTIES_ALPHABETICALLY)
        .disable(WRITE_DATES_AS_TIMESTAMPS)
        .build();

    final var simpleModule = new SimpleModule();
    simpleModule.setDeserializers(this.serializers);
    this.mapper.registerModule(simpleModule);
    this.mapper.registerModule(new JavaTimeModule());
    this.mapper.registerModule(new Jdk8Module());
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
  public EISA1MessageType parse(
    final byte[] data)
    throws EIProtocolException
  {
    try {
      return this.mapper.readValue(data, EISA1MessageType.class);
    } catch (final IOException e) {
      throw new EIProtocolException(e.getMessage(), e);
    }
  }

  @Override
  public byte[] serialize(
    final EISA1MessageType message)
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
    return "Admin API 1.0 messages.";
  }

  @Override
  public String toString()
  {
    return "[EISA1Messages 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode()));
  }
}
