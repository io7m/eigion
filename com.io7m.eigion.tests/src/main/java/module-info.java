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
  requires com.io7m.eigion.amberjack.api;
  requires com.io7m.eigion.amberjack;
  requires com.io7m.eigion.client.api;
  requires com.io7m.eigion.client.database.api;
  requires com.io7m.eigion.client.database;
  requires com.io7m.eigion.client.vanilla;
  requires com.io7m.eigion.domaincheck.api;
  requires com.io7m.eigion.domaincheck;
  requires com.io7m.eigion.error_codes;
  requires com.io7m.eigion.hash;
  requires com.io7m.eigion.launcher.api;
  requires com.io7m.eigion.launcher.felix;
  requires com.io7m.eigion.launcher.main;
  requires com.io7m.eigion.model;
  requires com.io7m.eigion.news.xml;
  requires com.io7m.eigion.pike.api;
  requires com.io7m.eigion.pike;
  requires com.io7m.eigion.preferences;
  requires com.io7m.eigion.protocol.amberjack.cb;
  requires com.io7m.eigion.protocol.amberjack;
  requires com.io7m.eigion.protocol.api;
  requires com.io7m.eigion.protocol.pike.cb;
  requires com.io7m.eigion.protocol.pike;
  requires com.io7m.eigion.server.api;
  requires com.io7m.eigion.server.database.api;
  requires com.io7m.eigion.server.database.postgres;
  requires com.io7m.eigion.server;
  requires com.io7m.eigion.services.api;
  requires com.io7m.eigion.storage.api;
  requires com.io7m.eigion.storage.derby;
  requires com.io7m.eigion.storage.s3;
  requires com.io7m.eigion.taskrecorder;
  requires com.io7m.eigion.tests.arbitraries;
  requires com.io7m.idstore.admin_client.api;
  requires com.io7m.idstore.admin_client;
  requires com.io7m.idstore.database.api;
  requires com.io7m.idstore.database.postgres;
  requires com.io7m.idstore.model;
  requires com.io7m.idstore.protocol.admin.cb;
  requires com.io7m.idstore.protocol.admin;
  requires com.io7m.idstore.protocol.user.cb;
  requires com.io7m.idstore.protocol.user;
  requires com.io7m.idstore.server.api;
  requires com.io7m.idstore.server.security;
  requires com.io7m.idstore.server;
  requires com.io7m.idstore.services.api;
  requires com.io7m.idstore.user_client.api;
  requires com.io7m.idstore.user_client;
  requires com.io7m.jmulticlose.core;
  requires jakarta.mail;
  requires org.apache.derby.client;
  requires org.apache.derby.commons;
  requires org.apache.derby.engine;
  requires org.apache.derby.server;
  requires org.apache.derby.tools;
  requires org.eclipse.jetty.server;
  requires org.eclipse.jetty.servlet;
  requires org.slf4j;
}
