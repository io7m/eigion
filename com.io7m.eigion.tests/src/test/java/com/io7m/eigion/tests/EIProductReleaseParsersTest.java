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
import com.io7m.eigion.model.EIChange;
import com.io7m.eigion.model.EIChangeTicket;
import com.io7m.eigion.model.EICreation;
import com.io7m.eigion.model.EIProductBundleDependency;
import com.io7m.eigion.model.EIProductDependency;
import com.io7m.eigion.model.EIProductHash;
import com.io7m.eigion.model.EIProductIdentifier;
import com.io7m.eigion.model.EIProductRelease;
import com.io7m.eigion.model.EIProductVersion;
import com.io7m.eigion.product.parser.EIProductReleaseParsers;
import com.io7m.eigion.product.parser.EIProductReleaseSerializers;
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
import java.util.stream.Stream;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TWO;
import static java.math.BigInteger.ZERO;
import static java.util.Optional.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class EIProductReleaseParsersTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIProductReleaseParsersTest.class);

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
      new EIProductHash(
        "SHA-256",
        "5891B5B522D5DF086D0FF0B110FBD9D21BB4FC7163AF34D08286A2E846F6BE03"
      ),
      List.of(
        URI.create(
          "https://www.example.com/bundles/com.io7m.ex/com.io7m.ex/1.0.0")
      )
    );

  private static final EIChange CHANGE_0 =
    new EIChange(
      "Description",
      List.of(
        new EIChangeTicket("X", URI.create("https://x.com/X")),
        new EIChangeTicket("Y", URI.create("https://x.com/Y")),
        new EIChangeTicket("Z", URI.create("https://x.com/Z"))
      )
    );

  private static final EIProductRelease RELEASE_0 =
    new EIProductRelease(
      new EIProductVersion(ONE, ZERO, ZERO, empty()),
      List.of(PRODUCT_DEPENDENCY_0),
      List.of(BUNDLE_DEPENDENCY_0),
      List.of(CHANGE_0),
      empty(),
      EICreation.zero()
    );

  private EIProductReleaseParsers parsers;
  private EIProductReleaseSerializers serializers;
  private Path directory;

  @BeforeEach
  public void setup()
    throws IOException
  {
    this.parsers =
      new EIProductReleaseParsers();
    this.serializers =
      new EIProductReleaseSerializers();
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
   * Error cases and their error codes.
   *
   * @return A stream of test cases
   */

  @TestFactory
  public Stream<DynamicTest> testErrorCases()
  {
    return Stream.of(
      new ErrorCase("release-error-0.json", "invalid-hash"),
      new ErrorCase("release-error-1.json", "databind")
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
    final var r =
      this.roundTrip(this.resourceOf("release-ex-0.json"));

    assertEquals(ONE, r.version().major());
    assertEquals(ZERO, r.version().minor());
    assertEquals(ZERO, r.version().patch());
    assertEquals(empty(), r.version().qualifier());
    assertEquals(List.of(PRODUCT_DEPENDENCY_0), r.productDependencies());
    assertEquals(List.of(BUNDLE_DEPENDENCY_0), r.bundleDependencies());
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
    final var r =
      this.roundTrip(this.resourceOf("release-ex-1.json"));

    assertEquals(ONE, r.version().major());
    assertEquals(ZERO, r.version().minor());
    assertEquals(ZERO, r.version().patch());
    assertEquals(Optional.of("SNAPSHOT"), r.version().qualifier());
    assertEquals(List.of(PRODUCT_DEPENDENCY_0), r.productDependencies());
    assertEquals(List.of(BUNDLE_DEPENDENCY_0), r.bundleDependencies());
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
        RELEASE_0
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
      ).execute(RELEASE_0);
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

  private EIProductRelease roundTrip(
    final Path file)
    throws Exception
  {
    final var parsed = this.parseFile(file);
    final var tmp = this.directory.resolve("tmp.json");
    LOG.debug("tmp: {}", tmp);
    this.serializers.serializeFile(tmp, parsed);
    return this.parseFile(tmp);
  }

  private EIProductRelease parseFile(
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
      EIProductReleaseParsersTest.class,
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
