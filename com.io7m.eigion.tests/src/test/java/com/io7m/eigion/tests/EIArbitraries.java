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

import com.io7m.eigion.model.EIPassword;
import com.io7m.eigion.model.EIPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.eigion.model.EIPasswordAlgorithmType;
import com.io7m.eigion.model.EISubsetMatch;
import com.io7m.eigion.protocol.admin_api.v1.EISA1AuditEvent;
import com.io7m.eigion.protocol.admin_api.v1.EISA1Password;
import com.io7m.eigion.protocol.admin_api.v1.EISA1SubsetMatch;
import com.io7m.eigion.protocol.admin_api.v1.EISA1User;
import com.io7m.eigion.protocol.admin_api.v1.EISA1UserBan;
import com.io7m.eigion.protocol.admin_api.v1.EISA1UserSummary;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.Provide;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

public final class EIArbitraries
{
  private EIArbitraries()
  {

  }

  public static <T> Arbitrary<EISA1SubsetMatch<T>> subsetMatchesV1(
    final Arbitrary<T> values)
  {
    return values.tuple2().map(k -> {
      return new EISA1SubsetMatch<>(k.get1(), k.get2());
    });
  }

  public static <T> Arbitrary<EISubsetMatch<T>> subsetMatches(
    final Arbitrary<T> values)
  {
    return values.tuple2().map(k -> {
      return new EISubsetMatch<>(k.get1(), k.get2());
    });
  }

  public static Arbitrary<UUID> uuids()
  {
    return Arbitraries.create(UUID::randomUUID);
  }

  public static Arbitrary<OffsetDateTime> offsetDateTimes()
  {
    return Arbitraries.longs().map(t -> {
      final var i = Instant.ofEpochMilli(t);
      return OffsetDateTime.ofInstant(i, ZoneId.of("UTC"));
    });
  }

  public static Arbitrary<EIPassword> passwords()
  {
    final var hashes =
      Arbitraries.strings()
        .withChars("ABCDEF0123456789")
        .ofMinLength(1)
        .ofMaxLength(256);

    final var algo =
      EIPasswordAlgorithmPBKDF2HmacSHA256.create();

    return Combinators.combine(hashes, hashes)
      .as((h, s) -> new EIPassword(algo, h, s));
  }

  public static Arbitrary<EISA1AuditEvent> auditEventsV1()
  {
    final var ids =
      Arbitraries.longs();
    final var uuids =
      uuids();
    final var times =
      offsetDateTimes();
    final var text =
      Arbitraries.strings();

    return Combinators.combine(ids, uuids, times, text, text)
      .as(EISA1AuditEvent::new);
  }

  public static Arbitrary<EISA1Password> passwordsV1()
  {
    final var algo =
      EIPasswordAlgorithmPBKDF2HmacSHA256.create();

    final var hashes =
      Arbitraries.strings()
        .withChars("ABCDEF0123456789")
        .ofMinLength(1)
        .ofMaxLength(256);

    return Combinators.combine(hashes, hashes).as((h, s) -> {
      return new EISA1Password(algo.identifier(), h, s);
    });
  }

  public static Arbitrary<EISA1UserBan> userBanV1()
  {
    return Combinators.combine(offsetDateTimes(), Arbitraries.strings())
      .as((t, s) -> new EISA1UserBan(Optional.of(t), s));
  }

  public static Arbitrary<EISA1User> userV1()
  {
    final var uuids =
      uuids();
    final var times =
      offsetDateTimes();
    final var text =
      Arbitraries.strings();

    return Combinators.combine(uuids, text, text, times, times, passwordsV1(), userBanV1())
      .as((id, name, email, created, login, password, ban) -> {
      return new EISA1User(id, name, email, created, login, password, Optional.of(ban));
    });
  }

  public static Arbitrary<EISA1UserSummary> userSummaryV1()
  {
    final var uuids =
      uuids();
    final var text =
      Arbitraries.strings();

    return Combinators.combine(uuids, text, text)
      .as(EISA1UserSummary::new);
  }
}
