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
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.io7m.anethum.common.ParseException;
import com.io7m.anethum.common.ParseStatus;
import com.io7m.dixmont.core.DmJsonRestrictedDeserializers;
import com.io7m.eigion.model.EIProduct;
import com.io7m.eigion.model.EIProducts;
import com.io7m.eigion.product.parser.api.EIProductsParserType;
import com.io7m.eigion.product.parser.internal.v1.EIv1Product;
import com.io7m.eigion.product.parser.internal.v1.EIv1ProductDependency;
import com.io7m.eigion.product.parser.internal.v1.EIv1ProductHash;
import com.io7m.eigion.product.parser.internal.v1.EIv1ProductId;
import com.io7m.eigion.product.parser.internal.v1.EIv1ProductRelease;
import com.io7m.eigion.product.parser.internal.v1.EIv1Products;
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.jlexing.core.LexicalPositions;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static com.io7m.anethum.common.ParseSeverity.PARSE_ERROR;

/**
 * A products parser.
 */

public final class EIProductsParser implements EIProductsParserType
{
  private final URI source;
  private final InputStream stream;
  private final Consumer<ParseStatus> statusConsumer;
  private final SimpleDeserializers serializers;
  private final JsonMapper mapper;

  /**
   * A products parser.
   *
   * @param inSource         The source URI
   * @param inStream         The source stream
   * @param inStatusConsumer The status consumer
   */

  public EIProductsParser(
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

    this.serializers =
      DmJsonRestrictedDeserializers.builder()
        .allowClass(EIv1Product.class)
        .allowClass(EIv1ProductDependency.class)
        .allowClass(EIv1ProductHash.class)
        .allowClass(EIv1ProductId.class)
        .allowClass(EIv1ProductRelease.class)
        .allowClass(EIv1Products.class)
        .allowClass(EIvNProductsType.class)
        .allowClass(String.class)
        .allowClassName(
          "java.util.List<com.io7m.eigion.product.parser.internal.v1.EIv1ProductRelease>")
        .allowClassName(
          "java.util.List<com.io7m.eigion.product.parser.internal.v1.EIv1ProductDependency>")
        .allowClassName(
          "java.util.List<com.io7m.eigion.product.parser.internal.v1.EIv1Product>")
        .allowClassName(
          "java.util.List<java.lang.String>")
        .build();

    this.mapper =
      JsonMapper.builder()
        .build();

    final var simpleModule = new SimpleModule();
    simpleModule.setDeserializers(this.serializers);
    this.mapper.registerModule(simpleModule);
  }

  @Override
  public EIProducts execute()
    throws ParseException
  {
    final var productList =
      new ArrayList<EIProduct>();
    final var errors =
      new ArrayList<ParseStatus>();

    final Consumer<ParseStatus> errorConsumer = (ParseStatus status) -> {
      errors.add(status);
      this.statusConsumer.accept(status);
    };

    try {
      final var products =
        this.mapper.readValue(this.stream, EIvNProductsType.class);

      if (products instanceof EIv1Products v1products) {
        return switch (this.parseProductsV1(
          v1products,
          productList,
          errorConsumer)) {
          case SUCCEEDED -> new EIProducts(List.copyOf(productList));
          case FAILED -> throw new ParseException(
            "Product parsing failed.",
            errors);
        };
      }

      throw new IllegalStateException(
        "Unrecognized product type: %s".formatted(products.getClass())
      );
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

  private Status parseProductsV1(
    final EIv1Products v1products,
    final ArrayList<EIProduct> productList,
    final Consumer<ParseStatus> errorConsumer)
  {
    var failedAny = false;

    for (final var v1Product : v1products.products) {
      final var productOpt =
        v1Product.toProduct(this.source, errorConsumer);

      if (productOpt.isPresent()) {
        productList.add(productOpt.get());
      } else {
        failedAny = true;
      }
    }

    return failedAny ? Status.FAILED : Status.SUCCEEDED;
  }

  @Override
  public void close()
    throws IOException
  {
    this.stream.close();
  }

  private enum Status
  {
    FAILED,
    SUCCEEDED
  }
}
