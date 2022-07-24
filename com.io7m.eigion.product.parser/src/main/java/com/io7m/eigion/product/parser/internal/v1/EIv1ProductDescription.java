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
import com.io7m.eigion.model.EIProductCategory;
import com.io7m.eigion.model.EIProductDescription;
import com.io7m.eigion.model.EIValidityException;
import com.io7m.jlexing.core.LexicalPositions;

import java.net.URI;
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
public final class EIv1ProductDescription
  implements EIv1FromV1Type<EIProductDescription>
{
  @JsonProperty(value = "Title", required = true)
  public final String title;

  @JsonProperty(value = "Description", required = true)
  public final EIv1RichText description;

  @JsonProperty(value = "Links", required = true)
  public final List<EIv1Link> links;

  @JsonProperty(value = "Categories", required = true)
  public final List<String> categories;

  @JsonCreator
  public EIv1ProductDescription(
    final @JsonProperty(value = "Title", required = true) String inT,
    final @JsonProperty(value = "Description", required = true) EIv1RichText inD,
    final @JsonProperty(value = "Links", required = true) List<EIv1Link> inLinks,
    final @JsonProperty(value = "Categories", required = true) List<String> inC)
  {
    this.title =
      Objects.requireNonNull(inT, "title");
    this.description =
      Objects.requireNonNull(inD, "description");
    this.links =
      Objects.requireNonNull(inLinks, "links");
    this.categories =
      Objects.requireNonNull(inC, "inC");
  }

  @Override
  public Optional<EIProductDescription> toProduct(
    final URI source,
    final Consumer<ParseStatus> errorConsumer)
  {
    try {
      final var newDescription =
        this.description.toProduct(source, errorConsumer);
      final var newCategories =
        this.toProductCategories(source, errorConsumer);

      final var newLinks =
        this.links.stream()
          .map(l -> l.toProduct(source, errorConsumer))
          .toList();

      for (final var newLink : newLinks) {
        if (newLink.isEmpty()) {
          return Optional.empty();
        }
      }

      if (newDescription.isEmpty()) {
        return Optional.empty();
      }

      if (newCategories.isEmpty()) {
        return Optional.empty();
      }

      return Optional.of(
        new EIProductDescription(
          this.title,
          newDescription.get(),
          newCategories.get(),
          newLinks.stream()
            .map(Optional::get)
            .toList())
      );
    } catch (final Exception e) {
      errorConsumer.accept(
        ParseStatus.builder()
          .setSeverity(ParseSeverity.PARSE_ERROR)
          .setErrorCode("invalid-product-description")
          .setLexical(LexicalPositions.zeroWithFile(source))
          .setMessage(e.getMessage())
          .build()
      );
      return Optional.empty();
    }
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
      } catch (final EIValidityException e) {
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
