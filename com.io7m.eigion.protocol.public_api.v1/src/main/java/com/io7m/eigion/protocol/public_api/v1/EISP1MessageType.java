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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.io7m.eigion.protocol.api.EIProtocolMessageType;

import java.util.Map;
import java.util.stream.Stream;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY;
import static java.util.Map.entry;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;

/**
 * The type of Public API v1 messages.
 */

@JsonTypeInfo(
  use = JsonTypeInfo.Id.CUSTOM,
  include = JsonTypeInfo.As.PROPERTY,
  property = "%Type"
)
@JsonTypeIdResolver(EISP1ProductIdTypeResolver.class)
@JsonPropertyOrder({"%Schema", "%Type"})
public sealed interface EISP1MessageType
  extends EIProtocolMessageType
  permits EISP1CommandType, EISP1ResponseType
{
  /**
   * A mapping of classes to type IDs.
   */

  Map<Class<?>, String> TYPE_ID_FOR_CLASS =
    Stream.of(
      EISP1CommandGroupCreateBegin.class,
      EISP1CommandGroupCreateCancel.class,
      EISP1CommandGroupCreateReady.class,
      EISP1CommandGroupCreateRequests.class,
      EISP1CommandGroupInvite.class,
      EISP1CommandGroupInviteByName.class,
      EISP1CommandGroupInviteRespond.class,
      EISP1CommandGroupInvitesReceived.class,
      EISP1CommandGroupInvitesSent.class,
      EISP1CommandGroupLeave.class,
      EISP1CommandGroups.class,
      EISP1CommandLogin.class,
      EISP1GroupCreationRequest.class,
      EISP1GroupInviteStatus.class,
      EISP1GroupRoles.class,
      EISP1Hash.class,
      EISP1MessageType.class,
      EISP1ProductSummary.class,
      EISP1ResponseError.class,
      EISP1ResponseGroupCreateBegin.class,
      EISP1ResponseGroupCreateCancel.class,
      EISP1ResponseGroupCreateReady.class,
      EISP1ResponseGroupCreateRequests.class,
      EISP1ResponseGroupInvite.class,
      EISP1ResponseGroupInviteRespond.class,
      EISP1ResponseGroupInvites.class,
      EISP1ResponseGroupLeave.class,
      EISP1ResponseGroups.class,
      EISP1ResponseImageCreated.class,
      EISP1ResponseImageGet.class,
      EISP1ResponseLogin.class,
      EISP1ResponseProductList.class
    ).collect(toUnmodifiableMap(identity(), EISP1MessageType::typeIdOf));
  /**
   * A mapping of type IDs to classes.
   */

  Map<String, Class<?>> CLASS_FOR_TYPE_ID =
    makeClassForTypeId();

  private static String typeIdOf(
    final Class<?> c)
  {
    return c.getSimpleName().replace("EISP1", "");
  }

  private static Map<String, Class<?>> makeClassForTypeId()
  {
    return TYPE_ID_FOR_CLASS.entrySet()
      .stream()
      .map(e -> entry(e.getValue(), e.getKey()))
      .collect(toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * @return The schema identifier
   */

  @JsonProperty(value = "%Schema", required = false, access = READ_ONLY)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  default String schemaId()
  {
    return EISP1Messages.SCHEMA_ID;
  }
}
