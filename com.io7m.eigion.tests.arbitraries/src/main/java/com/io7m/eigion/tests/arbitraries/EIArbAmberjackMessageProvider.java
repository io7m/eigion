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
import com.io7m.eigion.model.EIAuditEvent;
import com.io7m.eigion.model.EIAuditSearchParameters;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIPage;
import com.io7m.eigion.model.EITimeRange;
import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.protocol.amberjack.EIAJCommandAuditSearchBegin;
import com.io7m.eigion.protocol.amberjack.EIAJCommandAuditSearchNext;
import com.io7m.eigion.protocol.amberjack.EIAJCommandAuditSearchPrevious;
import com.io7m.eigion.protocol.amberjack.EIAJCommandGroupCreate;
import com.io7m.eigion.protocol.amberjack.EIAJCommandLogin;
import com.io7m.eigion.protocol.amberjack.EIAJMessageType;
import com.io7m.eigion.protocol.amberjack.EIAJResponseAuditSearch;
import com.io7m.eigion.protocol.amberjack.EIAJResponseError;
import com.io7m.eigion.protocol.amberjack.EIAJResponseGroupCreate;
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
      commandGroupCreate(),
      commandAuditSearchBegin(),
      commandAuditSearchNext(),
      commandAuditSearchPrevious(),
      responseLogin(),
      responseError(),
      responseGroupCreate(),
      responseAuditSearch()
    );
  }

  private static Arbitrary<EIAJResponseAuditSearch> responseAuditSearch()
  {
    final var events =
      Arbitraries.defaultFor(EIAuditEvent.class)
        .list();

    return Combinators.combine(
      Arbitraries.defaultFor(UUID.class),
      events,
      Arbitraries.integers().between(0, 100),
      Arbitraries.integers().between(0, 100),
      Arbitraries.integers().between(0, 100)
    ).as((uuid, aEvents, index, count, offset) -> {
      return new EIAJResponseAuditSearch(uuid, new EIPage<>(
        aEvents,
        index.intValue(),
        count.intValue(),
        offset.longValue()
      ));
    });
  }

  private static Arbitrary<EIAJCommandAuditSearchNext> commandAuditSearchNext()
  {
    return Arbitraries.of(new EIAJCommandAuditSearchNext());
  }

  private static Arbitrary<EIAJCommandAuditSearchPrevious> commandAuditSearchPrevious()
  {
    return Arbitraries.of(new EIAJCommandAuditSearchPrevious());
  }

  private static Arbitrary<EIAJCommandAuditSearchBegin> commandAuditSearchBegin()
  {
    final var parameters =
      Combinators.combine(
        Arbitraries.defaultFor(EITimeRange.class),
        Arbitraries.strings().optional(),
        Arbitraries.strings().optional(),
        Arbitraries.strings().optional(),
        Arbitraries.longs().between(1L, 999L)
      ).as(EIAuditSearchParameters::new);

    return parameters.map(EIAJCommandAuditSearchBegin::new);
  }

  private static Arbitrary<EIAJResponseGroupCreate> responseGroupCreate()
  {
    return Arbitraries.defaultFor(UUID.class)
      .map(EIAJResponseGroupCreate::new);
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

  private static Arbitrary<EIAJCommandGroupCreate> commandGroupCreate()
  {
    return Arbitraries.defaultFor(EIGroupName.class)
      .map(EIAJCommandGroupCreate::new);
  }
}
