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


package com.io7m.eigion.tests.domaincheck;

import com.io7m.eigion.domaincheck.api.EIDomainCheckerConfiguration;
import com.io7m.eigion.domaincheck.internal.EIDomainChecker;
import com.io7m.eigion.server.database.api.EISDatabaseType;
import com.io7m.eigion.server.service.domaincheck.EISDomainChecking;
import com.io7m.eigion.tests.EIFakeClock;
import com.io7m.eigion.tests.service.EIServiceContract;
import io.opentelemetry.api.OpenTelemetry;

import java.net.http.HttpClient;

import static org.mockito.Mockito.mock;

public final class EISDomainCheckingTest
  extends EIServiceContract<EISDomainChecking>
{
  @Override
  protected EISDomainChecking createInstanceA()
  {
    return new EISDomainChecking(
      mock(EISDatabaseType.class),
      EIDomainChecker.create(new EIDomainCheckerConfiguration(
        OpenTelemetry.noop(),
        new EIFakeClock(),
        HttpClient.newHttpClient()
      ))
    );
  }

  @Override
  protected EISDomainChecking createInstanceB()
  {
    return new EISDomainChecking(
      mock(EISDatabaseType.class),
      EIDomainChecker.create(new EIDomainCheckerConfiguration(
        OpenTelemetry.noop(),
        new EIFakeClock(),
        HttpClient.newHttpClient()
      ))
    );
  }
}
