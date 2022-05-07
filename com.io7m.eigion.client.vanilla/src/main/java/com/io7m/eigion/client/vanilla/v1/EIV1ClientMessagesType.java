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

package com.io7m.eigion.client.vanilla.v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

/**
 * The V1 client messages.
 */

// CHECKSTYLE:OFF

public interface EIV1ClientMessagesType
{
  @JsonDeserialize
  @JsonSerialize
  final class EIV1Login implements EIV1ClientMessagesType
  {
    @JsonProperty(value = "%type", required = true)
    public final String type;
    @JsonProperty(value = "userName", required = true)
    public final String userName;
    @JsonProperty(value = "password", required = true)
    public final String password;

    @JsonCreator
    public EIV1Login(
      final @JsonProperty(value = "%type", required = true) String type,
      final @JsonProperty(value = "userName", required = true) String userName,
      final @JsonProperty(value = "password", required = true) String password)
    {
      this.type = type;
      this.userName = userName;
      this.password = password;
    }
  }

  @JsonDeserialize
  @JsonSerialize
  final class EIV1LoginResponse implements EIV1ClientMessagesType
  {
    @JsonProperty(value = "%type", required = true)
    public final String type;
    @JsonProperty(value = "message", required = true)
    public final String message;

    @JsonCreator
    public EIV1LoginResponse(
      final @JsonProperty(value = "%type", required = true) String type,
      final @JsonProperty(value = "message", required = true) String message)
    {
      this.type = type;
      this.message = message;
    }
  }

  @JsonDeserialize
  @JsonSerialize
  final class EIV1NewsItem implements EIV1ClientMessagesType
  {
    @JsonProperty(value = "%type", required = true)
    public final String type;
    @JsonProperty(value = "date", required = true)
    public final String date;
    @JsonProperty(value = "format", required = true)
    public final String format;
    @JsonProperty(value = "title", required = true)
    public final String title;
    @JsonProperty(value = "text", required = true)
    public final String text;

    @JsonCreator
    public EIV1NewsItem(
      final @JsonProperty(value = "%type", required = true) String type,
      final @JsonProperty(value = "date", required = true) String date,
      final @JsonProperty(value = "format", required = true) String format,
      final @JsonProperty(value = "title", required = true) String title,
      final @JsonProperty(value = "text", required = true) String text)
    {
      this.type = type;
      this.date = date;
      this.format = format;
      this.title = title;
      this.text = text;
    }
  }

  @JsonDeserialize
  @JsonSerialize
  final class EIV1News implements EIV1ClientMessagesType
  {
    @JsonProperty(value = "%type", required = true)
    public final String type;
    @JsonProperty(value = "items", required = true)
    public final List<EIV1NewsItem> items;

    @JsonCreator
    public EIV1News(
      final @JsonProperty(value = "%type", required = true) String type,
      final @JsonProperty(value = "items", required = true) List<EIV1NewsItem> items)
    {
      this.type = type;
      this.items = items;
    }
  }
}
