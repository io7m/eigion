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

import com.io7m.eigion.amberjack.api.EIAClientFactoryType;
import com.io7m.eigion.amberjack.cmdline.EIShellFactoryType;

/**
 * Eigion platform (Amberjack command-line main distribution)
 */

open module com.io7m.eigion.amberjack.cmdline.main
{
  requires static org.osgi.annotation.bundle;
  requires static org.osgi.annotation.versioning;

  requires com.io7m.eigion.amberjack.api;
  requires com.io7m.eigion.amberjack.cmdline;

  requires ch.qos.logback.core;
  requires ch.qos.logback.classic;
  requires jcommander;
  requires org.slf4j;

  uses EIAClientFactoryType;
  uses EIShellFactoryType;

  exports com.io7m.eigion.amberjack.cmdline.main;
}
