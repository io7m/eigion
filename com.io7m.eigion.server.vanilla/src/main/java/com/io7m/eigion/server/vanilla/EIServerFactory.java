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


package com.io7m.eigion.server.vanilla;

import com.io7m.eigion.server.api.EIServerConfiguration;
import com.io7m.eigion.server.api.EIServerConfiguratorType;
import com.io7m.eigion.server.api.EIServerException;
import com.io7m.eigion.server.api.EIServerFactoryType;
import com.io7m.eigion.server.api.EIServerType;
import com.io7m.eigion.server.vanilla.internal.EIServer;
import com.io7m.eigion.server.vanilla.internal.EIServerConfigurator;

/**
 * The default server implementation.
 */

public final class EIServerFactory implements EIServerFactoryType
{
  /**
   * The default server implementation.
   */

  public EIServerFactory()
  {

  }

  @Override
  public EIServerType createServer(
    final EIServerConfiguration configuration)
  {
    return new EIServer(configuration);
  }

  @Override
  public EIServerConfiguratorType createServerConfigurator(
    final EIServerConfiguration configuration)
    throws EIServerException
  {
    return EIServerConfigurator.create(configuration);
  }
}
