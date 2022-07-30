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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A request to create a group.
 *
 * @param groupName   The group name
 * @param userFounder The user that founded the group
 * @param token       The request token
 * @param status      The request status
 */

public record EIGroupCreationRequest(
  EIGroupName groupName,
  UUID userFounder,
  EIToken token,
  Optional<EIGroupCreationRequestStatusType> status)
{
  /**
   * A request to create a group.
   *
   * @param groupName   The group name
   * @param userFounder The user that founded the group
   * @param token       The request token
   * @param status      The request status
   */

  public EIGroupCreationRequest
  {
    Objects.requireNonNull(groupName, "groupName");
    Objects.requireNonNull(userFounder, "userFounder");
    Objects.requireNonNull(token, "token");
    Objects.requireNonNull(status, "status");
  }

  /**
   * @return The set of verification URIs in preference order
   */

  public List<URI> verificationURIs()
  {
    final var groupSegments =
      Arrays.stream(this.groupName.value().split("\\."))
        .collect(Collectors.toList());

    Collections.reverse(groupSegments);

    final var hostName =
      String.join(".", groupSegments);

    return Stream.of("https", "http")
      .map(proto -> this.formattedURI(hostName, proto))
      .map(URI::create).toList();
  }

  private String formattedURI(
    final String hostName,
    final String proto)
  {
    return "%s://%s/.well-known/eigion-group-challenge/%s".formatted(
      proto,
      hostName,
      this.token
    );
  }
}

