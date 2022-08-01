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


package com.io7m.eigion.tests;

import com.io7m.eigion.protocol.api.EIProtocolException;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandType;
import com.io7m.eigion.protocol.public_api.v1.EISP1MessageType;
import com.io7m.eigion.protocol.public_api.v1.EISP1Messages;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseError;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseType;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static java.nio.charset.StandardCharsets.UTF_8;

public interface EIServerPublicMessengerType
{
  Logger msgLogger();

  HttpClient msgHttpClient();

  EISP1Messages msgPublic();

  default HttpResponse<byte[]> msgSendPublicText(
    final String endpoint,
    final String text)
    throws Exception
  {
    return this.msgSendPublicBytes(endpoint, text.getBytes(UTF_8));
  }

  default HttpResponse<byte[]> msgSendPublicBytes(
    final String endpoint,
    final byte[] text)
    throws Exception
  {
    final var request =
      HttpRequest.newBuilder()
        .POST(HttpRequest.BodyPublishers.ofByteArray(text))
        .uri(URI.create("http://localhost:40001" + endpoint))
        .build();

    final var response =
      this.msgHttpClient().send(
        request,
        HttpResponse.BodyHandlers.ofByteArray()
      );

    try {
      final var rr = this.msgPublic().parse(response.body());
      this.msgLogger().debug("response: {}", rr);
    } catch (final Exception e) {
      // OK
    }

    return response;
  }

  default HttpResponse<byte[]> msgSendPublicBytesOrFail(
    final String endpoint,
    final byte[] text)
    throws Exception
  {
    final var r =
      this.msgSendPublicBytes(endpoint, text);

    if (r.statusCode() >= 400) {
      final var error =
        this.msgParsePublic(r, EISP1ResponseError.class);

      throw new IllegalStateException(
        "Server responded: %d: %s: %s"
          .formatted(
            Integer.valueOf(r.statusCode()),
            error.errorCode(),
            error.message())
      );
    }

    return r;
  }

  default HttpResponse<byte[]> msgSendPublicCommand(
    final EISP1CommandType command)
    throws Exception
  {
    return this.msgSendPublicBytes(
      "/public/1/0/command",
      this.msgPublic().serialize(command)
    );
  }

  default <T extends EISP1ResponseType> T msgSendPublicCommandOrFail(
    final EISP1CommandType command,
    final Class<T> responseClass)
    throws Exception
  {
    final var r =
      this.msgSendPublicBytesOrFail(
        "/public/1/0/command",
        this.msgPublic().serialize(command)
      );
    return this.msgParsePublic(r, responseClass);
  }

  default <T extends EISP1MessageType> T msgParsePublic(
    final HttpResponse<byte[]> response,
    final Class<T> clazz)
    throws EIProtocolException
  {
    final var bodyText = response.body();
    this.msgLogger().debug("received: {}", new String(bodyText, UTF_8));
    return clazz.cast(this.msgPublic().parse(bodyText));
  }

  default HttpResponse<byte[]> msgGetPublic(
    final String endpoint)
    throws Exception
  {
    final var request =
      HttpRequest.newBuilder()
        .GET()
        .uri(URI.create("http://localhost:40001" + endpoint))
        .build();

    return this.msgHttpClient().send(
      request,
      HttpResponse.BodyHandlers.ofByteArray()
    );
  }
}
