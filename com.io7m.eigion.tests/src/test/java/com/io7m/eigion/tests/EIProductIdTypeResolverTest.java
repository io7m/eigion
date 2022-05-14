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


package com.io7m.eigion.tests;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.io7m.eigion.product.parser.internal.EIProductIdTypeResolver;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class EIProductIdTypeResolverTest
{
  @Test
  public void testExceptions()
  {
    assertAll(
      () -> {
        assertThrows(IllegalStateException.class, () -> {
          new EIProductIdTypeResolver().idFromBaseType();
        });
      },
      () -> {
        assertThrows(IllegalArgumentException.class, () -> {
          new EIProductIdTypeResolver().idFromValue(new Object());
        });
      },
      () -> {
        assertThrows(IllegalStateException.class, () -> {
          new EIProductIdTypeResolver().idFromBaseType();
        });
      },
      () -> {
        assertThrows(IllegalStateException.class, () -> {
          new EIProductIdTypeResolver().getDescForKnownTypeIds();
        });
      },
      () -> {
        assertThrows(IllegalStateException.class, () -> {
          new EIProductIdTypeResolver().idFromValueAndType(
            new Object(),
            Integer.class);
        });
      }
    );
  }

  @Test
  public void testMechanism()
  {
    assertEquals(
      JsonTypeInfo.Id.CUSTOM,
      new EIProductIdTypeResolver().getMechanism()
    );
  }

  @Test
  public void testIdVersion1()
    throws IOException
  {
    final var context =
      Mockito.mock(DatabindContext.class);

    final var type =
      new EIProductIdTypeResolver()
        .typeFromId(context, "https://www.io7m.com/eigion/products-1.json");

    assertEquals(
      "Lcom/io7m/eigion/product/parser/internal/v1/EIv1Products;",
      type.getErasedSignature()
    );
  }

  @Test
  public void testIdUnrecognized()
    throws IOException
  {
    final var context =
      Mockito.mock(DatabindContext.class);

    assertThrows(IOException.class, () -> {
      new EIProductIdTypeResolver()
        .typeFromId(context, "unrecognized");
    });
  }
}
