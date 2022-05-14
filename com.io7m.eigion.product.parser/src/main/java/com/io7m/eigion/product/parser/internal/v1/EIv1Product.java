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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.io7m.anethum.common.ParseSeverity;
import com.io7m.anethum.common.ParseStatus;
import com.io7m.eigion.product.api.EIProduct;
import com.io7m.eigion.product.api.EIProductCategory;
import com.io7m.eigion.product.api.EIProductDependency;
import com.io7m.jlexing.core.LexicalPositions;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/*
 * These are effectively JSON DTOs and therefore are exempt from the usual style checks.
 */

// CHECKSTYLE:OFF

@JsonSerialize
@JsonDeserialize
public final class EIv1Product implements EIv1FromV1Type<EIProduct>
{
  @JsonProperty(value = "id", required = true)
  public final EIv1ProductId id;

  @JsonProperty(value = "product-dependencies", required = true)
  public final List<EIv1ProductDependency> productDependencies;

  @JsonProperty(value = "bundle-dependencies", required = true)
  public final List<EIv1ProductDependency> bundleDependencies;

  @JsonProperty(value = "categories", required = true)
  public final List<String> categories;

  @JsonCreator
  public EIv1Product(
    @JsonProperty(value = "id", required = true) final EIv1ProductId inId,
    @JsonProperty(value = "product-dependencies", required = true) final List<EIv1ProductDependency> inPd,
    @JsonProperty(value = "bundle-dependencies", required = true) final List<EIv1ProductDependency> inBd,
    @JsonProperty(value = "categories", required = true) final List<String> inCategories)
  {
    this.id =
      Objects.requireNonNull(inId, "name");
    this.productDependencies =
      Objects.requireNonNull(inPd, "productDependencies");
    this.bundleDependencies =
      Objects.requireNonNull(inBd, "bundleDependencies");
    this.categories =
      Objects.requireNonNull(inCategories, "categories");
  }

  private static Optional<List<EIProductDependency>> toDependencies(
    final List<EIv1ProductDependency> dependencies,
    final URI source,
    final Consumer<ParseStatus> errorConsumer)
  {
    var anyFailed = false;

    final var newDeps =
      new ArrayList<EIProductDependency>(dependencies.size());

    for (final var dep : dependencies) {
      final var newDep =
        dep.toProduct(source, errorConsumer);

      if (newDep.isEmpty()) {
        anyFailed = true;
        continue;
      }
      newDeps.add(newDep.get());
    }

    return anyFailed ? Optional.empty() : Optional.of(newDeps);
  }

  @Override
  public Optional<EIProduct> toProduct(
    final URI source,
    final Consumer<ParseStatus> errorConsumer)
  {
    final var newId =
      this.id.toProduct(source, errorConsumer);
    final var newCategories =
      this.toProductCategories(source, errorConsumer);
    final var newProductDependencies =
      toDependencies(this.productDependencies, source, errorConsumer);
    final var newBundleDependencies =
      toDependencies(this.bundleDependencies, source, errorConsumer);

    if (newId.isPresent()
      && newCategories.isPresent()
      && newProductDependencies.isPresent()
      && newBundleDependencies.isPresent()) {

      return Optional.of(
        new EIProduct(
          newId.get(),
          newProductDependencies.get(),
          newBundleDependencies.get(),
          newCategories.get())
      );
    }

    return Optional.empty();
  }

  private Optional<Set<EIProductCategory>> toProductCategories(
    final URI source,
    final Consumer<ParseStatus> errorConsumer)
  {
    var anyFailed = false;

    final var categories =
      new HashSet<EIProductCategory>(this.categories.size());

    for (final var name : this.categories) {
      try {
        categories.add(EIProductCategory.category(name));
      } catch (final IllegalArgumentException e) {
        anyFailed = true;
        errorConsumer.accept(
          ParseStatus.builder()
            .setMessage(e.getMessage())
            .setLexical(LexicalPositions.zeroWithFile(source))
            .setErrorCode("invalid-category")
            .setSeverity(ParseSeverity.PARSE_ERROR)
            .build()
        );
      }
    }

    return anyFailed ? Optional.empty() : Optional.of(categories);
  }
}
