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

import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.io7m.dixmont.core.DmJsonRestrictedDeserializers;
import com.io7m.eigion.product.parser.internal.v1.EIv1Change;
import com.io7m.eigion.product.parser.internal.v1.EIv1ChangeTicket;
import com.io7m.eigion.product.parser.internal.v1.EIv1Link;
import com.io7m.eigion.product.parser.internal.v1.EIv1Product;
import com.io7m.eigion.product.parser.internal.v1.EIv1ProductBundleDependency;
import com.io7m.eigion.product.parser.internal.v1.EIv1ProductDependency;
import com.io7m.eigion.product.parser.internal.v1.EIv1ProductDescription;
import com.io7m.eigion.product.parser.internal.v1.EIv1ProductHash;
import com.io7m.eigion.product.parser.internal.v1.EIv1ProductId;
import com.io7m.eigion.product.parser.internal.v1.EIv1ProductRelease;
import com.io7m.eigion.product.parser.internal.v1.EIv1Products;
import com.io7m.eigion.product.parser.internal.v1.EIv1RichText;

import java.net.URI;

/**
 * Configuration for JSON serializers.
 */

public final class EISerializers
{
  static final SimpleDeserializers SERIALIZERS =
    createSerializers();

  private EISerializers()
  {

  }

  private static String listOf(
    final Class<?> clazz)
  {
    return String.format("java.util.List<%s>", clazz.getCanonicalName());
  }

  private static SimpleDeserializers createSerializers()
  {
    return DmJsonRestrictedDeserializers.builder()
      .allowClass(EIv1Change.class)
      .allowClass(EIv1ChangeTicket.class)
      .allowClass(EIv1Link.class)
      .allowClass(EIv1Product.class)
      .allowClass(EIv1ProductBundleDependency.class)
      .allowClass(EIv1ProductDependency.class)
      .allowClass(EIv1ProductDescription.class)
      .allowClass(EIv1ProductHash.class)
      .allowClass(EIv1ProductId.class)
      .allowClass(EIv1ProductRelease.class)
      .allowClass(EIv1Products.class)
      .allowClass(EIv1RichText.class)
      .allowClass(EIvNProductsType.class)
      .allowClass(String.class)
      .allowClass(URI.class)
      .allowClassName(listOf(EIv1Change.class))
      .allowClassName(listOf(EIv1ChangeTicket.class))
      .allowClassName(listOf(EIv1Link.class))
      .allowClassName(listOf(EIv1Product.class))
      .allowClassName(listOf(EIv1ProductBundleDependency.class))
      .allowClassName(listOf(EIv1ProductDependency.class))
      .allowClassName(listOf(EIv1ProductRelease.class))
      .allowClassName(listOf(String.class))
      .allowClassName(listOf(URI.class))
      .build();
  }
}
