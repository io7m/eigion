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

package com.io7m.eigion.product.parser.internal;

import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.io7m.anethum.common.ParseException;
import com.io7m.anethum.common.ParseStatus;
import com.io7m.eigion.model.EIProductRelease;
import com.io7m.eigion.product.parser.api.EIProductReleaseParserType;
import com.io7m.eigion.product.parser.internal.v1.EIv1ProductRelease;
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.jlexing.core.LexicalPositions;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static com.io7m.anethum.common.ParseSeverity.PARSE_ERROR;
import static com.io7m.eigion.product.parser.internal.EISerializers.SERIALIZERS;

/**
 * A product release parser.
 */

public final class EIProductReleaseParser
  implements EIProductReleaseParserType
{
  private final URI source;
  private final InputStream stream;
  private final Consumer<ParseStatus> statusConsumer;
  private final JsonMapper mapper;

  /**
   * A products parser.
   *
   * @param inSource         The source URI
   * @param inStream         The source stream
   * @param inStatusConsumer The status consumer
   */

  public EIProductReleaseParser(
    final URI inSource,
    final InputStream inStream,
    final Consumer<ParseStatus> inStatusConsumer)
  {
    this.source =
      Objects.requireNonNull(inSource, "source");
    this.stream =
      Objects.requireNonNull(inStream, "stream");
    this.statusConsumer =
      Objects.requireNonNull(inStatusConsumer, "statusConsumer");

    this.mapper =
      JsonMapper.builder()
        .build();

    final var simpleModule = new SimpleModule();
    simpleModule.setDeserializers(SERIALIZERS);
    this.mapper.registerModule(simpleModule);
  }

  @Override
  public EIProductRelease execute()
    throws ParseException
  {
    final var errors =
      new ArrayList<ParseStatus>();

    final Consumer<ParseStatus> errorConsumer = (ParseStatus status) -> {
      errors.add(status);
      this.statusConsumer.accept(status);
    };

    try {
      final var products =
        this.mapper.readValue(this.stream, EIv1ProductRelease.class);
      return products.toProduct(this.source, errorConsumer)
        .orElseThrow(() -> new ParseException("Parsing failed", errors));
    } catch (final DatabindException e) {
      final var loc = e.getLocation();
      errorConsumer.accept(
        ParseStatus.builder()
          .setMessage(e.getMessage())
          .setErrorCode("databind")
          .setLexical(LexicalPosition.of(
            loc.getLineNr(),
            loc.getColumnNr(),
            Optional.of(this.source)))
          .setSeverity(PARSE_ERROR)
          .build()
      );
      throw new ParseException(e.getMessage(), errors);
    } catch (final IOException e) {
      errorConsumer.accept(
        ParseStatus.builder()
          .setMessage(e.getMessage())
          .setErrorCode("parse-failed")
          .setLexical(LexicalPositions.zeroWithFile(this.source))
          .setSeverity(PARSE_ERROR)
          .build()
      );
      throw new ParseException(e.getMessage(), errors);
    }
  }

  @Override
  public void close()
    throws IOException
  {
    this.stream.close();
  }
}
