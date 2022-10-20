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
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.protocol.pike.EIPCommandLogin;
import com.io7m.eigion.protocol.pike.EIPMessageType;
import com.io7m.eigion.protocol.pike.EIPResponseError;
import com.io7m.eigion.protocol.pike.EIPResponseLogin;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.providers.TypeUsage;

import java.util.Set;
import java.util.UUID;

/**
 * A provider of {@link EIPMessageType} values.
 */

public final class EIArbPikeMessageProvider extends EIArbAbstractProvider
{
  /**
   * A provider of values.
   */

  public EIArbPikeMessageProvider()
  {

  }

  @Override
  public boolean canProvideFor(
    final TypeUsage targetType)
  {
    return targetType.isOfType(EIPMessageType.class);
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

  private static Arbitrary<EIPResponseError> responseError()
  {
    return Combinators.combine(
      Arbitraries.defaultFor(UUID.class),
      Arbitraries.defaultFor(EIErrorCode.class),
      Arbitraries.strings()
    ).as(EIPResponseError::new);
  }

  private static Arbitrary<EIPResponseLogin> responseLogin()
  {
    return Combinators.combine(
      Arbitraries.defaultFor(UUID.class),
      Arbitraries.defaultFor(EIUser.class)
    ).as(EIPResponseLogin::new);
  }

  private static Arbitrary<EIPCommandLogin> commandLogin()
  {
    return Combinators.combine(
      Arbitraries.strings(),
      Arbitraries.strings()
    ).as(EIPCommandLogin::new);
  }
}
