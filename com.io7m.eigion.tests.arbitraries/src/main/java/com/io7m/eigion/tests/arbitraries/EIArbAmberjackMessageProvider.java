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

import com.io7m.eigion.error_codes.EIErrorCode;
import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.protocol.amberjack.EIAJCommandLogin;
import com.io7m.eigion.protocol.amberjack.EIAJMessageType;
import com.io7m.eigion.protocol.amberjack.EIAJResponseError;
import com.io7m.eigion.protocol.amberjack.EIAJResponseLogin;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.providers.TypeUsage;

import java.util.Set;
import java.util.UUID;

/**
 * A provider of {@link EIToken} values.
 */

public final class EIArbAmberjackMessageProvider extends EIArbAbstractProvider
{
  /**
   * A provider of values.
   */

  public EIArbAmberjackMessageProvider()
  {

  }

  @Override
  public boolean canProvideFor(
    final TypeUsage targetType)
  {
    return targetType.isOfType(EIAJMessageType.class);
  }

  @Override
  public Set<Arbitrary<?>> provideFor(
    final TypeUsage targetType,
    final SubtypeProvider subtypeProvider)
  {
    return Set.of(
      commandLogin(),
      responseLogin(),
      responseError()
    );
  }

  private static Arbitrary<EIAJResponseError> responseError()
  {
    return Combinators.combine(
      Arbitraries.defaultFor(UUID.class),
      Arbitraries.defaultFor(EIErrorCode.class),
      Arbitraries.strings()
    ).as(EIAJResponseError::new);
  }

  private static Arbitrary<EIAJResponseLogin> responseLogin()
  {
    return Combinators.combine(
      Arbitraries.defaultFor(UUID.class),
      Arbitraries.defaultFor(EIUser.class)
    ).as(EIAJResponseLogin::new);
  }

  private static Arbitrary<EIAJCommandLogin> commandLogin()
  {
    return Combinators.combine(
      Arbitraries.strings(),
      Arbitraries.strings()
    ).as(EIAJCommandLogin::new);
  }
}
