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
import com.io7m.eigion.model.EIUserDisplayName;
import com.io7m.eigion.model.EIUserEmail;
import com.io7m.eigion.protocol.admin_api.v1.EISA1GroupRole;
import com.io7m.eigion.protocol.admin_api.v1.EISA1Password;
import com.io7m.eigion.protocol.admin_api.v1.EISA1User;
import com.io7m.eigion.protocol.admin_api.v1.EISA1UserBan;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.providers.TypeUsage;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A provider of {@link EISA1User} values.
 */

public final class EIArbEISA1UserProvider extends EIArbAbstractProvider
{
  /**
   * A provider of values.
   */

  public EIArbEISA1UserProvider()
  {

  }

  @Override
  public boolean canProvideFor(
    final TypeUsage targetType)
  {
    return targetType.isOfType(EISA1User.class);
  }

  private static <K0, K1, V> Map<K1, Set<V>> convert(
    final Map<K0, Set<V>> s,
    final Function<K0, K1> f)
  {
    return s.entrySet()
      .stream()
      .map(e -> Map.entry(f.apply(e.getKey()), e.getValue()))
      .collect(Collectors.toUnmodifiableMap(
        Map.Entry::getKey,
        Map.Entry::getValue));
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
      Arbitraries.defaultFor(EISA1Password.class);
    final var ub =
      Arbitraries.defaultFor(Optional.class, EISA1UserBan.class);
    final var g =
      Arbitraries.defaultFor(
        TypeUsage.of(
          Map.class,
          TypeUsage.of(EIGroupName.class),
          TypeUsage.of(Set.class, TypeUsage.of(EISA1GroupRole.class))
        )
      );

    final Arbitrary<Map<String, Set<EISA1GroupRole>>> g2 =
      g.map(m -> {
        final var mm = (Map<EIGroupName, Set<EISA1GroupRole>>) m;
        return convert(mm, EIGroupName::value);
      });

    final Arbitrary<EISA1User> a =
      Combinators.combine(u, d, e, t, t, p, ub, g2)
        .as((
              UUID id,
              EIUserDisplayName name,
              EIUserEmail email,
              OffsetDateTime t0,
              OffsetDateTime t1,
              EISA1Password pass,
              Optional ban,
              Map<String, Set<EISA1GroupRole>> groups) -> {
          return new EISA1User(
            id,
            name.value(),
            email.value(),
            t0,
            t1,
            pass,
            (Optional<EISA1UserBan>) ban,
            groups
          );
        });

    return Set.of(a);
  }
}
