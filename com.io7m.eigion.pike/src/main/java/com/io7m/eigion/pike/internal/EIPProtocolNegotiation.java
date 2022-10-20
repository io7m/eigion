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


package com.io7m.eigion.pike.internal;

import com.io7m.eigion.pike.api.EIPClientException;
import com.io7m.eigion.protocol.pike.cb.EIPCB1Messages;
import com.io7m.genevan.core.GenProtocolException;
import com.io7m.genevan.core.GenProtocolIdentifier;
import com.io7m.genevan.core.GenProtocolServerEndpointType;
import com.io7m.genevan.core.GenProtocolSolved;
import com.io7m.genevan.core.GenProtocolSolver;
import com.io7m.genevan.core.GenProtocolVersion;
import com.io7m.verdant.core.VProtocolException;
import com.io7m.verdant.core.VProtocols;
import com.io7m.verdant.core.cb.VProtocolMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.HTTP_ERROR;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.IO_ERROR;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.NO_SUPPORTED_PROTOCOLS;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.eigion.pike.internal.EIPCompression.decompressResponse;
import static java.net.http.HttpResponse.BodyHandlers.ofByteArray;

/**
 * Functions to negotiate protocols.
 */

public final class EIPProtocolNegotiation
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIPProtocolNegotiation.class);

  private EIPProtocolNegotiation()
  {

  }

  private static List<IdAServerEndpoint> fetchSupportedVersions(
    final URI base,
    final HttpClient httpClient,
    final EIPStrings strings)
    throws InterruptedException, EIPClientException
  {
    LOG.debug("retrieving supported server protocols");

    final var request =
      HttpRequest.newBuilder(base)
        .GET()
        .build();

    final HttpResponse<byte[]> response;
    try {
      response = httpClient.send(request, ofByteArray());
    } catch (final IOException e) {
      throw new EIPClientException(IO_ERROR, e);
    }

    LOG.debug("server: status {}", response.statusCode());

    if (response.statusCode() >= 400) {
      throw new EIPClientException(
        HTTP_ERROR,
        strings.format("httpError", Integer.valueOf(response.statusCode()))
      );
    }

    final var protocols =
      VProtocolMessages.create();

    final VProtocols message;
    try {
      final var body = decompressResponse(response, response.headers());
      message = protocols.parse(base, body);
    } catch (final VProtocolException e) {
      throw new EIPClientException(PROTOCOL_ERROR, e);
    } catch (final IOException e) {
      throw new EIPClientException(IO_ERROR, e);
    }

    return message.protocols()
      .stream()
      .map(v -> {
        return new IdAServerEndpoint(
          new GenProtocolIdentifier(
            v.id().toString(),
            new GenProtocolVersion(
              new BigInteger(Long.toUnsignedString(v.versionMajor())),
              new BigInteger(Long.toUnsignedString(v.versionMinor()))
            )
          ),
          v.endpointPath()
        );
      }).toList();
  }

  private record IdAServerEndpoint(
    GenProtocolIdentifier supported,
    String endpoint)
    implements GenProtocolServerEndpointType
  {
    IdAServerEndpoint
    {
      Objects.requireNonNull(supported, "supported");
      Objects.requireNonNull(endpoint, "endpoint");
    }
  }

  /**
   * Negotiate a protocol handler.
   *
   * @param locale     The locale
   * @param httpClient The HTTP client
   * @param strings    The string resources
   * @param base       The base URI
   *
   * @return The protocol handler
   *
   * @throws EIPClientException   On errors
   * @throws InterruptedException On interruption
   */

  public static EIPClientProtocolHandlerType negotiateProtocolHandler(
    final Locale locale,
    final HttpClient httpClient,
    final EIPStrings strings,
    final URI base)
    throws EIPClientException, InterruptedException
  {
    Objects.requireNonNull(locale, "locale");
    Objects.requireNonNull(httpClient, "httpClient");
    Objects.requireNonNull(strings, "strings");
    Objects.requireNonNull(base, "base");

    final var clientSupports =
      List.of(
        new EIPClientProtocolHandlers1()
      );

    final var serverProtocols =
      fetchSupportedVersions(base, httpClient, strings);

    LOG.debug("server supports {} protocols", serverProtocols.size());

    final var solver =
      GenProtocolSolver.<EIPClientProtocolHandlerFactoryType, IdAServerEndpoint>create(
        locale);

    final GenProtocolSolved<EIPClientProtocolHandlerFactoryType, IdAServerEndpoint> solved;
    try {
      solved = solver.solve(
        serverProtocols,
        clientSupports,
        List.of(EIPCB1Messages.protocolId().toString())
      );
    } catch (final GenProtocolException e) {
      throw new EIPClientException(NO_SUPPORTED_PROTOCOLS, e.getMessage(), e);
    }

    final var serverEndpoint =
      solved.serverEndpoint();
    final var target =
      base.resolve(serverEndpoint.endpoint())
        .normalize();

    final var protocol = serverEndpoint.supported();
    LOG.debug(
      "using protocol {} {}.{} at endpoint {}",
      protocol.identifier(),
      protocol.version().versionMajor(),
      protocol.version().versionMinor(),
      target
    );

    return solved.clientHandler().createHandler(
      httpClient,
      strings,
      target
    );
  }
}
