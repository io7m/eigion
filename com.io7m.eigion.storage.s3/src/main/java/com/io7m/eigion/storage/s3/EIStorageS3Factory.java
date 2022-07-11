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


package com.io7m.eigion.storage.s3;

import com.io7m.eigion.storage.api.EIStorageAbstract;
import com.io7m.eigion.storage.api.EIStorageParameterDescription;
import com.io7m.eigion.storage.api.EIStorageParameters;
import com.io7m.eigion.storage.api.EIStorageType;
import com.io7m.eigion.storage.s3.internal.EIStorageS3;

import java.util.Map;

/**
 * A storage implementation using S3-compatible storage.
 */

public final class EIStorageS3Factory extends EIStorageAbstract
{
  /**
   * A storage implementation using S3-compatible storage.
   */

  public EIStorageS3Factory()
  {

  }

  @Override
  protected EIStorageType createActual(
    final EIStorageParameters parameters)
  {
    return new EIStorageS3();
  }

  @Override
  public String name()
  {
    return "s3";
  }

  @Override
  public String description()
  {
    return "S3-based storage implementation";
  }

  @Override
  public Map<String, EIStorageParameterDescription> parameters()
  {
    return Map.ofEntries(

    );
  }
}
