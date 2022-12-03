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

package com.io7m.eigion.tests.server.controller.pike;

import com.io7m.eigion.model.EIPermissionSet;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.server.controller.EISStrings;
import com.io7m.eigion.server.controller.amberjack.EISAJCommandContext;
import com.io7m.eigion.server.controller.pike.EISPCommandContext;
import com.io7m.eigion.server.database.api.EISDatabaseTransactionType;
import com.io7m.eigion.server.service.clock.EISClock;
import com.io7m.eigion.server.service.domaincheck.EISDomainCheckingType;
import com.io7m.eigion.server.service.sessions.EISession;
import com.io7m.eigion.server.service.sessions.EISessionSecretIdentifier;
import com.io7m.eigion.server.service.telemetry.api.EISTelemetryNoOp;
import com.io7m.eigion.server.service.telemetry.api.EISTelemetryServiceType;
import com.io7m.eigion.services.api.EIServiceDirectory;
import com.io7m.eigion.tests.EIFakeClock;
import com.io7m.idstore.user_client.api.IdUClientType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.internal.verification.Times;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

import static org.mockito.Mockito.mock;

public abstract class EISPCmdAbstractContract
{
  private EIServiceDirectory services;
  private EISDatabaseTransactionType transaction;
  private EIFakeClock clock;
  private EISClock serverClock;
  private EISStrings strings;
  private OffsetDateTime timeStart;
  private EISDomainCheckingType domainChecking;

  protected final EISDomainCheckingType domainChecking()
  {
    return this.domainChecking;
  }

  protected final Times once()
  {
    return new Times(1);
  }

  protected final Times twice()
  {
    return new Times(2);
  }

  @BeforeEach
  protected final void commandSetup()
    throws Exception
  {
    this.services =
      new EIServiceDirectory();
    this.transaction =
      mock(EISDatabaseTransactionType.class);

    this.clock =
      new EIFakeClock();
    this.serverClock =
      new EISClock(this.clock);
    this.timeStart =
      this.serverClock.now();
    this.strings =
      new EISStrings(Locale.ROOT);
    this.domainChecking =
      mock(EISDomainCheckingType.class);

    this.services.register(EISDomainCheckingType.class, this.domainChecking);
    this.services.register(EISClock.class, this.serverClock);
    this.services.register(EISStrings.class, this.strings);
    this.services.register(
      EISTelemetryServiceType.class,
      EISTelemetryNoOp.noop());
  }

  protected final OffsetDateTime timeStart()
  {
    return this.timeStart;
  }

  @AfterEach
  protected final void commandTearDown()
    throws Exception
  {
    this.services.close();
  }

  protected final EIServiceDirectory services()
  {
    return this.services;
  }

  protected final EISDatabaseTransactionType transaction()
  {
    return this.transaction;
  }

  protected final EIUser createUser(
    final EIPermissionSet permissions)
  {
    return new EIUser(
      UUID.randomUUID(),
      permissions
    );
  }

  protected final EISPCommandContext createContextAndSession(
    final EIUser user)
  {
    final var session =
      new EISession(
        user,
        EISessionSecretIdentifier.generate(),
        mock(IdUClientType.class)
      );

    return new EISPCommandContext(
      this.services,
      UUID.randomUUID(),
      this.transaction,
      session,
      "127.0.0.1",
      "NCSA Mosaic 0.1"
    );
  }
}
