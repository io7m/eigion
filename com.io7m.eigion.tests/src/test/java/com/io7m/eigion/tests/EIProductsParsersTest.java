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

import com.io7m.anethum.common.ParseException;
import com.io7m.eigion.product.api.EIProducts;
import com.io7m.eigion.product.parser.EIProductsParsers;
import com.io7m.eigion.product.parser.EIProductsSerializers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.io7m.eigion.product.api.EIProductCategory.category;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static java.util.Optional.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class EIProductsParsersTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIProductsParsersTest.class);

  private EIProductsParsers parsers;
  private EIProductsSerializers serializers;
  private Path directory;

  @BeforeEach
  public void setup()
    throws IOException
  {
    this.parsers =
      new EIProductsParsers();
    this.serializers =
      new EIProductsSerializers();
    this.directory =
      EITestDirectories.createTempDirectory();
  }

  @AfterEach
  public void tearDown()
    throws IOException
  {
    EITestDirectories.deleteDirectory(this.directory);
  }

  @Test
  public void testExample0()
    throws Exception
  {
    final var products =
      this.parseFile(this.resourceOf("products-ex-0.json"));

    assertEquals(1, products.products().size());

    {
      final var p = products.products().get(0);
      assertEquals(ONE, p.id().version().major());
      assertEquals(ZERO, p.id().version().minor());
      assertEquals(ZERO, p.id().version().patch());
      assertEquals(empty(), p.id().version().qualifier());
      assertEquals(List.of(), p.bundleDependencies());
      assertEquals(List.of(), p.productDependencies());
      assertEquals(Set.of(
        category("Category 0"),
        category("Category 1"),
        category("Category 2")
      ), p.categories());
    }
  }

  @Test
  public void testExample1()
    throws Exception
  {
    final var products =
      this.parseFile(this.resourceOf("products-ex-1.json"));

    assertEquals(1, products.products().size());

    {
      final var p = products.products().get(0);
      assertEquals(ONE, p.id().version().major());
      assertEquals(ZERO, p.id().version().minor());
      assertEquals(ZERO, p.id().version().patch());
      assertEquals(Optional.of("SNAPSHOT"), p.id().version().qualifier());
      assertEquals(List.of(), p.bundleDependencies());
      assertEquals(List.of(), p.productDependencies());
      assertEquals(Set.of(
        category("Category 0"),
        category("Category 1"),
        category("Category 2")
      ), p.categories());
    }
  }

  @Test
  public void testError0()
    throws Exception
  {
    final var ex = assertThrows(ParseException.class, () -> {
      this.parseFile(this.resourceOf("products-error-0.json"));
    });

    assertTrue(
      ex.statusValues()
        .stream()
        .anyMatch(s -> Objects.equals(s.errorCode(), "invalid-identifier"))
    );
  }

  @Test
  public void testError1()
    throws Exception
  {
    final var ex = assertThrows(ParseException.class, () -> {
      this.parseFile(this.resourceOf("products-error-1.json"));
    });

    assertTrue(
      ex.statusValues()
        .stream()
        .anyMatch(s -> Objects.equals(s.errorCode(), "invalid-identifier"))
    );
  }

  @Test
  public void testError2()
    throws Exception
  {
    final var ex = assertThrows(ParseException.class, () -> {
      this.parseFile(this.resourceOf("products-error-2.json"));
    });

    assertTrue(
      ex.statusValues()
        .stream()
        .anyMatch(s -> Objects.equals(s.errorCode(), "databind"))
    );
  }

  @Test
  public void testError3()
    throws Exception
  {
    final var ex = assertThrows(ParseException.class, () -> {
      this.parseFile(this.resourceOf("products-error-3.json"));
    });

    assertTrue(
      ex.statusValues()
        .stream()
        .anyMatch(s -> Objects.equals(s.errorCode(), "invalid-category"))
    );
  }

  @Test
  public void testRoundTripExample0()
    throws Exception
  {
    final var products =
      this.roundTrip(this.resourceOf("products-ex-0.json"));

    assertEquals(1, products.products().size());

    {
      final var p = products.products().get(0);
      assertEquals(ONE, p.id().version().major());
      assertEquals(ZERO, p.id().version().minor());
      assertEquals(ZERO, p.id().version().patch());
      assertEquals(empty(), p.id().version().qualifier());
      assertEquals(List.of(), p.bundleDependencies());
      assertEquals(List.of(), p.productDependencies());
      assertEquals(Set.of(
        category("Category 0"),
        category("Category 1"),
        category("Category 2")
      ), p.categories());
    }
  }

  @Test
  public void testRoundTripExample1()
    throws Exception
  {
    final var products =
      this.roundTrip(this.resourceOf("products-ex-1.json"));

    assertEquals(1, products.products().size());

    {
      final var p = products.products().get(0);
      assertEquals(ONE, p.id().version().major());
      assertEquals(ZERO, p.id().version().minor());
      assertEquals(ZERO, p.id().version().patch());
      assertEquals(Optional.of("SNAPSHOT"), p.id().version().qualifier());
      assertEquals(List.of(), p.bundleDependencies());
      assertEquals(List.of(), p.productDependencies());
      assertEquals(Set.of(
        category("Category 0"),
        category("Category 1"),
        category("Category 2")
      ), p.categories());
    }
  }


  private EIProducts roundTrip(
    final Path file)
    throws Exception
  {
    final var parsed = this.parseFile(file);
    final var tmp = this.directory.resolve("tmp.json");
    LOG.debug("tmp: {}", tmp);
    this.serializers.serializeFile(tmp, parsed);
    return this.parseFile(tmp);
  }

  private EIProducts parseFile(
    final Path file)
    throws Exception
  {
    try {
      return this.parsers.parseFile(file);
    } catch (final IOException e) {
      throw e;
    } catch (final ParseException e) {
      for (final var error : e.statusValues()) {
        LOG.debug("error: {}", error);
      }
      throw e;
    }
  }

  private Path resourceOf(
    final String name)
    throws IOException
  {
    return EITestDirectories.resourceOf(
      EIProductsParsersTest.class,
      this.directory,
      name
    );
  }
}
