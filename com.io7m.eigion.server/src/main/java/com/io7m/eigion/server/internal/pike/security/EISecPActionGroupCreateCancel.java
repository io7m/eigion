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

package com.io7m.eigion.server.internal.pike.security;

import com.io7m.eigion.model.EIGroupCreationRequest;
import com.io7m.eigion.model.EIUser;

import java.util.Objects;

/**
 * A user wants to tell the server that a group creation should be cancelled.
 *
 * @param user    The user
 * @param request The request
 */

public record EISecPActionGroupCreateCancel(
  EIUser user,
  EIGroupCreationRequest request)
  implements EISecPActionType
{
  /**
   * A user wants to tell the server that a group creation should be cancelled.
   *
   * @param user    The user
   * @param request The request
   */

  public EISecPActionGroupCreateCancel
  {
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(request, "request");
  }
}
