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

import com.io7m.eigion.model.EIProductIdentifier;
import com.io7m.eigion.model.EIProductVersion;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import static com.io7m.eigion.model.EIProductIdentifier.VALID_ARTIFACT_NAME;
import static com.io7m.eigion.model.EIProductIdentifier.VALID_GROUP_NAME;
import static com.io7m.eigion.model.EIProductVersion.VALID_VERSION;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class EIProductIdentifierTest
{
  private static DynamicTest groupValidTest(
    final String text)
  {
    return DynamicTest.dynamicTest(
      "testValidGroup_%s".formatted(text),
      () -> {
        final var matcher = VALID_GROUP_NAME.matcher(text);
        assertTrue(matcher.matches());
      });
  }

  private static DynamicTest groupInvalidTest(
    final String text)
  {
    return DynamicTest.dynamicTest(
      "testInvalidGroup_%s".formatted(text),
      () -> {
        final var matcher = VALID_GROUP_NAME.matcher(text);
        assertFalse(matcher.matches());

        assertThrows(IllegalArgumentException.class, () -> {
          new EIProductIdentifier(text, "x");
        });
      });
  }

  private static DynamicTest artifactValidTest(
    final String text)
  {
    return DynamicTest.dynamicTest(
      "testValidArtifact_%s".formatted(text),
      () -> {
        final var matcher = VALID_ARTIFACT_NAME.matcher(text);
        assertTrue(matcher.matches());
      });
  }

  private static DynamicTest artifactInvalidTest(
    final String text)
  {
    return DynamicTest.dynamicTest(
      "testInvalidArtifact_%s".formatted(text),
      () -> {
        final var matcher = VALID_ARTIFACT_NAME.matcher(text);
        assertFalse(matcher.matches());

        assertThrows(IllegalArgumentException.class, () -> {
          new EIProductIdentifier("x", text);
        });
      });
  }

  private static DynamicTest versionValidTest(
    final String text)
  {
    return DynamicTest.dynamicTest(
      "testValidVersion_%s".formatted(text),
      () -> {
        final var matcher = VALID_VERSION.matcher(text);
        assertTrue(matcher.matches());
      });
  }

  private static DynamicTest versionInvalidTest(
    final String text)
  {
    return DynamicTest.dynamicTest(
      "testInvalidVersion_%s".formatted(text),
      () -> {
        final var matcher = VALID_VERSION.matcher(text);
        assertFalse(matcher.matches());

        assertThrows(IllegalArgumentException.class, () -> {
          EIProductVersion.parse(text);
        });
      });
  }

  @TestFactory
  public Stream<DynamicTest> testGroupValid()
  {
    return Stream.of(
      "a",
      "a9",
      "a-b",
      "a_b",
      "A-B",
      "A_b",
      "com.io7m.eigion"
    ).map(EIProductIdentifierTest::groupValidTest);
  }

  @TestFactory
  public Stream<DynamicTest> testGroupInvalid()
  {
    return Stream.of(
      " ",
      "9",
      "_",
      "-",
      "com._"
    ).map(EIProductIdentifierTest::groupInvalidTest);
  }

  @TestFactory
  public Stream<DynamicTest> testArtifactValid()
  {
    return Stream.of(
      "a",
      "a9",
      "a-b",
      "a_b",
      "A-B",
      "A_b",
      "com.io7m.eigion"
    ).map(EIProductIdentifierTest::artifactValidTest);
  }

  @TestFactory
  public Stream<DynamicTest> testArtifactInvalid()
  {
    return Stream.of(
      " ",
      "9",
      "_",
      "-",
      "com._"
    ).map(EIProductIdentifierTest::artifactInvalidTest);
  }

  @TestFactory
  public Stream<DynamicTest> testVersionValid()
  {
    return Stream.of(
      "1.0.0",
      "2.0.0-SNAPSHOT"
    ).map(EIProductIdentifierTest::versionValidTest);
  }

  @TestFactory
  public Stream<DynamicTest> testVersionInvalid()
  {
    return Stream.of(
      "0",
      "1.0",
      "2.0-SNAPSHOT",
      "a",
      "0.2.0a"
    ).map(EIProductIdentifierTest::versionInvalidTest);
  }
}
