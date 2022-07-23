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

/**
 * Eigion platform (Client vanilla implementation)
 */

module com.io7m.eigion.client.vanilla
{
  requires static org.osgi.annotation.versioning;
  requires static org.osgi.annotation.bundle;

  requires com.fasterxml.jackson.annotation;
  requires com.fasterxml.jackson.databind;
  requires com.io7m.dixmont.core;
  requires com.io7m.eigion.client.api;
  requires com.io7m.jxtrand.api;
  requires com.io7m.jxtrand.vanilla;
  requires java.net.http;

  opens com.io7m.eigion.client.vanilla.v1
    to com.fasterxml.jackson.databind;
  opens com.io7m.eigion.client.vanilla.internal
    to com.io7m.jxtrand.vanilla;

  exports com.io7m.eigion.client.vanilla;

  exports com.io7m.eigion.client.vanilla.v1
    to com.io7m.eigion.tests;
}
