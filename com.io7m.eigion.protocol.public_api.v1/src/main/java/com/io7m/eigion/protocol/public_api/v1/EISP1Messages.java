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
import com.io7m.eigion.hash.EIHash;
import com.io7m.eigion.model.EIProductIdentifier;
import com.io7m.eigion.model.EIProductSummary;
import com.io7m.eigion.protocol.api.EIProtocolException;
import com.io7m.eigion.protocol.api.EIProtocolMessagesType;
import com.io7m.eigion.protocol.public_api.v1.dto.EISP1CommandLoginJSON;
import com.io7m.eigion.protocol.public_api.v1.dto.EISP1HashJSON;
import com.io7m.eigion.protocol.public_api.v1.dto.EISP1JsonType;
import com.io7m.eigion.protocol.public_api.v1.dto.EISP1ProductSummaryJSON;
import com.io7m.eigion.protocol.public_api.v1.dto.EISP1ResponseErrorJSON;
import com.io7m.eigion.protocol.public_api.v1.dto.EISP1ResponseImageCreatedJSON;
import com.io7m.eigion.protocol.public_api.v1.dto.EISP1ResponseImageGetJSON;
import com.io7m.eigion.protocol.public_api.v1.dto.EISP1ResponseProductListJSON;
import com.io7m.eigion.services.api.EIServiceType;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Optional;
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

  /**
   * The Public API v1 message protocol.
   */

  public EISP1Messages()
  {
    this.serializers =
      DmJsonRestrictedDeserializers.builder()
        .allowClass(BigInteger.class)
        .allowClass(EISP1CommandLoginJSON.class)
        .allowClass(EISP1HashJSON.class)
        .allowClass(EISP1JsonType.class)
        .allowClass(EISP1ProductSummaryJSON.class)
        .allowClass(EISP1ResponseErrorJSON.class)
        .allowClass(EISP1ResponseImageCreatedJSON.class)
        .allowClass(EISP1ResponseImageGetJSON.class)
        .allowClass(EISP1ResponseProductListJSON.class)
        .allowClass(String.class)
        .allowClass(UUID.class)
        .allowClassName(
          "java.util.List<com.io7m.eigion.protocol.public_api.v1.dto.EISP1ProductSummaryJSON>")
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

  private static EISP1MessageType mapImageCreated(
    final EISP1ResponseImageCreatedJSON imageCreated)
  {
    return new EISP1ResponseImageCreated(
      imageCreated.requestId(),
      imageCreated.imageID()
    );
  }

  private static EISP1MessageType mapImageGet(
    final EISP1ResponseImageGetJSON imageGet)
  {
    return new EISP1ResponseImageGet(
      imageGet.requestId(),
      imageGet.id(),
      mapHash(imageGet.hash())
    );
  }

  private static EIHash mapHash(
    final EISP1HashJSON hash)
  {
    return new EIHash(hash.algorithm(), hash.value());
  }

  private static EISP1MessageType mapError(
    final EISP1ResponseErrorJSON error)
  {
    return new EISP1ResponseError(
      error.requestId(),
      error.errorCode(),
      error.message()
    );
  }

  private static EISP1MessageType mapLogin(
    final EISP1CommandLoginJSON login)
  {
    return new EISP1CommandLogin(
      login.userName(),
      login.password()
    );
  }

  private static EISP1MessageType mapProductList(
    final EISP1ResponseProductListJSON productList)
  {
    return new EISP1ResponseProductList(
      productList.requestId(),
      productList.products()
        .stream()
        .map(EISP1Messages::mapProductSummary)
        .toList()
    );
  }

  private static EIProductSummary mapProductSummary(
    final EISP1ProductSummaryJSON summary)
  {
    return new EIProductSummary(
      new EIProductIdentifier(
        summary.group(),
        summary.name()
      ),
      summary.title(),
      Optional.empty()
    );
  }

  private static EISP1ProductSummaryJSON serializeProductSummary(
    final EIProductSummary summary)
  {
    return new EISP1ProductSummaryJSON(
      schemaId(),
      summary.id().group(),
      summary.id().name(),
      summary.title()
    );
  }

  @Override
  public EISP1MessageType parse(
    final byte[] data)
    throws EIProtocolException
  {
    final EISP1JsonType m;
    try {
      m = this.mapper.readValue(data, EISP1JsonType.class);
    } catch (final IOException e) {
      throw new EIProtocolException(e.getMessage(), e);
    }

    if (m instanceof EISP1CommandLoginJSON login) {
      return mapLogin(login);
    }
    if (m instanceof EISP1ResponseErrorJSON error) {
      return mapError(error);
    }
    if (m instanceof EISP1ResponseImageCreatedJSON imageCreated) {
      return mapImageCreated(imageCreated);
    }
    if (m instanceof EISP1ResponseProductListJSON productList) {
      return mapProductList(productList);
    }
    if (m instanceof EISP1ResponseImageGetJSON imageGet) {
      return mapImageGet(imageGet);
    }

    throw new IllegalArgumentException(
      String.format("Unrecognized message: %s", m.getClass())
    );
  }

  @Override
  public byte[] serialize(
    final EISP1MessageType message)
    throws EIProtocolException
  {
    try {
      if (message instanceof EISP1CommandLogin login) {
        return this.serializeLogin(login);
      }
      if (message instanceof EISP1ResponseError error) {
        return this.serializeResponseError(error);
      }
      if (message instanceof EISP1ResponseImageCreated imageCreated) {
        return this.serializeImageCreated(imageCreated);
      }
      if (message instanceof EISP1ResponseProductList productList) {
        return this.serializeResponseProductList(productList);
      }
      if (message instanceof EISP1ResponseImageGet imageGet) {
        return this.serializeResponseImageGet(imageGet);
      }

    } catch (final JsonProcessingException e) {
      throw new EIProtocolException(e.getMessage(), e);
    }

    throw new IllegalArgumentException(
      String.format("Unrecognized message: %s", message.getClass())
    );
  }

  private byte[] serializeResponseImageGet(
    final EISP1ResponseImageGet imageGet)
    throws JsonProcessingException
  {
    return this.mapper.writeValueAsBytes(
      new EISP1ResponseImageGetJSON(
        schemaId(),
        imageGet.requestId(),
        imageGet.imageId(),
        new EISP1HashJSON(
          schemaId(),
          imageGet.hash().algorithm(),
          imageGet.hash().hash())
      )
    );
  }

  private byte[] serializeResponseProductList(
    final EISP1ResponseProductList productList)
    throws JsonProcessingException
  {
    return this.mapper.writeValueAsBytes(
      new EISP1ResponseProductListJSON(
        schemaId(),
        productList.requestId(),
        productList.items()
          .stream()
          .map(EISP1Messages::serializeProductSummary)
          .toList()
      )
    );
  }

  private byte[] serializeImageCreated(
    final EISP1ResponseImageCreated imageCreated)
    throws JsonProcessingException
  {
    return this.mapper.writeValueAsBytes(
      new EISP1ResponseImageCreatedJSON(
        schemaId(),
        imageCreated.requestId(),
        imageCreated.imageId()
      )
    );
  }

  private byte[] serializeResponseError(
    final EISP1ResponseError error)
    throws JsonProcessingException
  {
    return this.mapper.writeValueAsBytes(
      new EISP1ResponseErrorJSON(
        schemaId(),
        error.requestId(),
        error.errorCode(),
        error.message()
      )
    );
  }

  private byte[] serializeLogin(
    final EISP1CommandLogin login)
    throws JsonProcessingException
  {
    return this.mapper.writeValueAsBytes(
      new EISP1CommandLoginJSON(
        schemaId(),
        login.userName(),
        login.password()
      )
    );
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
