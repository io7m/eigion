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
 * Eigion platform (Server controller)
 */

module com.io7m.eigion.server.controller
{
  requires static org.osgi.annotation.bundle;
  requires static org.osgi.annotation.versioning;

  requires com.io7m.eigion.error_codes;
  requires com.io7m.eigion.model;
  requires com.io7m.eigion.protocol.amberjack;
  requires com.io7m.eigion.protocol.api;
  requires com.io7m.eigion.protocol.pike;
  requires com.io7m.eigion.server.database.api;
  requires com.io7m.eigion.server.http;
  requires com.io7m.eigion.server.security;
  requires com.io7m.eigion.server.service.clock;
  requires com.io7m.eigion.server.service.domaincheck;
  requires com.io7m.eigion.server.service.idstore;
  requires com.io7m.eigion.server.service.sessions;
  requires com.io7m.eigion.server.service.telemetry.api;
  requires com.io7m.eigion.services.api;

  requires com.io7m.idstore.model;
  requires com.io7m.idstore.user_client.api;
  requires com.io7m.jxtrand.vanilla;
  requires jetty.servlet.api;

  exports com.io7m.eigion.server.controller.amberjack.security;
  exports com.io7m.eigion.server.controller.amberjack;
  exports com.io7m.eigion.server.controller.command_exec;
  exports com.io7m.eigion.server.controller.pike.security;
  exports com.io7m.eigion.server.controller.pike;
  exports com.io7m.eigion.server.controller;
  exports com.io7m.eigion.server.controller.login;
}
