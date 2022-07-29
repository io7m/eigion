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

package com.io7m.eigion.server.vanilla.internal.admin_api;

import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandType;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.security.EISecurityException;
import com.io7m.eigion.server.vanilla.internal.EIHTTPErrorStatusException;

/**
 * The type of command executors.
 *
 * @param <C> The type of accepted commands
 */

public interface EIACommandExecutorType<C extends EISA1CommandType>
{
  /**
   * Execute a command.
   *
   * @param context The execution context
   * @param command The command
   *
   * @return The result of execution
   *
   * @throws EIServerDatabaseException On errors
   */

  EIACommandExecutionResult execute(
    EIACommandContext context,
    C command)
    throws
    EIServerDatabaseException,
    EIHTTPErrorStatusException,
    EISecurityException;
}
