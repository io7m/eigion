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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
public abstract class EIWithServerContract
{
  @Container
  private final PostgreSQLContainer<?> container =
    new PostgreSQLContainer<>("postgres")
      .withDatabaseName("postgres")
      .withUsername("postgres")
      .withPassword("12345678");

  private EIFakeClock clock;
  private EITestIdstore idstore;
  private EITestServer server;

  protected final EIFakeClock clock()
  {
    return this.clock;
  }

  protected final EITestIdstore idstore()
  {
    return this.idstore;
  }

  protected final EITestServer server()
  {
    return this.server;
  }

  @BeforeEach
  public final void serverSetup()
    throws Exception
  {
    this.clock =
      new EIFakeClock();
    this.idstore =
      EITestIdstore.create(this.container, this.clock);
    this.idstore.idstore()
      .start();

    this.server =
      EITestServer.create(this.container, this.clock);
    this.server.server()
      .start();
  }

  @AfterEach
  public final void serverTearDown()
    throws Exception
  {
    this.idstore.close();
    this.server.close();
  }
}
