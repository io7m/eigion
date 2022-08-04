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
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.io7m.dixmont.core.DmJsonRestrictedDeserializers;
import com.io7m.eigion.protocol.api.EIProtocolException;
import com.io7m.eigion.protocol.api.EIProtocolMessagesType;
import com.io7m.eigion.services.api.EIServiceType;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.util.UUID;

import static com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS;
import static com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;
import static com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

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

  /**
   * The Public API v1 message protocol.
   */

  public EISP1Messages()
  {
    this.serializers =
      DmJsonRestrictedDeserializers.builder()
        .allowClass(BigInteger.class)
        .allowClass(EISP1CommandGroupCreateBegin.class)
        .allowClass(EISP1CommandGroupCreateCancel.class)
        .allowClass(EISP1CommandGroupCreateReady.class)
        .allowClass(EISP1CommandGroupCreateRequests.class)
        .allowClass(EISP1CommandGroupGrant.class)
        .allowClass(EISP1CommandGroupInvite.class)
        .allowClass(EISP1CommandGroupInviteByName.class)
        .allowClass(EISP1CommandGroupInviteCancel.class)
        .allowClass(EISP1CommandGroupInviteRespond.class)
        .allowClass(EISP1CommandGroupInvitesReceived.class)
        .allowClass(EISP1CommandGroupInvitesSent.class)
        .allowClass(EISP1CommandGroupLeave.class)
        .allowClass(EISP1CommandGroups.class)
        .allowClass(EISP1CommandLogin.class)
        .allowClass(EISP1CommandUserSelf.class)
        .allowClass(EISP1GroupCreationRequest.class)
        .allowClass(EISP1GroupInvite.class)
        .allowClass(EISP1GroupInviteStatus.class)
        .allowClass(EISP1GroupRole.class)
        .allowClass(EISP1GroupRoles.class)
        .allowClass(EISP1Hash.class)
        .allowClass(EISP1MessageType.class)
        .allowClass(EISP1Password.class)
        .allowClass(EISP1ProductSummary.class)
        .allowClass(EISP1ResponseError.class)
        .allowClass(EISP1ResponseGroupCreateBegin.class)
        .allowClass(EISP1ResponseGroupCreateCancel.class)
        .allowClass(EISP1ResponseGroupCreateReady.class)
        .allowClass(EISP1ResponseGroupCreateRequests.class)
        .allowClass(EISP1ResponseGroupGrant.class)
        .allowClass(EISP1ResponseGroupInvite.class)
        .allowClass(EISP1ResponseGroupInviteCancel.class)
        .allowClass(EISP1ResponseGroupInviteRespond.class)
        .allowClass(EISP1ResponseGroupInvites.class)
        .allowClass(EISP1ResponseGroupLeave.class)
        .allowClass(EISP1ResponseGroupLeave.class)
        .allowClass(EISP1ResponseGroups.class)
        .allowClass(EISP1ResponseImageCreated.class)
        .allowClass(EISP1ResponseImageGet.class)
        .allowClass(EISP1ResponseLogin.class)
        .allowClass(EISP1ResponseProductList.class)
        .allowClass(EISP1ResponseUserSelf.class)
        .allowClass(EISP1User.class)
        .allowClass(EISP1UserBan.class)
        .allowClass(EISP1UserSummary.class)
        .allowClass(String.class)
        .allowClass(URI.class)
        .allowClass(UUID.class)
        .allowClass(boolean.class)
        .allowClassName(listOf(EISP1GroupCreationRequest.class))
        .allowClassName(listOf(EISP1GroupInvite.class))
        .allowClassName(listOf(EISP1GroupRoles.class))
        .allowClassName(listOf(EISP1ProductSummary.class))
        .allowClassName(mapOf(String.class, setOf(EISP1GroupRole.class)))
        .allowClassName(setOf(EISP1GroupRole.class))
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
