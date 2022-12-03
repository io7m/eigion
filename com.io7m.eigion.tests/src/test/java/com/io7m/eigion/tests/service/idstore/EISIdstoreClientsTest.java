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


package com.io7m.eigion.tests.service.idstore;

import com.io7m.eigion.server.api.EIServerIdstoreConfiguration;
import com.io7m.eigion.server.service.idstore.EISIdstoreClients;
import com.io7m.eigion.server.service.idstore.EISIdstoreClientsType;
import com.io7m.eigion.tests.service.EIServiceContract;

import java.net.URI;
import java.util.Locale;

public final class EISIdstoreClientsTest
  extends EIServiceContract<EISIdstoreClientsType>
{
  @Override
  protected EISIdstoreClientsType createInstanceA()
  {
    return EISIdstoreClients.create(
      Locale.ROOT,
      new EIServerIdstoreConfiguration(
        URI.create("urn:x"),
        URI.create("urn:y"))
    );
  }

  @Override
  protected EISIdstoreClientsType createInstanceB()
  {
    return EISIdstoreClients.create(
      Locale.ROOT,
      new EIServerIdstoreConfiguration(
        URI.create("urn:x"),
        URI.create("urn:y"))
    );
  }
}
