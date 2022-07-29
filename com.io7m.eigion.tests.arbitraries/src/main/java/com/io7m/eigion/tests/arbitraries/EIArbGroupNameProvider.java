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

package com.io7m.eigion.tests.arbitraries;

import com.io7m.eigion.model.EIGroupName;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.providers.TypeUsage;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * A provider of {@link EIGroupName} values.
 */

public final class EIArbGroupNameProvider extends EIArbAbstractProvider
{
  /**
   * A provider of values.
   */

  public EIArbGroupNameProvider()
  {

  }

  @Override
  public boolean canProvideFor(
    final TypeUsage targetType)
  {
    return targetType.isOfType(EIGroupName.class);
  }

  @Override
  public Set<Arbitrary<?>> provideFor(
    final TypeUsage targetType,
    final SubtypeProvider subtypeProvider)
  {
    final var chars =
      Arbitraries.chars()
        .filter(EIArbGroupNameProvider::isSafeCharacter)
        .list()
        .ofMinSize(1)
        .ofMaxSize(62);

    final var s =
      chars.map(chs -> chs.stream()
        .map(Object::toString)
        .collect(Collectors.joining()))
        .map(xs -> 'a' + xs);

    final var segments =
      s.list()
        .ofMinSize(1)
        .ofMaxSize(4);

    final var names =
      segments
        .map(strings -> String.join(".", strings))
        .map(EIGroupName::new);

    return Set.of(names);
  }

  private static boolean isSafeCharacter(
    final Character x)
  {
    if (Character.isAlphabetic(x)) {
      return true;
    }
    if (Character.isDigit(x)) {
      return true;
    }
    return switch (x) {
      case '_' -> true;
      case '-' -> true;
      default -> false;
    };
  }
}
