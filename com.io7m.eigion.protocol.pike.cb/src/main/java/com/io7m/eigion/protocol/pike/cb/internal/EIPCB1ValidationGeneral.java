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


package com.io7m.eigion.protocol.pike.cb.internal;

import com.io7m.cedarbridge.runtime.api.CBIntegerUnsigned32;
import com.io7m.cedarbridge.runtime.api.CBIntegerUnsigned64;
import com.io7m.cedarbridge.runtime.api.CBIntegerUnsigned8;
import com.io7m.cedarbridge.runtime.api.CBList;
import com.io7m.cedarbridge.runtime.api.CBSerializableType;
import com.io7m.eigion.model.EIPage;
import com.io7m.eigion.model.EITimeRange;
import com.io7m.eigion.protocol.pike.cb.EIP1Page;
import com.io7m.eigion.protocol.pike.cb.EIP1TimeRange;
import com.io7m.eigion.protocol.pike.cb.EIP1TimestampUTC;
import com.io7m.eigion.protocol.pike.cb.EIP1UUID;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.function.Function;

/**
 * Functions to translate between the core command set and the Admin v1
 * Cedarbridge encoding command set.
 */

public final class EIPCB1ValidationGeneral
{
  private EIPCB1ValidationGeneral()
  {

  }

  public static <A, B extends CBSerializableType> EIP1Page<B> toWirePage(
    final EIPage<A> page,
    final Function<A, B> f)
  {
    return new EIP1Page<>(
      new CBList<>(page.items().stream().map(f).toList()),
      new CBIntegerUnsigned32(Integer.toUnsignedLong(page.pageIndex())),
      new CBIntegerUnsigned32(Integer.toUnsignedLong(page.pageCount())),
      new CBIntegerUnsigned64(page.pageFirstOffset())
    );
  }

  public static EIP1TimestampUTC toWireTimestamp(
    final OffsetDateTime t)
  {
    return new EIP1TimestampUTC(
      new CBIntegerUnsigned32(Integer.toUnsignedLong(t.getYear())),
      new CBIntegerUnsigned8(t.getMonthValue()),
      new CBIntegerUnsigned8(t.getDayOfMonth()),
      new CBIntegerUnsigned8(t.getHour()),
      new CBIntegerUnsigned8(t.getMinute()),
      new CBIntegerUnsigned8(t.getSecond()),
      new CBIntegerUnsigned32(Integer.toUnsignedLong(t.getNano() / 1000))
    );
  }

  public static EIP1UUID toWireUUID(
    final UUID uuid)
  {
    return new EIP1UUID(
      new CBIntegerUnsigned64(uuid.getMostSignificantBits()),
      new CBIntegerUnsigned64(uuid.getLeastSignificantBits())
    );
  }

  public static EIP1TimeRange toWireTimeRange(
    final EITimeRange timeRange)
  {
    return new EIP1TimeRange(
      toWireTimestamp(timeRange.timeLower()),
      toWireTimestamp(timeRange.timeUpper())
    );
  }

  public static UUID fromWireUUID(
    final EIP1UUID uuid)
  {
    return new UUID(
      uuid.fieldMsb().value(),
      uuid.fieldLsb().value()
    );
  }

  public static <A extends CBSerializableType, B> EIPage<B> fromWirePage(
    final EIP1Page<A> page,
    final Function<A, B> f)
  {
    return new EIPage<>(
      page.fieldItems().values().stream().map(f).toList(),
      (int) page.fieldPageIndex().value(),
      (int) page.fieldPageCount().value(),
      page.fieldPageFirstOffset().value()
    );
  }

  public static EITimeRange fromWireTimeRange(
    final EIP1TimeRange t)
  {
    return new EITimeRange(
      fromWireTimestamp(t.fieldLower()),
      fromWireTimestamp(t.fieldUpper())
    );
  }

  public static OffsetDateTime fromWireTimestamp(
    final EIP1TimestampUTC t)
  {
    return OffsetDateTime.of(
      (int) (t.fieldYear().value() & 0xffffffffL),
      t.fieldMonth().value(),
      t.fieldDay().value(),
      t.fieldHour().value(),
      t.fieldMinute().value(),
      t.fieldSecond().value(),
      (int) (t.fieldMillisecond().value() * 1000L),
      ZoneOffset.UTC
    );
  }
}
