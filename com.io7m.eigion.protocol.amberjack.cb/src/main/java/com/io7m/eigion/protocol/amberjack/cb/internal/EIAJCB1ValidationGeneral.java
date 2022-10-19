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


package com.io7m.eigion.protocol.amberjack.cb.internal;

import com.io7m.cedarbridge.runtime.api.CBIntegerUnsigned32;
import com.io7m.cedarbridge.runtime.api.CBIntegerUnsigned64;
import com.io7m.cedarbridge.runtime.api.CBIntegerUnsigned8;
import com.io7m.cedarbridge.runtime.api.CBList;
import com.io7m.cedarbridge.runtime.api.CBSerializableType;
import com.io7m.cedarbridge.runtime.api.CBString;
import com.io7m.eigion.model.EIAuditEvent;
import com.io7m.eigion.model.EIPage;
import com.io7m.eigion.model.EITimeRange;
import com.io7m.eigion.protocol.amberjack.cb.EIAJ1AuditEvent;
import com.io7m.eigion.protocol.amberjack.cb.EIAJ1Page;
import com.io7m.eigion.protocol.amberjack.cb.EIAJ1TimeRange;
import com.io7m.eigion.protocol.amberjack.cb.EIAJ1TimestampUTC;
import com.io7m.eigion.protocol.amberjack.cb.EIAJ1UUID;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.function.Function;

/**
 * Functions to translate between the core command set and the Admin v1
 * Cedarbridge encoding command set.
 */

public final class EIAJCB1ValidationGeneral
{
  private EIAJCB1ValidationGeneral()
  {

  }

  public static <A, B extends CBSerializableType> EIAJ1Page<B> toWirePage(
    final EIPage<A> page,
    final Function<A, B> f)
  {
    return new EIAJ1Page<>(
      new CBList<>(page.items().stream().map(f).toList()),
      new CBIntegerUnsigned32(Integer.toUnsignedLong(page.pageIndex())),
      new CBIntegerUnsigned32(Integer.toUnsignedLong(page.pageCount())),
      new CBIntegerUnsigned64(page.pageFirstOffset())
    );
  }

  public static EIAJ1TimestampUTC toWireTimestamp(
    final OffsetDateTime t)
  {
    return new EIAJ1TimestampUTC(
      new CBIntegerUnsigned32(Integer.toUnsignedLong(t.getYear())),
      new CBIntegerUnsigned8(t.getMonthValue()),
      new CBIntegerUnsigned8(t.getDayOfMonth()),
      new CBIntegerUnsigned8(t.getHour()),
      new CBIntegerUnsigned8(t.getMinute()),
      new CBIntegerUnsigned8(t.getSecond()),
      new CBIntegerUnsigned32(Integer.toUnsignedLong(t.getNano() / 1000))
    );
  }

  public static EIAJ1UUID toWireUUID(
    final UUID uuid)
  {
    return new EIAJ1UUID(
      new CBIntegerUnsigned64(uuid.getMostSignificantBits()),
      new CBIntegerUnsigned64(uuid.getLeastSignificantBits())
    );
  }

  public static EIAJ1TimeRange toWireTimeRange(
    final EITimeRange timeRange)
  {
    return new EIAJ1TimeRange(
      toWireTimestamp(timeRange.timeLower()),
      toWireTimestamp(timeRange.timeUpper())
    );
  }

  public static UUID fromWireUUID(
    final EIAJ1UUID uuid)
  {
    return new UUID(
      uuid.fieldMsb().value(),
      uuid.fieldLsb().value()
    );
  }

  public static <A extends CBSerializableType, B> EIPage<B> fromWirePage(
    final EIAJ1Page<A> page,
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
    final EIAJ1TimeRange t)
  {
    return new EITimeRange(
      fromWireTimestamp(t.fieldLower()),
      fromWireTimestamp(t.fieldUpper())
    );
  }

  public static OffsetDateTime fromWireTimestamp(
    final EIAJ1TimestampUTC t)
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

  public static EIAJ1AuditEvent toWireAuditEvent(
    final EIAuditEvent e)
  {
    return new EIAJ1AuditEvent(
      new CBIntegerUnsigned64(e.id()),
      toWireUUID(e.owner()),
      toWireTimestamp(e.time()),
      new CBString(e.type()),
      new CBString(e.message())
    );
  }

  public static EIAuditEvent fromWireAuditEvent(
    final EIAJ1AuditEvent i)
  {
    return new EIAuditEvent(
      i.fieldId().value(),
      fromWireUUID(i.fieldOwner()),
      fromWireTimestamp(i.fieldTime()),
      i.fieldType().value(),
      i.fieldMessage().value()
    );
  }
}
