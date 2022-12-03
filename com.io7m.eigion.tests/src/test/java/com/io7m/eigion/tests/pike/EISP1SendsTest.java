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


package com.io7m.eigion.tests.pike;

import com.io7m.eigion.protocol.pike.cb.EIPCB1Messages;
import com.io7m.eigion.server.pike_v1.EISP1Sends;
import com.io7m.eigion.tests.service.EIServiceContract;

public final class EISP1SendsTest
  extends EIServiceContract<EISP1Sends>
{
  @Override
  protected EISP1Sends createInstanceA()
  {
    return new EISP1Sends(new EIPCB1Messages());
  }

  @Override
  protected EISP1Sends createInstanceB()
  {
    return new EISP1Sends(new EIPCB1Messages());
  }
}
