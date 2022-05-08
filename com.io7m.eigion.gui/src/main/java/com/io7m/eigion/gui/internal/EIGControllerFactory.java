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


package com.io7m.eigion.gui.internal;

import com.io7m.eigion.gui.EIGConfiguration;
import com.io7m.eigion.gui.internal.dashboard.EIGDashboardController;
import com.io7m.eigion.gui.internal.login.EIGLoginScreenController;
import com.io7m.eigion.gui.internal.logo.EIGLogoController;
import com.io7m.eigion.gui.internal.news.EIGNewsController;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import javafx.util.Callback;

import java.util.Objects;

/**
 * The main factory of controllers for the UI.
 */

public final class EIGControllerFactory
  implements Callback<Class<?>, Object>
{
  private final EIServiceDirectoryType services;
  private final EIGConfiguration configuration;

  /**
   * The main factory of controllers for the UI.
   *
   * @param inServices      The service directory
   * @param inConfiguration The UI configuration
   */

  public EIGControllerFactory(
    final EIServiceDirectoryType inServices,
    final EIGConfiguration inConfiguration)
  {
    this.services =
      Objects.requireNonNull(inServices, "services");
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
  }

  @Override
  public Object call(
    final Class<?> param)
  {
    return switch (param.getCanonicalName()) {
      case "com.io7m.eigion.gui.internal.logo.EIGLogoController" -> {
        yield new EIGLogoController(this.services, this.configuration);
      }
      case "com.io7m.eigion.gui.internal.EIGMainController" -> {
        yield new EIGMainController(this.services, this.configuration);
      }
      case "com.io7m.eigion.gui.internal.login.EIGLoginScreenController" -> {
        yield new EIGLoginScreenController(this.services);
      }
      case "com.io7m.eigion.gui.internal.news.EIGNewsController" -> {
        yield new EIGNewsController(this.services);
      }
      case "com.io7m.eigion.gui.internal.dashboard.EIGDashboardController" -> {
        yield new EIGDashboardController(this.services, this.configuration);
      }
      default -> {
        throw new IllegalStateException(
          "Unrecognized controller: %s".formatted(param.getCanonicalName())
        );
      }
    };
  }
}
