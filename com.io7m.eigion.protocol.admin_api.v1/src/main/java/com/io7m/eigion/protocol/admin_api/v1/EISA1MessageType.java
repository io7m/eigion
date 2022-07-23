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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.io7m.eigion.protocol.api.EIProtocolMessageType;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY;
import static java.util.Map.entry;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;

/**
 * The type of Admin API v1 messages.
 */

@JsonTypeInfo(
  use = JsonTypeInfo.Id.CUSTOM,
  include = JsonTypeInfo.As.PROPERTY,
  property = "%Type"
)
@JsonTypeIdResolver(EISA1ProductIdTypeResolver.class)
@JsonPropertyOrder({"%Schema", "%Type"})
public sealed interface EISA1MessageType
  extends EIProtocolMessageType
  permits EISA1CommandType,
  EISA1ResponseType,
  EISA1Transaction,
  EISA1TransactionResponse
{
  /**
   * A mapping of classes to type IDs.
   */

  Map<Class<?>, String> TYPE_ID_FOR_CLASS =
    Stream.of(
      EISA1CommandAuditGet.class,
      EISA1CommandLogin.class,
      EISA1CommandServicesList.class,
      EISA1CommandUserCreate.class,
      EISA1CommandUserGet.class,
      EISA1CommandUserGetByEmail.class,
      EISA1CommandUserGetByName.class,
      EISA1CommandUserSearch.class,
      EISA1Password.class,
      EISA1ResponseAuditGet.class,
      EISA1ResponseError.class,
      EISA1ResponseLogin.class,
      EISA1ResponseServiceList.class,
      EISA1ResponseUserCreate.class,
      EISA1ResponseUserGet.class,
      EISA1ResponseUserList.class,
      EISA1Service.class,
      EISA1Transaction.class,
      EISA1TransactionResponse.class,
      EISA1User.class,
      EISA1UserBan.class,
      EISA1UserSummary.class
    ).collect(toUnmodifiableMap(identity(), EISA1MessageType::typeIdOf));

  private static String typeIdOf(
    final Class<?> c)
  {
    return c.getSimpleName().replace("EISA1", "");
  }

  /**
   * A mapping of type IDs to classes.
   */

  Map<String, Class<?>> CLASS_FOR_TYPE_ID =
    makeClassForTypeId();

  private static Map<String, Class<?>> makeClassForTypeId()
  {
    return TYPE_ID_FOR_CLASS.entrySet()
      .stream()
      .map(e -> entry(e.getValue(), e.getKey()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * @return The schema identifier
   */

  @JsonProperty(value = "%Schema", required = false, access = READ_ONLY)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  default String schemaId()
  {
    return EISA1Messages.SCHEMA_ID;
  }
}
