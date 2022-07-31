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

import com.io7m.eigion.pike.api.EIPClientFactoryType;

/**
 * Eigion platform (Pike client)
 */

module com.io7m.eigion.pike
{
  requires static org.osgi.annotation.bundle;
  requires static org.osgi.annotation.versioning;

  requires transitive com.io7m.eigion.pike.api;

  requires com.io7m.eigion.protocol.api;
  requires com.io7m.eigion.protocol.public_api.v1;
  requires com.io7m.eigion.protocol.versions;
  requires com.io7m.jxtrand.vanilla;
  requires java.net.http;
  requires org.slf4j;

  opens com.io7m.eigion.pike.internal
    to com.io7m.jxtrand.vanilla;

  provides EIPClientFactoryType
    with com.io7m.eigion.pike.EIPClients;

  exports com.io7m.eigion.pike;
}