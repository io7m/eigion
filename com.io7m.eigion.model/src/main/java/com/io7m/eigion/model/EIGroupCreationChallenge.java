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


package com.io7m.eigion.model;

import java.net.URI;
import java.util.Objects;

/**
 * A challenge that must be completed in order to create a group.
 *
 * @param groupName The group name
 * @param token     The token
 * @param location  The location for the token
 */

public record EIGroupCreationChallenge(
  EIGroupName groupName,
  EIToken token,
  URI location)
{
  /**
   * A challenge that must be completed in order to create a group.
   *
   * @param groupName The group name
   * @param token     The token
   * @param location  The location for the token
   */

  public EIGroupCreationChallenge
  {
    Objects.requireNonNull(groupName, "groupName");
    Objects.requireNonNull(token, "token");
    Objects.requireNonNull(location, "location");
  }
}
