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
import com.io7m.anethum.common.ParseStatus;
import com.io7m.eigion.model.EIChange;
import com.io7m.eigion.model.EIProductBundleDependency;
import com.io7m.eigion.model.EIProductDependency;
import com.io7m.eigion.model.EIProductRelease;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/*
 * These are effectively JSON DTOs and therefore are exempt from the usual style checks.
 */

// CHECKSTYLE:OFF

@JsonSerialize
@JsonDeserialize
public final class EIv1ProductRelease
  implements EIv1FromV1Type<EIProductRelease>
{
  @JsonProperty(value = "Version", required = true)
  public final String version;

  @JsonProperty(value = "ProductDependencies", required = true)
  public final List<EIv1ProductDependency> productDependencies;

  @JsonProperty(value = "BundleDependencies", required = true)
  public final List<EIv1ProductBundleDependency> bundleDependencies;

  @JsonProperty(value = "Changes", required = true)
  public final List<EIv1Change> changes;

  @JsonCreator
  public EIv1ProductRelease(
    @JsonProperty(value = "Version", required = true) final String inVersion,
    @JsonProperty(value = "ProductDependencies", required = true) final List<EIv1ProductDependency> inPd,
    @JsonProperty(value = "BundleDependencies", required = true) final List<EIv1ProductBundleDependency> inBd,
    @JsonProperty(value = "Changes", required = true) final List<EIv1Change> inC)
  {
    this.version =
      Objects.requireNonNull(inVersion, "version");
    this.productDependencies =
      Objects.requireNonNull(inPd, "productDependencies");
    this.bundleDependencies =
      Objects.requireNonNull(inBd, "bundleDependencies");
    this.changes =
      Objects.requireNonNull(inC, "changes");
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

  private static Optional<List<EIProductBundleDependency>> toBundleDependencies(
    final List<EIv1ProductBundleDependency> dependencies,
    final URI source,
    final Consumer<ParseStatus> errorConsumer)
  {
    var anyFailed = false;

    final var newDeps =
      new ArrayList<EIProductBundleDependency>(dependencies.size());

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

  private static Optional<List<EIChange>> toChanges(
    final List<EIv1Change> changes,
    final URI source,
    final Consumer<ParseStatus> errorConsumer)
  {
    final var newChanges =
      changes.stream()
        .map(t -> t.toProduct(source, errorConsumer))
        .toList();

    for (final var newChange : newChanges) {
      if (newChange.isEmpty()) {
        return Optional.empty();
      }
    }
    return Optional.of(
      newChanges.stream()
        .map(Optional::orElseThrow)
        .toList()
    );
  }

  @Override
  public Optional<EIProductRelease> toProduct(
    final URI source,
    final Consumer<ParseStatus> errorConsumer)
  {
    final var newVersion =
      EIv1ProductVersion.toProduct(source, errorConsumer, this.version);
    final var newProductDependencies =
      toDependencies(this.productDependencies, source, errorConsumer);
    final var newBundleDependencies =
      toBundleDependencies(this.bundleDependencies, source, errorConsumer);
    final Optional<List<EIChange>> newChanges =
      toChanges(this.changes, source, errorConsumer);

    if (newVersion.isPresent()
      && newProductDependencies.isPresent()
      && newBundleDependencies.isPresent()
      && newChanges.isPresent()) {

      return Optional.of(
        new EIProductRelease(
          newVersion.get(),
          newProductDependencies.get(),
          newBundleDependencies.get(),
          newChanges.get()
        )
      );
    }

    return Optional.empty();
  }
}
