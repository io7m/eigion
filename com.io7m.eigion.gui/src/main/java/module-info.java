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
 * Application runtime management (GUI)
 */

module com.io7m.eigion.gui
{
  requires com.io7m.eigion.client.api;
  requires com.io7m.eigion.client.vanilla;
  requires com.io7m.eigion.news.xml;
  requires com.io7m.eigion.preferences;
  requires com.io7m.jmulticlose.core;
  requires com.io7m.jxtrand.api;
  requires java.xml;
  requires javafx.base;
  requires javafx.controls;
  requires javafx.fxml;
  requires javafx.graphics;
  requires org.slf4j;

  requires transitive com.io7m.eigion.services.api;
  requires transitive com.io7m.jade.api;
  requires transitive com.io7m.jtensors.core;

  opens com.io7m.eigion.gui.internal
    to javafx.fxml;
  opens com.io7m.eigion.gui.internal.errors
    to javafx.fxml;
  opens com.io7m.eigion.gui.internal.login
    to javafx.fxml;
  opens com.io7m.eigion.gui.internal.news
    to javafx.fxml;
  opens com.io7m.eigion.gui.internal.dashboard
    to javafx.fxml;

  exports com.io7m.eigion.gui;
  opens com.io7m.eigion.gui.internal.client to javafx.fxml;
  opens com.io7m.eigion.gui.internal.logo to javafx.fxml;
}
