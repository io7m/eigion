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

import com.io7m.eigion.hash.EIHash;
import com.io7m.eigion.storage.api.EIStorageParameters;
import com.io7m.eigion.storage.derby.EIStorageDerbyFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class EIStorageDerbyTest
{
  private Path directory;
  private EIStorageDerbyFactory storage;
  private String databaseFile;

  @BeforeEach
  public void setup()
    throws Exception
  {
    this.directory =
      EITestDirectories.createTempDirectory();
    this.databaseFile =
      this.directory.resolve("file.db").toString();

    this.storage =
      new EIStorageDerbyFactory();
  }

  @AfterEach
  public void tearDown()
    throws Exception
  {
    EITestDirectories.deleteDirectory(this.directory);
  }

  @Test
  public void testPutGet()
    throws Exception
  {
    final var storage =
      this.storage.create(
        new EIStorageParameters(Map.of("file", this.databaseFile)));

    final var hash =
      new EIHash(
        "SHA-256",
        "2CF24DBA5FB0A30E26E83B2AC5B9E29E1B161E5C1FA7425E73043362938B9824"
      );

    final var helloBytes =
      "hello".getBytes(UTF_8);

    storage.put(
      "/x",
      "text/plain",
      hash,
      new ByteArrayInputStream(helloBytes)
    );

    final var stored =
      storage.get("/x").orElseThrow();

    assertEquals(hash, stored.hash());
    assertEquals(5L, stored.contentSize());
    assertArrayEquals(helloBytes, stored.data().readAllBytes());
    assertEquals("text/plain", stored.contentType());

    storage.delete("/x");
    assertEquals(Optional.empty(), storage.get("/x"));
  }
}
