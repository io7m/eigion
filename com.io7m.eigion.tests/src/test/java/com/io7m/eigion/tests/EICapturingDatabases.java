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

import com.io7m.eigion.server.database.api.EIServerDatabaseConfiguration;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseFactoryType;
import com.io7m.eigion.server.database.api.EIServerDatabaseType;

import java.util.Objects;
import java.util.function.Consumer;

final class EICapturingDatabases
  implements EIServerDatabaseFactoryType
{
  private final EIServerDatabaseFactoryType delegate;
  private EIServerDatabaseType mostRecent;

  EICapturingDatabases(
    final EIServerDatabaseFactoryType inDelegate)
  {
    this.delegate =
      Objects.requireNonNull(inDelegate, "delegate");
  }

  @Override
  public EIServerDatabaseType open(
    final EIServerDatabaseConfiguration configuration,
    final Consumer<String> startupMessages)
    throws EIServerDatabaseException
  {
    final var database =
      this.delegate.open(configuration, startupMessages);
    this.mostRecent = database;
    return database;
  }

  public EIServerDatabaseType mostRecent()
  {
    return this.mostRecent;
  }
}
