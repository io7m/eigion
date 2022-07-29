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

package com.io7m.eigion.protocol.admin_api.v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A response to a request to retrieve a set of admins.
 *
 * @param requestId The request ID
 * @param admins     The admins
 */

@JsonDeserialize
@JsonSerialize
public record EISA1ResponseAdminList(
  @JsonProperty(value = "RequestID", required = true)
  UUID requestId,
  @JsonProperty(value = "Admins", required = true)
  List<EISA1AdminSummary> admins)
  implements EISA1ResponseType
{
  /**
   * A response to a request to retrieve a set of admins.
   *
   * @param requestId The request ID
   * @param admins     The admins
   */

  @JsonCreator
  public EISA1ResponseAdminList
  {
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(admins, "admins");
  }
}