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
import com.io7m.anethum.common.ParseStatus;
import com.io7m.anethum.common.SerializeException;
import com.io7m.eigion.model.EIProductBundleDependency;
import com.io7m.eigion.model.EIProductCategory;
import com.io7m.eigion.model.EIProductDependency;
import com.io7m.eigion.hash.EIHash;
import com.io7m.eigion.model.EIProductIdentifier;
import com.io7m.eigion.model.EIProductVersion;
import com.io7m.eigion.model.EIProducts;
import com.io7m.eigion.product.parser.EIProductsParsers;
import com.io7m.eigion.product.parser.EIProductsSerializers;
import com.io7m.eigion.product.parser.api.EIProductsSerializerConfiguration;
import org.apache.commons.io.input.BrokenInputStream;
import org.apache.commons.io.output.BrokenOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.io7m.eigion.model.EIProductCategory.category;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TWO;
import static java.math.BigInteger.ZERO;
import static java.util.Optional.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class EIProductsParsersTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIProductsParsersTest.class);

  private static final EIProductIdentifier EXAMPLE_ID =
    new EIProductIdentifier(
      "com.io7m.eigion",
      "com.io7m.eigion.api"
    );

  private static final EIProductDependency PRODUCT_DEPENDENCY_0 =
    new EIProductDependency(
      new EIProductIdentifier(
        "com.io7m.zed",
        "com.io7m.zed"
      ),
      new EIProductVersion(TWO, ZERO, ZERO, empty())
    );

  private static final EIProductBundleDependency BUNDLE_DEPENDENCY_0 =
    new EIProductBundleDependency(
      new EIProductIdentifier(
        "com.io7m.ex",
        "com.io7m.ex"
      ),
      new EIProductVersion(ONE, ZERO, ZERO, empty()),
      new EIHash(
        "SHA-256",
        "5891B5B522D5DF086D0FF0B110FBD9D21BB4FC7163AF34D08286A2E846F6BE03"
      ),
      List.of(
        URI.create(
          "https://www.example.com/bundles/com.io7m.ex/com.io7m.ex/1.0.0")
      )
    );

  private static final Set<EIProductCategory> EXAMPLE_CATEGORIES =
    Set.of(
      category("Category 0"),
      category("Category 1"),
      category("Category 2")
    );

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

  /**
   * A simple parsed example.
   *
   * @throws Exception On errors
   */

  @Test
  public void testExample0()
    throws Exception
  {
    final var products =
      this.parseFile(this.resourceOf("products-ex-0.json"));

    assertEquals(1, products.products().size());

    {
      final var p = products.products().get(0);
      assertEquals(EXAMPLE_ID, p.id());
      assertEquals(EXAMPLE_CATEGORIES, p.description().categories());

      final var r = p.releases().get(0);
      assertEquals(ONE, r.version().major());
      assertEquals(ZERO, r.version().minor());
      assertEquals(ZERO, r.version().patch());
      assertEquals(empty(), r.version().qualifier());
      assertEquals(List.of(PRODUCT_DEPENDENCY_0), r.productDependencies());
      assertEquals(List.of(BUNDLE_DEPENDENCY_0), r.bundleDependencies());
      assertEquals(1, p.releases().size());
    }
  }

  /**
   * A simple parsed example.
   *
   * @throws Exception On errors
   */

  @Test
  public void testExample1()
    throws Exception
  {
    final var products =
      this.parseFile(this.resourceOf("products-ex-1.json"));

    assertEquals(1, products.products().size());

    {
      final var p = products.products().get(0);
      assertEquals(EXAMPLE_ID, p.id());
      assertEquals(EXAMPLE_CATEGORIES, p.description().categories());

      final var r = p.releases().get(0);
      assertEquals(ONE, r.version().major());
      assertEquals(ZERO, r.version().minor());
      assertEquals(ZERO, r.version().patch());
      assertEquals(Optional.of("SNAPSHOT"), r.version().qualifier());
      assertEquals(List.of(PRODUCT_DEPENDENCY_0), r.productDependencies());
      assertEquals(List.of(BUNDLE_DEPENDENCY_0), r.bundleDependencies());
      assertEquals(1, p.releases().size());
    }
  }

  /**
   * Error cases and their error codes.
   *
   * @return A stream of test cases
   */

  @TestFactory
  public Stream<DynamicTest> testErrorCases()
  {
    return Stream.of(
      new ErrorCase("products-error-0.json", "invalid-version"),
      new ErrorCase("products-error-1.json", "invalid-version"),
      new ErrorCase("products-error-2.json", "databind"),
      new ErrorCase("products-error-3.json", "invalid-category"),
      new ErrorCase("products-error-4.json", "invalid-hash"),
      new ErrorCase("products-error-5.json", "databind"),
      new ErrorCase("products-error-6.json", "databind"),
      new ErrorCase("products-error-7.json", "databind"),
      new ErrorCase("products-error-8.json", "invalid-link"),
      new ErrorCase("products-error-9.json", "invalid-product-description"),
      new ErrorCase("products-error-10.json", "invalid-rich-text"),
      new ErrorCase("products-error-11.json", "invalid-identifier")
    ).map(this::errorCaseFor);
  }

  private DynamicTest errorCaseFor(
    final ErrorCase e)
  {
    return DynamicTest.dynamicTest(
      "testErrorCase_" + e.file,
      () -> this.runForError(e.file, e.errorCode)
    );
  }

  private void runForError(
    final String name,
    final String code)
  {
    final var ex = assertThrows(ParseException.class, () -> {
      this.parseFile(this.resourceOf(name));
    });

    assertTrue(
      ex.statusValues()
        .stream()
        .anyMatch(s -> Objects.equals(s.errorCode(), code)),
      String.format(
        "At least one error must be '%s' (received %s)",
        code,
        ex.statusValues()
          .stream()
          .map(ParseStatus::errorCode)
          .toList()
      )
    );
  }

  /**
   * A round-trip parse and serialize works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testRoundTripExample0()
    throws Exception
  {
    final var products =
      this.roundTrip(this.resourceOf("products-ex-0.json"));

    assertEquals(1, products.products().size());

    {
      final var p = products.products().get(0);
      assertEquals(EXAMPLE_ID, p.id());
      assertEquals(EXAMPLE_CATEGORIES, p.description().categories());

      final var r = p.releases().get(0);
      assertEquals(ONE, r.version().major());
      assertEquals(ZERO, r.version().minor());
      assertEquals(ZERO, r.version().patch());
      assertEquals(empty(), r.version().qualifier());
      assertEquals(List.of(PRODUCT_DEPENDENCY_0), r.productDependencies());
      assertEquals(List.of(BUNDLE_DEPENDENCY_0), r.bundleDependencies());
      assertEquals(1, p.releases().size());
    }
  }

  /**
   * A round-trip parse and serialize works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testRoundTripExample1()
    throws Exception
  {
    final var products =
      this.roundTrip(this.resourceOf("products-ex-1.json"));

    assertEquals(1, products.products().size());

    {
      final var p = products.products().get(0);
      assertEquals(EXAMPLE_ID, p.id());
      assertEquals(EXAMPLE_CATEGORIES, p.description().categories());

      final var r = p.releases().get(0);
      assertEquals(ONE, r.version().major());
      assertEquals(ZERO, r.version().minor());
      assertEquals(ZERO, r.version().patch());
      assertEquals(Optional.of("SNAPSHOT"), r.version().qualifier());
      assertEquals(List.of(PRODUCT_DEPENDENCY_0), r.productDependencies());
      assertEquals(List.of(BUNDLE_DEPENDENCY_0), r.bundleDependencies());
      assertEquals(1, p.releases().size());
    }
  }

  /**
   * An unknown format version fails.
   */

  @Test
  public void testSerializerFormatUnknown()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      this.serializers.serializeFileWithContext(
        new EIProductsSerializerConfiguration(9999),
        this.directory.resolve("tmp.json"),
        new EIProducts(List.of())
      );
    });
  }

  /**
   * A broken stream fails.
   */

  @Test
  public void testSerializerBrokenStream()
  {
    assertThrows(SerializeException.class, () -> {
      this.serializers.createSerializerWithContext(
        new EIProductsSerializerConfiguration(1),
        URI.create("urn:source"),
        new BrokenOutputStream()
      ).execute(new EIProducts(List.of()));
    });
  }

  /**
   * A broken stream fails.
   */

  @Test
  public void testParserBrokenStream()
  {
    assertThrows(ParseException.class, () -> {
      this.parsers.createParserWithContext(
        null,
        URI.create("urn:source"),
        new BrokenInputStream(),
        parseStatus -> {
        }
      ).execute();
    });
  }

  /**
   * An unsupported version fails.
   */

  @Test
  public void testSerializerUnsupported0()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      this.serializers.createSerializerWithContext(
        new EIProductsSerializerConfiguration(999999),
        URI.create("urn:source"),
        new ByteArrayOutputStream()
      );
    });
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

  private record ErrorCase(
    String file,
    String errorCode)
  {

  }
}
