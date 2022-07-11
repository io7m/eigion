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
import com.io7m.eigion.storage.api.EIStorageName;
import com.io7m.eigion.storage.api.EIStorageParameters;
import com.io7m.eigion.storage.api.EIStorageType;
import com.io7m.eigion.storage.api.EIStored;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class EIFakeStorage implements EIStorageType
{
  private final Map<EIStorageName, EIStored> data;

  public EIFakeStorage(
    final EIStorageParameters parameters)
  {
    this.data = new ConcurrentHashMap<>();
  }

  @Override
  public void put(
    final EIStorageName name,
    final String contentType,
    final EIHash hash,
    final InputStream data)
    throws IOException
  {
    final var bytes = data.readAllBytes();

    this.data.put(
      name,
      new EIStored(
        name,
        contentType,
        Integer.toUnsignedLong(bytes.length),
        hash,
        new ByteArrayInputStream(bytes)
      )
    );
  }

  @Override
  public void delete(
    final EIStorageName name)
  {
    this.data.remove(name);
  }

  @Override
  public Optional<EIStored> get(
    final EIStorageName name)
    throws IOException
  {
    return Optional.ofNullable(this.data.get(name));
  }
}
