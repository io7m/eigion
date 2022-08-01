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

import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandType;
import com.io7m.eigion.protocol.admin_api.v1.EISA1MessageType;
import com.io7m.eigion.protocol.admin_api.v1.EISA1Messages;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseError;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseType;
import com.io7m.eigion.protocol.api.EIProtocolException;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static java.nio.charset.StandardCharsets.UTF_8;

public interface EIServerAdminMessengerType
{
  Logger msgLogger();

  HttpClient msgHttpClient();

  EISA1Messages msgAdmin();

  default HttpResponse<byte[]> msgSendAdminText(
    final String endpoint,
    final String text)
    throws Exception
  {
    return this.msgSendAdminBytes(endpoint, text.getBytes(UTF_8));
  }

  default HttpResponse<byte[]> msgSendAdminBytes(
    final String endpoint,
    final byte[] text)
    throws Exception
  {
    final var request =
      HttpRequest.newBuilder()
        .POST(HttpRequest.BodyPublishers.ofByteArray(text))
        .uri(URI.create("http://localhost:40000" + endpoint))
        .build();

    final var response =
      this.msgHttpClient().send(
        request,
        HttpResponse.BodyHandlers.ofByteArray()
      );

    try {
      final var rr = this.msgAdmin().parse(response.body());
      this.msgLogger().debug("response: {}", rr);
    } catch (final Exception e) {
      // OK
    }

    return response;
  }

  default HttpResponse<byte[]> msgSendAdminBytesOrFail(
    final String endpoint,
    final byte[] text)
    throws Exception
  {
    final var r =
      this.msgSendAdminBytes(endpoint, text);

    if (r.statusCode() >= 400) {
      final var error =
        this.msgParseAdmin(r, EISA1ResponseError.class);

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

  default HttpResponse<byte[]> msgSendAdminCommand(
    final EISA1CommandType command)
    throws Exception
  {
    return this.msgSendAdminBytes(
      "/public/1/0/command",
      this.msgAdmin().serialize(command)
    );
  }

  default <T extends EISA1ResponseType> T msgSendAdminCommandOrFail(
    final EISA1CommandType command,
    final Class<T> responseClass)
    throws Exception
  {
    final var r =
      this.msgSendAdminBytesOrFail(
        "/public/1/0/command",
        this.msgAdmin().serialize(command)
      );
    return this.msgParseAdmin(r, responseClass);
  }

  default <T extends EISA1MessageType> T msgParseAdmin(
    final HttpResponse<byte[]> response,
    final Class<T> clazz)
    throws EIProtocolException
  {
    final var bodyText = response.body();
    this.msgLogger().debug("received: {}", new String(bodyText, UTF_8));
    return clazz.cast(this.msgAdmin().parse(bodyText));
  }

  default HttpResponse<byte[]> msgGetAdmin(
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
