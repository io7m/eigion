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


package com.io7m.eigion.server.vanilla.internal;

import com.io7m.eigion.hash.EIHash;
import com.io7m.eigion.services.api.EIServiceType;
import com.io7m.eigion.storage.api.EIStorageConfigurationException;
import com.io7m.eigion.storage.api.EIStorageFactoryType;
import com.io7m.eigion.storage.api.EIStorageName;
import com.io7m.eigion.storage.api.EIStorageParameters;
import com.io7m.eigion.storage.api.EIStorageType;
import com.io7m.eigion.storage.api.EIStored;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

/**
 * A storage service for images.
 */

public final class EIServerImageStorage
  implements EIServiceType, EIStorageType
{
  private final EIStorageType storage;

  private EIServerImageStorage(
    final EIStorageType inStorage)
  {
    this.storage =
      Objects.requireNonNull(inStorage, "storage");
  }

  /**
   * Create a storage service for images.
   *
   * @param factory    The storage factory
   * @param parameters The storage parameters
   *
   * @return A storage service
   *
   * @throws EIStorageConfigurationException On configuration errors
   * @throws IOException                     On I/O errors
   */

  public static EIServerImageStorage create(
    final EIStorageFactoryType factory,
    final EIStorageParameters parameters)
    throws EIStorageConfigurationException, IOException
  {
    Objects.requireNonNull(factory, "factory");
    Objects.requireNonNull(parameters, "parameters");
    return new EIServerImageStorage(factory.create(parameters));
  }

  @Override
  public String description()
  {
    return "Image storage service.";
  }

  @Override
  public void put(
    final EIStorageName name,
    final String contentType,
    final EIHash hash,
    final InputStream data)
    throws IOException
  {
    this.storage.put(name, contentType, hash, data);
  }

  @Override
  public void delete(
    final EIStorageName name)
    throws IOException
  {
    this.storage.delete(name);
  }

  @Override
  public Optional<EIStored> get(
    final EIStorageName name)
    throws IOException
  {
    return this.storage.get(name);
  }

  @Override
  public String toString()
  {
    return "[EIServerImageStorage 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode()));
  }
}
