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

import java.io.InputStream;
import java.util.Objects;

/**
 * A stored object.
 *
 * @param name        The name
 * @param contentType The content type
 * @param contentSize The content size
 * @param hash        A hash of the content
 * @param data        The data
 */

public record EIStored(
  EIStorageName name,
  String contentType,
  long contentSize,
  EIHash hash,
  InputStream data)
{
  /**
   * A stored object.
   *
   * @param name        The name
   * @param contentType The content type
   * @param contentSize The content size
   * @param hash        A hash of the content
   * @param data        The data
   */

  public EIStored
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(contentType, "contentType");
    Objects.requireNonNull(hash, "hash");
    Objects.requireNonNull(data, "data");
  }
}
