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
import com.io7m.eigion.model.EIGroupRole;
import com.io7m.eigion.model.EIPassword;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.model.EIUserBan;
import com.io7m.eigion.model.EIUserDisplayName;
import com.io7m.eigion.model.EIUserEmail;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.providers.TypeUsage;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A provider of {@link EIUser} values.
 */

public final class EIArbUserProvider extends EIArbAbstractProvider
{
  /**
   * A provider of values.
   */

  public EIArbUserProvider()
  {

  }

  @Override
  public boolean canProvideFor(
    final TypeUsage targetType)
  {
    return targetType.isOfType(EIUser.class);
  }

  @Override
  public Set<Arbitrary<?>> provideFor(
    final TypeUsage targetType,
    final SubtypeProvider subtypeProvider)
  {
    final var u =
      Arbitraries.defaultFor(UUID.class);
    final var d =
      Arbitraries.defaultFor(EIUserDisplayName.class);
    final var e =
      Arbitraries.defaultFor(EIUserEmail.class);
    final var t =
      Arbitraries.defaultFor(OffsetDateTime.class);
    final var p =
      Arbitraries.defaultFor(EIPassword.class);
    final var ub =
      Arbitraries.defaultFor(Optional.class, EIUserBan.class);
    final var g =
      Arbitraries.defaultFor(
        TypeUsage.of(
          Map.class,
          TypeUsage.of(EIGroupName.class),
          TypeUsage.of(Set.class, TypeUsage.of(EIGroupRole.class))
        )
      );

    final Arbitrary<EIUser> a =
      Combinators.combine(u, d, e, t, t, p, ub, g)
        .as((
              UUID id,
              EIUserDisplayName name,
              EIUserEmail email,
              OffsetDateTime t0,
              OffsetDateTime t1,
              EIPassword pass,
              Optional ban,
              Object groups) -> {
          return new EIUser(
            id,
            name,
            email,
            t0,
            t1,
            pass,
            (Optional<EIUserBan>) ban,
            (Map<EIGroupName, Set<EIGroupRole>>) groups
          );
        });

    return Set.of(a);
  }
}