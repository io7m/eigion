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


package com.io7m.eigion.tests.amberjack;

import com.io7m.eigion.protocol.amberjack.EIAJMessageType;
import com.io7m.eigion.protocol.amberjack.cb.EIAJCB1Messages;
import com.io7m.eigion.tests.service.EIServiceContract;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class EIAJ1CBMessagesTest
  extends EIServiceContract<EIAJCB1Messages>
{
  private static final EIAJCB1Messages MESSAGES =
    new EIAJCB1Messages();

  @Property(tries = 2000)
  public void testSerialization(
    final @ForAll EIAJMessageType message)
    throws Exception
  {
    final var data =
      MESSAGES.serialize(message);
    final var m =
      MESSAGES.parse(data);

    assertEquals(message, m);
  }

  @Override
  protected EIAJCB1Messages createInstanceA()
  {
    return new EIAJCB1Messages();
  }

  @Override
  protected EIAJCB1Messages createInstanceB()
  {
    return new EIAJCB1Messages();
  }
}