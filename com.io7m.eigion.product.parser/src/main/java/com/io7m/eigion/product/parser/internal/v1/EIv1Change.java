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
import com.io7m.eigion.model.EIChange;
import com.io7m.jlexing.core.LexicalPositions;

import java.net.URI;
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
public final class EIv1Change
  implements EIv1FromV1Type<EIChange>
{
  @JsonProperty(value = "Description", required = true)
  public final String description;
  @JsonProperty(value = "Tickets", required = true)
  public final List<EIv1ChangeTicket> tickets;

  @JsonCreator
  public EIv1Change(
    @JsonProperty(value = "Description", required = true) final String inDescription,
    @JsonProperty(value = "Tickets", required = true) final List<EIv1ChangeTicket> inT)
  {
    this.description =
      Objects.requireNonNull(inDescription, "description");
    this.tickets =
      Objects.requireNonNull(inT, "tickets");
  }

  @Override
  public Optional<EIChange> toProduct(
    final URI source,
    final Consumer<ParseStatus> errorConsumer)
  {
    try {
      final var ticketOpts =
        this.tickets.stream()
          .map(t -> t.toProduct(source, errorConsumer))
          .toList();

      for (final var opt : ticketOpts) {
        if (opt.isEmpty()) {
          return Optional.empty();
        }
      }

      return Optional.of(
        new EIChange(
          this.description,
          ticketOpts.stream()
            .map(Optional::orElseThrow)
            .toList())
      );
    } catch (final IllegalArgumentException e) {
      errorConsumer.accept(
        ParseStatus.builder()
          .setSeverity(ParseSeverity.PARSE_ERROR)
          .setErrorCode("invalid-change")
          .setLexical(LexicalPositions.zeroWithFile(source))
          .setMessage(e.getMessage())
          .build()
      );
      return Optional.empty();
    }
  }
}
