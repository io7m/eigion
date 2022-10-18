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

import com.io7m.eigion.amberjack.api.EIAJClientException;
import com.io7m.eigion.protocol.amberjack.cb.EIAJCB1Messages;
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

import static com.io7m.eigion.amberjack.internal.EIAJCompression.decompressResponse;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.HTTP_ERROR;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.IO_ERROR;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.NO_SUPPORTED_PROTOCOLS;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.PROTOCOL_ERROR;
import static java.net.http.HttpResponse.BodyHandlers.ofByteArray;

/**
 * Functions to negotiate protocols.
 */

public final class EIAJProtocolNegotiation
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIAJProtocolNegotiation.class);

  private EIAJProtocolNegotiation()
  {

  }

  private static List<IdAServerEndpoint> fetchSupportedVersions(
    final URI base,
    final HttpClient httpClient,
    final EIAJStrings strings)
    throws InterruptedException, EIAJClientException
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
      throw new EIAJClientException(IO_ERROR, e);
    }

    LOG.debug("server: status {}", response.statusCode());

    if (response.statusCode() >= 400) {
      throw new EIAJClientException(
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
      throw new EIAJClientException(PROTOCOL_ERROR, e);
    } catch (final IOException e) {
      throw new EIAJClientException(IO_ERROR, e);
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
   * @throws EIAJClientException   On errors
   * @throws InterruptedException On interruption
   */

  public static EIAJClientProtocolHandlerType negotiateProtocolHandler(
    final Locale locale,
    final HttpClient httpClient,
    final EIAJStrings strings,
    final URI base)
    throws EIAJClientException, InterruptedException
  {
    Objects.requireNonNull(locale, "locale");
    Objects.requireNonNull(httpClient, "httpClient");
    Objects.requireNonNull(strings, "strings");
    Objects.requireNonNull(base, "base");

    final var clientSupports =
      List.of(
        new EIAJClientProtocolHandlers1()
      );

    final var serverProtocols =
      fetchSupportedVersions(base, httpClient, strings);

    LOG.debug("server supports {} protocols", serverProtocols.size());

    final var solver =
      GenProtocolSolver.<EIAJClientProtocolHandlerFactoryType, IdAServerEndpoint>create(
        locale);

    final GenProtocolSolved<EIAJClientProtocolHandlerFactoryType, IdAServerEndpoint> solved;
    try {
      solved = solver.solve(
        serverProtocols,
        clientSupports,
        List.of(EIAJCB1Messages.protocolId().toString())
      );
    } catch (final GenProtocolException e) {
      throw new EIAJClientException(NO_SUPPORTED_PROTOCOLS, e.getMessage(), e);
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
