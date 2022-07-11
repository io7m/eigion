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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * An abstract provider implementation.
 */

public abstract class EIStorageAbstract implements EIStorageFactoryType
{
  protected final void checkParameters(
    final EIStorageParameters parameters)
    throws EIStorageConfigurationException
  {
    Objects.requireNonNull(parameters, "parameters");

    final var errors =
      new ArrayList<String>();

    final var definitions =
      this.parameters();

    final var requiredParameters =
      definitions.values()
        .stream()
        .filter(EIStorageParameterDescription::required)
        .toList();

    final var requiredParameterNames =
      definitions.values()
        .stream()
        .filter(EIStorageParameterDescription::required)
        .map(EIStorageParameterDescription::name)
        .toList();

    final var recognizedParameterNames =
      definitions.values()
        .stream()
        .map(EIStorageParameterDescription::name)
        .collect(Collectors.toSet());

    for (final var required : requiredParameters) {
      if (!definitions.containsKey(required.name())) {
        errors.add(
          "Missing a required parameter: %s".formatted(required.name())
        );
      }
    }

    for (final var specified : definitions.keySet()) {
      if (!recognizedParameterNames.contains(specified)) {
        errors.add(
          "Received unrecognized parameter: %s".formatted(specified)
        );
      }
    }

    if (!errors.isEmpty()) {
      throw new EIStorageConfigurationException(
        errors, "Errors encountered during storage creation."
      );
    }
  }

  @Override
  public final EIStorageType create(
    final EIStorageParameters parameters)
    throws EIStorageConfigurationException, IOException
  {
    Objects.requireNonNull(parameters, "parameters");
    this.checkParameters(parameters);
    return this.createActual(parameters);
  }

  protected abstract EIStorageType createActual(
    EIStorageParameters parameters)
    throws EIStorageConfigurationException, IOException;
}
