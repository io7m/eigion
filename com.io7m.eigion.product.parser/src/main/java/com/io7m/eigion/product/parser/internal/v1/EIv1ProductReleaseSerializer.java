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


package com.io7m.eigion.product.parser.internal.v1;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.io7m.anethum.common.SerializeException;
import com.io7m.eigion.model.EIChange;
import com.io7m.eigion.model.EIChangeTicket;
import com.io7m.eigion.model.EIProductBundleDependency;
import com.io7m.eigion.model.EIProductDependency;
import com.io7m.eigion.hash.EIHash;
import com.io7m.eigion.model.EIProductIdentifier;
import com.io7m.eigion.model.EIProductRelease;
import com.io7m.eigion.model.EIProductVersion;
import com.io7m.eigion.product.parser.api.EIProductReleaseSerializerType;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * A version 1 serializer.
 */

public final class EIv1ProductReleaseSerializer
  implements EIProductReleaseSerializerType
{
  private final URI target;
  private final OutputStream stream;
  private final JsonMapper mapper;

  /**
   * A version 1 serializer.
   *
   * @param inTarget The target URI
   * @param inStream The output stream
   */

  public EIv1ProductReleaseSerializer(
    final URI inTarget,
    final OutputStream inStream)
  {
    this.target =
      Objects.requireNonNull(inTarget, "target");
    this.stream =
      Objects.requireNonNull(inStream, "stream");

    this.mapper =
      JsonMapper.builder()
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .build();
  }

  private static EIv1ProductRelease convertRelease(
    final EIProductRelease release)
  {
    return new EIv1ProductRelease(
      convertProductVersion(release.version()),
      convertProductDependencies(release.productDependencies()),
      convertBundleDependencies(release.bundleDependencies()),
      convertChanges(release.changes())
    );
  }

  private static List<EIv1Change> convertChanges(
    final List<EIChange> changes)
  {
    return changes.stream()
      .map(EIv1ProductReleaseSerializer::convertChange)
      .toList();
  }

  private static EIv1Change convertChange(
    final EIChange c)
  {
    return new EIv1Change(
      c.description(),
      c.tickets().stream()
        .map(EIv1ProductReleaseSerializer::convertChangeTicket)
        .toList()
    );
  }

  private static EIv1ChangeTicket convertChangeTicket(
    final EIChangeTicket t)
  {
    return new EIv1ChangeTicket(t.name(), t.location());
  }

  private static List<EIv1ProductBundleDependency> convertBundleDependencies(
    final List<EIProductBundleDependency> productDependencies)
  {
    return productDependencies.stream()
      .map(k -> new EIv1ProductBundleDependency(
        convertProductId(k.identifier()),
        convertProductVersion(k.version()),
        convertHash(k.hash()),
        k.links()
      )).toList();
  }

  private static List<EIv1ProductDependency> convertProductDependencies(
    final List<EIProductDependency> productDependencies)
  {
    return productDependencies.stream()
      .map(k -> new EIv1ProductDependency(
        convertProductId(k.identifier()),
        convertProductVersion(k.version())
      )).toList();
  }

  private static EIv1ProductHash convertHash(
    final EIHash hash)
  {
    return new EIv1ProductHash(hash.algorithm(), hash.hash());
  }

  private static EIv1ProductId convertProductId(
    final EIProductIdentifier id)
  {
    return new EIv1ProductId(
      id.name(),
      id.group()
    );
  }

  private static String convertProductVersion(
    final EIProductVersion version)
  {
    final var q = version.qualifier();
    final var vmaj = version.major();
    final var vmin = version.minor();
    final var vpat = version.patch();
    if (q.isPresent()) {
      return String.format("%s.%s.%s-%s", vmaj, vmin, vpat, q.get());
    }
    return String.format("%s.%s.%s", vmaj, vmin, vpat);
  }

  @Override
  public void execute(final EIProductRelease value)
    throws SerializeException
  {
    try {
      this.mapper.writeValue(this.stream, convertRelease(value));
    } catch (final IOException e) {
      throw new SerializeException(e.getMessage(), e);
    }
  }

  @Override
  public void close()
    throws IOException
  {
    this.stream.close();
  }
}
