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


package com.io7m.eigion.gui.internal.main;

import com.io7m.eigion.gui.EIGConfiguration;
import com.io7m.eigion.gui.internal.EIGApplication;
import com.io7m.eigion.gui.internal.EIGStrings;
import com.io7m.eigion.gui.internal.dashboard.EIGDashboardController;
import com.io7m.eigion.gui.internal.login.EIGLoginScreenController;
import com.io7m.eigion.gui.internal.splash.EIGSplashController;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import static javafx.animation.Interpolator.EASE_BOTH;

/**
 * The main screen controller that handles transitions between screens.
 */

public final class EIGMainScreenController implements Initializable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIGMainScreenController.class);

  private final EIGConfiguration configuration;
  private final EIGStrings strings;

  @FXML private StackPane mainContainer;

  private ObservableList<Node> children;
  private volatile Pane latestPane;

  /**
   * The main screen controller that handles transitions between screens.
   *
   * @param inConfiguration The application configuration
   * @param inStrings       The application strings
   */

  public EIGMainScreenController(
    final EIGConfiguration inConfiguration,
    final EIGStrings inStrings)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    this.children =
      this.mainContainer.getChildren();
  }

  private Pane openScreen(
    final String name,
    final Supplier<?> constructor)
  {
    try {
      final var xml =
        EIGApplication.class.getResource(
          "/com/io7m/eigion/gui/internal/" + name);
      Objects.requireNonNull(xml, "xml");

      final var loader =
        new FXMLLoader(xml, this.strings.resources());

      loader.setControllerFactory(ignored -> constructor.get());

      final Pane pane = loader.load();
      this.configuration.customCSS()
        .ifPresent(customCSS -> {
          pane.getStylesheets().add(customCSS.toString());
        });
      return pane;
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Pane openScreenWithServices(
    final EIServiceDirectoryType services,
    final Class<? extends EIScreenControllerWithServicesType> clazz,
    final String name)
  {
    try {
      final var xml =
        EIGApplication.class.getResource(
          "/com/io7m/eigion/gui/internal/" + name);
      Objects.requireNonNull(xml, "xml");

      final var loader =
        new FXMLLoader(xml, this.strings.resources());

      final var controllers =
        new EIGMainControllerFactory(
          this,
          services,
          this.configuration
        );

      loader.setControllerFactory(controllers);

      final Pane pane = loader.load();
      this.configuration.customCSS()
        .ifPresent(css -> pane.getStylesheets().add(css.toString()));

      clazz.cast(loader.getController());
      return pane;
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Open the splash screen.
   *
   * @param transition The desired transition
   */

  public void openSplashScreen(
    final EIGScreenTransition transition)
  {
    final var newPane =
      this.openScreen("splash.fxml", () -> {
        return new EIGSplashController(
          this,
          this.configuration,
          this.strings
        );
      });

    this.doTransition(newPane, transition);
  }

  private void doTransition(
    final Pane newPane,
    final EIGScreenTransition transition)
  {
    Platform.runLater(() -> {
      LOG.debug("transition {} -> {}", this.latestPane, newPane);

      switch (transition) {
        case IMMEDIATE -> {
          this.children.setAll(List.of(newPane));
          this.latestPane = newPane;
        }

        case WIPE -> {
          final var previousPane = this.latestPane;

          final var s = this.mainContainer.getScene();
          final var w = s.getWidth();
          final var h = s.getHeight();

          final var targetRectangle =
            new Rectangle(w, h);
          final var sourceRectangle =
            new Rectangle(w, h);

          final var line = new Rectangle(2.0, h);
          line.setFill(Color.WHITE);

          final var transitionTime =
            Duration.millis(500L);

          final var targetClipTrans = new TranslateTransition(transitionTime);
          targetClipTrans.setNode(targetRectangle);
          targetClipTrans.setFromX(w);
          targetClipTrans.setToX(0.0);
          targetClipTrans.setInterpolator(EASE_BOTH);

          final var sourceClipTrans = new TranslateTransition(transitionTime);
          sourceClipTrans.setNode(sourceRectangle);
          sourceClipTrans.setFromX(0.0);
          sourceClipTrans.setToX(-w);
          sourceClipTrans.setInterpolator(EASE_BOTH);

          final var lineTransition = new TranslateTransition(transitionTime);
          lineTransition.setNode(line);
          lineTransition.setFromX(w);
          lineTransition.setToX(0.0);
          lineTransition.setInterpolator(EASE_BOTH);
          lineTransition.setOnFinished(ignored -> {
            this.children.remove(line);
            if (previousPane != null) {
              this.children.remove(previousPane);
            }
            this.latestPane = newPane;
            newPane.setClip(null);
          });

          this.children.add(newPane);
          this.children.add(line);

          if (previousPane != null) {
            previousPane.setClip(sourceRectangle);
          }
          newPane.setClip(targetRectangle);

          sourceClipTrans.play();
          targetClipTrans.play();
          lineTransition.play();
        }
      }
    });
  }

  /**
   * Open the login screen.
   *
   * @param services   The services
   * @param transition The desired transition
   */

  public void openLoginScreen(
    final EIServiceDirectoryType services,
    final EIGScreenTransition transition)
  {
    this.doTransition(
      this.openScreenWithServices(
        services,
        EIGLoginScreenController.class,
        "login.fxml"
      ), transition
    );
  }

  /**
   * Open the dashboard screen.
   *
   * @param services   The services
   * @param transition The desired transition
   */

  public void openDashboard(
    final EIServiceDirectoryType services,
    final EIGScreenTransition transition)
  {
    this.doTransition(
      this.openScreenWithServices(
        services,
        EIGDashboardController.class,
        "dashboard.fxml"
      ), transition
    );
  }
}
