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

package com.io7m.eigion.storage.api;

import com.io7m.eigion.hash.EIHash;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * A trivial storage interface.
 */

public interface EIStorageType
{
  /**
   * Create or update an object.
   *
   * @param name        The name
   * @param contentType The content type
   * @param hash        A hash of the content
   * @param data        The data
   *
   * @throws IOException On errors
   */

  void put(
    EIStorageName name,
    String contentType,
    EIHash hash,
    InputStream data)
    throws IOException;

  /**
   * Create or update an object.
   *
   * @param name        The name
   * @param contentType The content type
   * @param hash        A hash of the content
   * @param data        The data
   *
   * @throws IOException On errors
   */

  default void put(
    final String name,
    final String contentType,
    final EIHash hash,
    final InputStream data)
    throws IOException
  {
    this.put(new EIStorageName(name), contentType, hash, data);
  }

  /**
   * Delete an object.
   *
   * @param name The name
   *
   * @throws IOException On errors
   */

  void delete(EIStorageName name)
    throws IOException;

  /**
   * Delete an object.
   *
   * @param name The name
   *
   * @throws IOException On errors
   */

  default void delete(
    final String name)
    throws IOException
  {
    this.delete(new EIStorageName(name));
  }

  /**
   * Get an object if it exists.
   *
   * @param name The name
   *
   * @return The stored object, if one exists with the given name
   *
   * @throws IOException On errors
   */

  Optional<EIStored> get(
    EIStorageName name)
    throws IOException;

  /**
   * Get an object if it exists.
   *
   * @param name The name
   *
   * @return The stored object, if one exists with the given name
   *
   * @throws IOException On errors
   */

  default Optional<EIStored> get(
    final String name)
    throws IOException
  {
    return this.get(new EIStorageName(name));
  }
}
