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


package com.io7m.eigion.protocol.public_api.v1.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.io7m.eigion.protocol.public_api.v1.internal.EISP1ProductIdTypeResolver;

import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Map.entry;

// CHECKSTYLE:OFF

@JsonTypeInfo(
  use = JsonTypeInfo.Id.CUSTOM,
  include = JsonTypeInfo.As.PROPERTY,
  property = "%Type"
)
@JsonTypeIdResolver(EISP1ProductIdTypeResolver.class)
public sealed interface EISP1JsonType
  permits EISP1CommandLoginJSON,
  EISP1HashJSON,
  EISP1ResponseImageGetJSON,
  EISP1ProductSummaryJSON,
  EISP1ResponseErrorJSON,
  EISP1ResponseImageCreatedJSON,
  EISP1ResponseProductListJSON
{
  Map<Class<?>, String> TYPE_ID_FOR_CLASS =
    Map.ofEntries(
      entry(
        EISP1ResponseProductListJSON.class,
        EISP1ResponseProductListJSON.TYPE_ID),
      entry(
        EISP1ProductSummaryJSON.class,
        EISP1ProductSummaryJSON.TYPE_ID),
      entry(
        EISP1CommandLoginJSON.class,
        EISP1CommandLoginJSON.TYPE_ID),
      entry(
        EISP1ResponseErrorJSON.class,
        EISP1ResponseErrorJSON.TYPE_ID),
      entry(
        EISP1ResponseImageCreatedJSON.class,
        EISP1ResponseImageCreatedJSON.TYPE_ID),
      entry(
        EISP1ResponseImageGetJSON.class,
        EISP1ResponseImageGetJSON.TYPE_ID),
      entry(
        EISP1HashJSON.class,
        EISP1HashJSON.TYPE_ID)
    );

  Map<String, Class<?>> CLASS_FOR_TYPE_ID =
    makeClassForTypeId();

  private static Map<String, Class<?>> makeClassForTypeId()
  {
    return TYPE_ID_FOR_CLASS.entrySet()
      .stream()
      .map(e -> entry(e.getValue(), e.getKey()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
