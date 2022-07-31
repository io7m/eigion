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

package com.io7m.eigion.protocol.public_api.v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

/**
 * A response to a request to create a group.
 *
 * @param requestId The request ID
 * @param groupName The name of the group
 * @param token     The token
 * @param location  The location that will be checked for the token
 */

@JsonDeserialize
@JsonSerialize
public record EISP1ResponseGroupCreateBegin(
  @JsonProperty(value = "RequestID", required = true)
  UUID requestId,
  @JsonProperty(value = "Group", required = true)
  String groupName,
  @JsonProperty(value = "Token", required = true)
  String token,
  @JsonProperty(value = "Location", required = true)
  URI location)
  implements EISP1ResponseType
{
  /**
   * A response to a request to log in.
   *
   * @param requestId The request ID
   * @param groupName The name of the group
   * @param token     The token
   * @param location  The location that will be checked for the token
   */

  @JsonCreator
  public EISP1ResponseGroupCreateBegin
  {
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(groupName, "groupName");
    Objects.requireNonNull(token, "token");
    Objects.requireNonNull(location, "location");
  }
}
