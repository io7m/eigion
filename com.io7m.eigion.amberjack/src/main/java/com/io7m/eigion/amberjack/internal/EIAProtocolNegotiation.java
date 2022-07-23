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


package com.io7m.eigion.amberjack.internal;

import com.io7m.eigion.amberjack.api.EIAClientException;
import com.io7m.eigion.protocol.api.EIProtocolException;
import com.io7m.eigion.protocol.versions.EISVMessageType;
import com.io7m.eigion.protocol.versions.EISVMessages;
import com.io7m.eigion.protocol.versions.EISVProtocols;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.net.http.HttpResponse.BodyHandlers.ofByteArray;
import static java.util.function.Function.identity;

/**
 * Functions to negotiate protocols.
 */

public final class EIAProtocolNegotiation
{
  private EIAProtocolNegotiation()
  {

  }

  private static EIAClientException noProtocolsInCommon(
    final Map<BigInteger, EIAClientProtocolHandlerFactoryType> handlers,
    final EIAStrings strings,
    final EISVProtocols protocols)
  {
    final var lineSeparator = System.lineSeparator();
    final var text = new StringBuilder(128);
    text.append(strings.format("noSupportedVersions"));
    text.append(lineSeparator);
    text.append("  ");
    text.append(strings.format("serverSupports"));
    text.append(lineSeparator);

    for (final var candidate : protocols.protocols()) {
      text.append("    ");
      text.append(candidate.id());
      text.append(" ");
      text.append(candidate.versionMajor());
      text.append(".");
      text.append(candidate.versionMinor());
      text.append(" ");
      text.append(candidate.endpointPath());
      text.append(lineSeparator);
    }

    text.append(strings.format("clientSupports"));
    text.append(lineSeparator);

    for (final var handler : handlers.values()) {
      text.append("    ");
      text.append(handler.id());
      text.append(" ");
      text.append(handler.versionMajor());
      text.append(".*");
      text.append(lineSeparator);
    }

    return new EIAClientException(text.toString());
  }

  private static EISVProtocols fetchSupportedVersions(
    final URI base,
    final HttpClient httpClient,
    final EIAStrings strings)
    throws InterruptedException, EIAClientException
  {
    final var vMessages =
      new EISVMessages();

    final var request =
      HttpRequest.newBuilder(base)
        .GET()
        .build();

    final HttpResponse<byte[]> response;
    try {
      response = httpClient.send(request, ofByteArray());
    } catch (final IOException e) {
      throw new EIAClientException(e);
    }

    if (response.statusCode() >= 400) {
      throw new EIAClientException(
        strings.format("httpError", Integer.valueOf(response.statusCode()))
      );
    }

    final EISVMessageType message;
    try {
      message = vMessages.parse(response.body());
    } catch (final EIProtocolException e) {
      throw new EIAClientException(e);
    }

    if (message instanceof EISVProtocols protocols) {
      return protocols;
    }

    throw new EIAClientException(
      strings.format(
        "unexpectedMessage", "EISVProtocols", message.getClass())
    );
  }

  /**
   * Negotiate a protocol handler.
   *
   * @param httpClient The HTTP client
   * @param strings    The string resources
   * @param user       The user
   * @param password   The password
   * @param base       The base URI
   *
   * @return The protocol handler
   *
   * @throws EIAClientException   On errors
   * @throws InterruptedException On interruption
   */

  public static EIAClientProtocolHandlerType negotiateProtocolHandler(
    final HttpClient httpClient,
    final EIAStrings strings,
    final String user,
    final String password,
    final URI base)
    throws EIAClientException, InterruptedException
  {
    Objects.requireNonNull(httpClient, "httpClient");
    Objects.requireNonNull(strings, "strings");
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(base, "base");

    final var handlerFactories =
      Stream.<EIAClientProtocolHandlerFactoryType>of(new EIAClientProtocolHandlers1())
        .collect(Collectors.toMap(
          EIAClientProtocolHandlerFactoryType::versionMajor,
          identity())
        );

    final var protocols =
      fetchSupportedVersions(base, httpClient, strings);

    final var candidates =
      protocols.protocols()
        .stream()
        .sorted(Comparator.reverseOrder())
        .toList();

    for (final var candidate : candidates) {
      final var handlerFactory =
        handlerFactories.get(candidate.versionMajor());

      if (handlerFactory != null) {
        final var target =
          base.resolve(candidate.endpointPath())
            .normalize();

        return handlerFactory.createHandler(
          httpClient,
          strings,
          target
        );
      }
    }

    throw noProtocolsInCommon(handlerFactories, strings, protocols);
  }
}
