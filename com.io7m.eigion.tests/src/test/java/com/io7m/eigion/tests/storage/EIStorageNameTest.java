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


package com.io7m.eigion.tests.storage;

import com.io7m.eigion.storage.api.EIStorageName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class EIStorageNameTest
{
  @TestFactory
  public Stream<DynamicTest> testValid()
  {
    return Stream.of(
        "/x",
        "//////images/////68e3939e-f64e-4e66-bf71-9bf860b682c1.jpg",
        "/images/68e3939e-f64e-4e66-bf71-9bf860b682c1.jpg")
      .map(EIStorageNameTest::validTestOf);
  }

  @TestFactory
  public Stream<DynamicTest> testInvalid()
  {
    return Stream.of(
        "/xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
          "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
          "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
          "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
        "x",
        ".",
        "..",
        "/..",
        "/.",
        "/a/../b",
        "/a/./b")
      .map(EIStorageNameTest::invalidTestOf);
  }

  private static DynamicTest validTestOf(
    final String text)
  {
    return DynamicTest.dynamicTest("testValid_" + text, () -> {
      new EIStorageName(text);
    });
  }

  private static DynamicTest invalidTestOf(
    final String text)
  {
    return DynamicTest.dynamicTest("testInvalid_" + text, () -> {
      assertThrows(IllegalArgumentException.class, () -> {
        new EIStorageName(text);
      });
    });
  }
}
