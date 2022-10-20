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

open module com.io7m.eigion.tests
{
  requires transitive org.slf4j;

  requires com.io7m.idstore.model;
  requires com.io7m.jmulticlose.core;
  requires org.apache.derby.client;
  requires org.apache.derby.commons;
  requires org.apache.derby.engine;
  requires org.apache.derby.server;
  requires org.apache.derby.tools;
  requires org.eclipse.jetty.server;
  requires org.eclipse.jetty.servlet;

  requires transitive com.io7m.eigion.amberjack.api;
  requires transitive com.io7m.eigion.amberjack;
  requires transitive com.io7m.eigion.client.api;
  requires transitive com.io7m.eigion.client.database.api;
  requires transitive com.io7m.eigion.client.database;
  requires transitive com.io7m.eigion.client.vanilla;
  requires transitive com.io7m.eigion.domaincheck.api;
  requires transitive com.io7m.eigion.domaincheck;
  requires transitive com.io7m.eigion.error_codes;
  requires transitive com.io7m.eigion.hash;
  requires transitive com.io7m.eigion.launcher.api;
  requires transitive com.io7m.eigion.launcher.felix;
  requires transitive com.io7m.eigion.launcher.main;
  requires transitive com.io7m.eigion.model;
  requires transitive com.io7m.eigion.news.xml;
  requires transitive com.io7m.eigion.pike.api;
  requires transitive com.io7m.eigion.pike;
  requires transitive com.io7m.eigion.preferences;
  requires transitive com.io7m.eigion.protocol.amberjack.cb;
  requires transitive com.io7m.eigion.protocol.amberjack;
  requires transitive com.io7m.eigion.protocol.api;
  requires transitive com.io7m.eigion.protocol.pike.cb;
  requires transitive com.io7m.eigion.protocol.pike;
  requires transitive com.io7m.eigion.server.api;
  requires transitive com.io7m.eigion.server.database.api;
  requires transitive com.io7m.eigion.server.database.postgres;
  requires transitive com.io7m.eigion.server;
  requires transitive com.io7m.eigion.services.api;
  requires transitive com.io7m.eigion.storage.api;
  requires transitive com.io7m.eigion.storage.derby;
  requires transitive com.io7m.eigion.storage.s3;
  requires transitive com.io7m.eigion.taskrecorder;
  requires transitive com.io7m.eigion.tests.arbitraries;
}
