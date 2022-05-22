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


package com.io7m.eigion.product.parser.internal;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.io7m.eigion.product.parser.api.EIProductSchemas;
import com.io7m.eigion.product.parser.internal.v1.EIv1Products;

import java.io.IOException;

import static com.io7m.eigion.product.parser.api.EIProductSchemas.VERSION_1;

/**
 * A type resolver that uses the contents of an '%Type' property.
 */

public final class EIProductIdTypeResolver implements TypeIdResolver
{
  /**
   * A type resolver that uses the contents of an '%Type' property.
   */

  public EIProductIdTypeResolver()
  {

  }

  @Override
  public void init(
    final JavaType javaType)
  {

  }

  @Override
  public String idFromValue(
    final Object o)
  {
    if (o instanceof EIv1Products) {
      return EIProductSchemas.version1();
    }
    throw new IllegalArgumentException("Unrecognized type: " + o.getClass());
  }

  @Override
  public String idFromValueAndType(
    final Object o,
    final Class<?> aClass)
  {
    throw new IllegalStateException();
  }

  @Override
  public String idFromBaseType()
  {
    throw new IllegalStateException();
  }

  @Override
  public JavaType typeFromId(
    final DatabindContext databindContext,
    final String id)
    throws IOException
  {
    return switch (id) {
      case VERSION_1 -> {
        yield TypeFactory.defaultInstance().constructType(EIv1Products.class);
      }
      default -> {
        throw new IOException("Unrecognized %Type type: " + id);
      }
    };
  }

  @Override
  public String getDescForKnownTypeIds()
  {
    throw new IllegalStateException();
  }

  @Override
  public JsonTypeInfo.Id getMechanism()
  {
    return JsonTypeInfo.Id.CUSTOM;
  }
}
