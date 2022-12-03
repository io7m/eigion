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

import com.io7m.eigion.storage.api.EIStorageConfigurationException;
import com.io7m.eigion.storage.api.EIStorageFactoryType;
import com.io7m.eigion.storage.api.EIStorageParameterDescription;
import com.io7m.eigion.storage.api.EIStorageParameters;
import com.io7m.eigion.storage.api.EIStorageType;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;

public final class EIFakeStorageFactory implements EIStorageFactoryType
{
  private final LinkedList<EIFakeStorage> storages;

  public EIFakeStorageFactory()
  {
    this.storages = new LinkedList<>();
  }

  public LinkedList<EIFakeStorage> storages()
  {
    return this.storages;
  }

  @Override
  public String name()
  {
    return "fake";
  }

  @Override
  public String description()
  {
    return "Fake storage";
  }

  @Override
  public Map<String, EIStorageParameterDescription> parameters()
  {
    return Map.of();
  }

  @Override
  public EIStorageType create(
    final EIStorageParameters parameters)
    throws EIStorageConfigurationException, IOException
  {
    final var storage = new EIFakeStorage(parameters);
    this.storages.add(storage);
    return storage;
  }
}
