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

package com.io7m.eigion.server.api;

import com.io7m.eigion.model.EIPermissionSet;
import com.io7m.eigion.server.database.api.EISDatabaseType;

import java.util.UUID;

/**
 * A server installer instance.
 */

public interface EIServerConfiguratorType extends AutoCloseable
{
  /**
   * @return The server's database instance
   */

  EISDatabaseType database();

  /**
   * Set permissions for the given user, creating it if it exists.
   *
   * @param userId      The user ID
   * @param permissions The permission set
   *
   * @throws EIServerException On errors
   */

  void userSetPermissions(
    UUID userId,
    EIPermissionSet permissions)
    throws EIServerException;

  @Override
  void close()
    throws EIServerException;
}
