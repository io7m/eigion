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

import com.io7m.eigion.model.EIProductVersion;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TWO;
import static java.math.BigInteger.ZERO;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class EIProductVersionTest
{
  @Test
  public void testCompare0()
  {
    final var v0 =
      new EIProductVersion(ONE, ZERO, ZERO, empty());
    final var v1 =
      new EIProductVersion(ONE, ZERO, ZERO, empty());

    assertEquals(0, v0.compareTo(v1));
    assertEquals(0, v1.compareTo(v0));
  }

  @Test
  public void testCompareMajor()
  {
    final var v0 =
      new EIProductVersion(ONE, ZERO, ZERO, empty());
    final var v1 =
      new EIProductVersion(TWO, ZERO, ZERO, empty());

    assertTrue(v0.compareTo(v1) < 0);
    assertTrue(v1.compareTo(v0) > 0);
  }

  @Test
  public void testCompareMinor()
  {
    final var v0 =
      new EIProductVersion(ONE, ZERO, ZERO, empty());
    final var v1 =
      new EIProductVersion(ONE, ONE, ZERO, empty());

    assertTrue(v0.compareTo(v1) < 0);
    assertTrue(v1.compareTo(v0) > 0);
  }

  @Test
  public void testComparePatch()
  {
    final var v0 =
      new EIProductVersion(ONE, ZERO, ZERO, empty());
    final var v1 =
      new EIProductVersion(ONE, ZERO, ONE, empty());

    assertTrue(v0.compareTo(v1) < 0);
    assertTrue(v1.compareTo(v0) > 0);
  }

  @Test
  public void testCompareQualifier0()
  {
    final var v0 =
      new EIProductVersion(ONE, ZERO, ZERO, of("a"));
    final var v1 =
      new EIProductVersion(ONE, ZERO, ZERO, empty());

    assertTrue(v0.compareTo(v1) < 0);
    assertTrue(v1.compareTo(v0) > 0);
  }

  @Test
  public void testCompareQualifier1()
  {
    final var v0 =
      new EIProductVersion(ONE, ZERO, ZERO, of("a"));
    final var v1 =
      new EIProductVersion(ONE, ZERO, ZERO, of("b"));

    assertTrue(v0.compareTo(v1) < 0);
    assertTrue(v1.compareTo(v0) > 0);
  }

  @Test
  public void testInvalid0()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new EIProductVersion(new BigInteger("-1"), ZERO, ZERO, empty());
    });
  }

  @Test
  public void testInvalid1()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new EIProductVersion(ZERO, new BigInteger("-1"), ZERO, empty());
    });
  }

  @Test
  public void testInvalid2()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new EIProductVersion(ZERO, ZERO, new BigInteger("-1"), empty());
    });
  }

  @Test
  public void testInvalid3()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new EIProductVersion(ZERO, ZERO, ZERO, of("-"));
    });
  }
}
