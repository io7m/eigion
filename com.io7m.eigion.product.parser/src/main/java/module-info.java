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

import com.io7m.eigion.product.parser.EIProductReleaseParsers;
import com.io7m.eigion.product.parser.EIProductReleaseSerializers;
import com.io7m.eigion.product.parser.EIProductsParsers;
import com.io7m.eigion.product.parser.EIProductsSerializers;
import com.io7m.eigion.product.parser.api.EIProductReleaseParsersType;
import com.io7m.eigion.product.parser.api.EIProductReleaseSerializersType;
import com.io7m.eigion.product.parser.api.EIProductsParsersType;
import com.io7m.eigion.product.parser.api.EIProductsSerializersType;

/**
 * Application runtime management (Product parser)
 */

module com.io7m.eigion.product.parser
{
  requires static org.osgi.annotation.bundle;
  requires static org.osgi.annotation.versioning;

  requires transitive com.io7m.anethum.api;
  requires transitive com.io7m.anethum.common;
  requires transitive com.io7m.eigion.model;
  requires transitive com.io7m.eigion.product.parser.api;

  requires com.io7m.dixmont.core;

  exports com.io7m.eigion.product.parser;

  exports com.io7m.eigion.product.parser.internal
    to com.fasterxml.jackson.databind, com.io7m.eigion.tests;
  exports com.io7m.eigion.product.parser.internal.v1
    to com.fasterxml.jackson.databind, com.io7m.eigion.tests;

  provides EIProductsSerializersType
    with EIProductsSerializers;
  provides EIProductsParsersType
    with EIProductsParsers;
  provides EIProductReleaseParsersType
    with EIProductReleaseParsers;
  provides EIProductReleaseSerializersType
    with EIProductReleaseSerializers;
}
