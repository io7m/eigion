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


package com.io7m.eigion.amberjack.api;

import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIGroupSearchByNameParameters;

/**
 * Commands to manipulate groups.
 */

public interface EIAJClientGroupsType
{
  /**
   * Create a new group.
   *
   * @param name The group name
   *
   * @throws EIAJClientException On errors
   * @throws InterruptedException On interruption
   */

  void groupCreate(EIGroupName name)
    throws EIAJClientException, InterruptedException;

  /**
   * Start searching groups by name.
   *
   * @param parameters The parameters
   *
   * @return The paged query
   *
   * @throws EIAJClientException  On errors
   * @throws InterruptedException On interruption
   */

  EIAJClientPagedType<EIGroupName> groupSearchByName(
    EIGroupSearchByNameParameters parameters)
    throws EIAJClientException, InterruptedException;
}
