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
import com.io7m.eigion.model.EIProduct;
import com.io7m.eigion.model.EIProductCategory;
import com.io7m.eigion.model.EIProductDependency;
import com.io7m.eigion.model.EIProductRelease;
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
  @JsonProperty(value = "ID", required = true)
  public final EIv1ProductId id;

  @JsonProperty(value = "Releases", required = true)
  public final List<EIv1ProductRelease> releases;

  @JsonProperty(value = "Categories", required = true)
  public final List<String> categories;

  @JsonCreator
  public EIv1Product(
    @JsonProperty(value = "ID", required = true) final EIv1ProductId inId,
    @JsonProperty(value = "Releases", required = true) final List<EIv1ProductRelease> inR,
    @JsonProperty(value = "Categories", required = true) final List<String> inCategories)
  {
    this.id =
      Objects.requireNonNull(inId, "name");
    this.releases =
      Objects.requireNonNull(inR, "releases");
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
    final var newReleases =
      this.toReleases(source, errorConsumer);

    if (newId.isPresent()
      && newCategories.isPresent()
      && newReleases.isPresent()) {

      return Optional.of(
        new EIProduct(
          newId.get(),
          newReleases.get(),
          newCategories.get(),
          Optional.empty()
        )
      );
    }

    return Optional.empty();
  }

  private Optional<List<EIProductRelease>> toReleases(
    final URI source,
    final Consumer<ParseStatus> errorConsumer)
  {
    var anyFailed = false;

    final var releases =
      new ArrayList<EIProductRelease>(this.releases.size());

    for (final var release : this.releases) {
      final var releaseOpt =
        release.toProduct(source, errorConsumer);
      if (releaseOpt.isEmpty()) {
        anyFailed = true;
      } else {
        releases.add(releaseOpt.get());
      }
    }

    return anyFailed ? Optional.empty() : Optional.of(releases);
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
