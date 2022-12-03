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
 * Eigion platform (Server Pike v1 frontend)
 */

module com.io7m.eigion.server.pike_v1
{
  requires static org.osgi.annotation.bundle;
  requires static org.osgi.annotation.versioning;

  requires com.io7m.eigion.error_codes;
  requires com.io7m.eigion.protocol.pike.cb;
  requires com.io7m.eigion.protocol.pike;
  requires com.io7m.eigion.server.api;
  requires com.io7m.eigion.server.controller;
  requires com.io7m.eigion.server.database.api;
  requires com.io7m.eigion.server.http;
  requires com.io7m.eigion.server.service.clock;
  requires com.io7m.eigion.server.service.configuration;
  requires com.io7m.eigion.server.service.idstore;
  requires com.io7m.eigion.server.service.limits;
  requires com.io7m.eigion.server.service.sessions;
  requires com.io7m.eigion.server.service.verdant;

  requires com.io7m.idstore.model;
  requires com.io7m.verdant.core;
  requires jetty.servlet.api;
  requires org.eclipse.jetty.http;
  requires org.eclipse.jetty.servlet;

  exports com.io7m.eigion.server.pike_v1;
}
