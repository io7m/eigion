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

import com.io7m.eigion.model.EIGroupCreationRequest;
import com.io7m.eigion.model.EIGroupCreationRequestStatusType;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIToken;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.providers.TypeUsage;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A provider of {@link EIGroupCreationRequest} values.
 */

public final class EIArbGroupCreationRequestProvider
  extends EIArbAbstractProvider
{
  /**
   * A provider of values.
   */

  public EIArbGroupCreationRequestProvider()
  {

  }

  @Override
  public boolean canProvideFor(
    final TypeUsage targetType)
  {
    return targetType.isOfType(EIGroupCreationRequest.class);
  }

  @Override
  public Set<Arbitrary<?>> provideFor(
    final TypeUsage targetType,
    final SubtypeProvider subtypeProvider)
  {
    final var u =
      Arbitraries.defaultFor(UUID.class);
    final var g =
      Arbitraries.defaultFor(EIGroupName.class);
    final var t =
      Arbitraries.defaultFor(EIToken.class);
    final var s =
      Arbitraries.defaultFor(
        Optional.class,
        EIGroupCreationRequestStatusType.class);

    return Set.of(Combinators.combine(g, u, t, s).as((ug, uu, ut, us) -> {
      return new EIGroupCreationRequest(
        ug,
        uu,
        ut,
        (Optional<EIGroupCreationRequestStatusType>) us
      );
    }));
  }
}
