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

package com.io7m.eigion.launcher.api;

import com.io7m.jdeferthrow.core.ExceptionTracker;
import com.io7m.jproperties.JProperties;
import com.io7m.jproperties.JPropertyNonexistent;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * The configuration parameters for a launcher.
 */

public final class EILauncherConfiguration
{
  private final Path runtimeDirectory;
  private final List<Path> javaModules;
  private final List<Path> osgiBundles;
  private final Map<String, String> parameters;

  private EILauncherConfiguration(
    final Path inRuntimeDirectory,
    final List<Path> inJavaModules,
    final List<Path> inOsgiBundles,
    final Map<String, String> inParameters)
  {
    this.runtimeDirectory =
      Objects.requireNonNull(inRuntimeDirectory, "runtimeDirectory");
    this.javaModules =
      Objects.requireNonNull(inJavaModules, "javaModules");
    this.osgiBundles =
      Objects.requireNonNull(inOsgiBundles, "osgiBundles");
    this.parameters =
      Objects.requireNonNull(inParameters, "inParameters");
  }

  /**
   * Export the current configuration to a set of properties.
   *
   * @return The properties
   */

  public Properties toProperties()
  {
    final var props = new Properties();
    for (final var entry : this.parameters.entrySet()) {
      props.setProperty(entry.getKey(), entry.getValue());
    }

    props.setProperty(
      "eigion.runtimeDirectory",
      this.runtimeDirectory.toString()
    );

    for (int index = 0; index < this.javaModules.size(); ++index) {
      final var key =
        String.format("eigion.modules.%d", Integer.valueOf(index));
      props.setProperty(key, this.javaModules.get(index).toString());
    }

    for (int index = 0; index < this.osgiBundles.size(); ++index) {
      final var key =
        String.format("eigion.bundles.%d", Integer.valueOf(index));
      props.setProperty(key, this.osgiBundles.get(index).toString());
    }

    return props;
  }

  /**
   * @return The run-time directory used for the OSGi container
   */

  public Path runtimeDirectory()
  {
    return this.runtimeDirectory;
  }

  /**
   * @return The list of Java modules that will be installed
   */

  public List<Path> javaModules()
  {
    return this.javaModules;
  }

  /**
   * @return The list of OSGi bundles that will be installed
   */

  public List<Path> osgiBundles()
  {
    return this.osgiBundles;
  }

  /**
   * @return The extra configuration parameters
   */

  public Map<String, String> parameters()
  {
    return this.parameters;
  }

  /**
   * Parse the given file.
   *
   * @param file The file
   *
   * @return A parsed configuration
   *
   * @throws IOException On errors
   */

  public static EILauncherConfiguration parseFile(
    final Path file)
    throws IOException
  {
    final var filesystem =
      file.getFileSystem();

    final var properties = new Properties();
    try (var stream = Files.newInputStream(file)) {
      properties.load(stream);
    }

    final var exceptions =
      new ExceptionTracker<IOException>();

    final var builder =
      new EILauncherConfiguration.Builder(filesystem.getPath(""));

    loadRuntimeDirectory(exceptions, filesystem, properties, builder);
    loadModules(filesystem, properties, builder);
    loadBundles(filesystem, properties, builder);
    builder.addParameters(toMap(properties));

    exceptions.throwIfNecessary();
    return builder.build();
  }

  private static void loadRuntimeDirectory(
    final ExceptionTracker<IOException> exceptions,
    final FileSystem filesystem,
    final Properties properties,
    final Builder builder)
  {
    try {
      builder.setRuntimeDirectory(
        filesystem.getPath(
          JProperties.getString(properties, "eigion.runtimeDirectory")
        )
      );
    } catch (final JPropertyNonexistent e) {
      exceptions.addException(new IOException(e));
    }
  }

  private static Map<String, String> toMap(
    final Properties properties)
  {
    final var map = new HashMap<String, String>();
    for (final var e : properties.entrySet()) {
      map.put((String) e.getKey(), (String) e.getValue());
    }
    return map;
  }

  private static void loadModules(
    final FileSystem filesystem,
    final Properties properties,
    final Builder builder)
  {
    for (int index = 0; index < Integer.MAX_VALUE; ++index) {
      final var key =
        String.format("eigion.modules.%d", Integer.valueOf(index));
      final var value =
        properties.getProperty(key);

      if (value == null) {
        break;
      }

      final var file =
        filesystem.getPath(value)
          .toAbsolutePath()
          .normalize();

      builder.addJavaModule(file);
    }
  }

  private static void loadBundles(
    final FileSystem filesystem,
    final Properties properties,
    final Builder builder)
  {
    for (int index = 0; index < Integer.MAX_VALUE; ++index) {
      final var key =
        String.format("eigion.bundles.%d", Integer.valueOf(index));
      final var value =
        properties.getProperty(key);

      if (value == null) {
        break;
      }

      final var file =
        filesystem.getPath(value)
          .toAbsolutePath()
          .normalize();

      builder.addOSGIBundle(file);
    }
  }

  /**
   * Create a new builder.
   *
   * @param runtimeDirectory The runtime directory
   *
   * @return A new builder
   */

  public static Builder builder(
    final Path runtimeDirectory)
  {
    return new Builder(runtimeDirectory);
  }

  /**
   * A mutable configuration builder.
   */

  public static final class Builder
  {
    private final ArrayList<Path> javaModules;
    private final ArrayList<Path> osgiBundles;
    private final HashMap<String, String> parameters;
    private Path runtimeDirectory;

    /**
     * Create a new builder.
     *
     * @param inRuntimeDirectory The runtime directory
     */

    public Builder(
      final Path inRuntimeDirectory)
    {
      this.runtimeDirectory =
        Objects.requireNonNull(inRuntimeDirectory, "runtimeDirectory")
          .toAbsolutePath();

      this.javaModules = new ArrayList<>();
      this.osgiBundles = new ArrayList<>();
      this.parameters = new HashMap<>();
    }

    /**
     * Add extra configuration parameters.
     *
     * @param inParameters The parameters
     *
     * @return this
     */

    public Builder addParameters(
      final Map<String, String> inParameters)
    {
      this.parameters.putAll(
        Objects.requireNonNull(inParameters, "parameters"));
      return this;
    }

    /**
     * Add a Java module.
     *
     * @param path The path
     *
     * @return this
     */

    public Builder addJavaModule(
      final Path path)
    {
      this.javaModules.add(
        Objects.requireNonNull(path, "path")
          .toAbsolutePath()
      );
      return this;
    }

    /**
     * Add an OSGi bundle.
     *
     * @param path The path
     *
     * @return this
     */

    public Builder addOSGIBundle(
      final Path path)
    {
      this.osgiBundles.add(
        Objects.requireNonNull(path, "path")
          .toAbsolutePath()
      );
      return this;
    }

    /**
     * @return The configuration
     */

    public EILauncherConfiguration build()
    {
      return new EILauncherConfiguration(
        this.runtimeDirectory,
        List.copyOf(this.javaModules),
        List.copyOf(this.osgiBundles),
        Map.copyOf(this.parameters)
      );
    }

    /**
     * Set the run-time directory.
     *
     * @param path The path
     *
     * @return this
     */

    public Builder setRuntimeDirectory(
      final Path path)
    {
      this.runtimeDirectory =
        Objects.requireNonNull(path, "path")
          .toAbsolutePath();
      return this;
    }
  }
}
