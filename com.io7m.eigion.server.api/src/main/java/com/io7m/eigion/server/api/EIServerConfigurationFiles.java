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

package com.io7m.eigion.server.api;

import com.io7m.eigion.server.api.xml.Configuration;
import com.io7m.eigion.server.api.xml.Database;
import com.io7m.eigion.server.api.xml.DatabaseKind;
import com.io7m.eigion.server.api.xml.HTTPService;
import com.io7m.eigion.server.api.xml.IdStore;
import com.io7m.eigion.server.api.xml.OpenTelemetry;
import com.io7m.eigion.services.api.EIServiceType;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.datatype.Duration;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * The configuration file parser.
 */

public final class EIServerConfigurationFiles
  implements EIServiceType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIServerConfigurationFiles.class);

  /**
   * The configuration file parser.
   */

  public EIServerConfigurationFiles()
  {

  }

  /**
   * Parse a configuration file.
   *
   * @param source The URI of the source file, for error messages
   * @param stream The input stream
   *
   * @return The file
   *
   * @throws IOException On errors
   */

  public EIServerConfigurationFile parse(
    final URI source,
    final InputStream stream)
    throws IOException
  {
    try {
      final var schemas =
        SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      final var schema =
        schemas.newSchema(
          EIServerConfigurationFiles.class.getResource(
            "/com/io7m/eigion/server/api/configuration-1.xsd")
        );

      final var context =
        JAXBContext.newInstance("com.io7m.eigion.server.api.xml");
      final var unmarshaller =
        context.createUnmarshaller();

      unmarshaller.setEventHandler(event -> {
        LOG.error(
          "{}:{}:{}: {}",
          event.getLocator().getURL(),
          Integer.valueOf(event.getLocator().getLineNumber()),
          Integer.valueOf(event.getLocator().getColumnNumber()),
          event.getMessage()
        );
        return true;
      });
      unmarshaller.setSchema(schema);

      final var streamSource =
        new StreamSource(stream, source.toString());
      final var configuration =
        (Configuration) unmarshaller.unmarshal(streamSource);

      return processConfiguration(configuration);
    } catch (final JAXBException | URISyntaxException | SAXException e) {
      throw new IOException(e);
    }
  }

  private static EIServerConfigurationFile processConfiguration(
    final Configuration configuration)
    throws URISyntaxException
  {
    return new EIServerConfigurationFile(
      new EIServerHTTPConfiguration(
        processAmberjack(configuration.getAmberjackService()),
        processPike(configuration.getPikeService())
      ),
      processDatabase(configuration.getDatabase()),
      processIdstore(configuration.getIdStore()),
      processOpenTelemetry(configuration.getOpenTelemetry())
    );
  }

  private static Optional<EIServerOpenTelemetryConfiguration> processOpenTelemetry(
    final OpenTelemetry openTelemetry)
    throws URISyntaxException
  {
    if (openTelemetry == null) {
      return Optional.empty();
    }

    return Optional.of(
      new EIServerOpenTelemetryConfiguration(
        openTelemetry.getLogicalServiceName(),
        new URI(openTelemetry.getCollectorAddress())
      )
    );
  }

  private static EIServerIdstoreConfiguration processIdstore(
    final IdStore idStore)
    throws URISyntaxException
  {
    return new EIServerIdstoreConfiguration(
      new URI(idStore.getBaseURI()),
      new URI(idStore.getPasswordResetURI())
    );
  }

  private static EIServerDatabaseConfiguration processDatabase(
    final Database database)
  {
    return new EIServerDatabaseConfiguration(
      processDatabaseKind(database.getKind()),
      database.getUser(),
      database.getPassword(),
      database.getDatabaseAddress(),
      database.getDatabasePort().intValue(),
      database.getDatabaseName(),
      database.isCreate(),
      database.isUpgrade()
    );
  }

  private static EIServerDatabaseKind processDatabaseKind(
    final DatabaseKind kind)
  {
    return switch (kind) {
      case POSTGRESQL -> EIServerDatabaseKind.POSTGRESQL;
    };
  }

  private static EIServerHTTPServiceConfiguration processPike(
    final HTTPService service)
    throws URISyntaxException
  {
    return new EIServerHTTPServiceConfiguration(
      service.getListenAddress(),
      service.getListenPort().intValue(),
      new URI(service.getExternalAddress()),
      processDuration(service.getSessionExpiration())
    );
  }

  private static EIServerHTTPServiceConfiguration processAmberjack(
    final HTTPService service)
    throws URISyntaxException
  {
    return new EIServerHTTPServiceConfiguration(
      service.getListenAddress(),
      service.getListenPort().intValue(),
      new URI(service.getExternalAddress()),
      processDuration(service.getSessionExpiration())
    );
  }

  private static Optional<java.time.Duration> processDuration(
    final Duration d)
  {
    if (d == null) {
      return Optional.empty();
    }

    var base = java.time.Duration.ofSeconds(0L);
    base = base.plusDays((long) d.getDays());
    base = base.plusHours((long) d.getHours());
    base = base.plusMinutes((long) d.getMinutes());
    base = base.plusSeconds((long) d.getSeconds());
    return Optional.of(base);
  }

  /**
   * Parse a configuration file.
   *
   * @param file The input file
   *
   * @return The file
   *
   * @throws IOException On errors
   */

  public EIServerConfigurationFile parse(
    final Path file)
    throws IOException
  {
    try (var stream = Files.newInputStream(file)) {
      return this.parse(file.toUri(), stream);
    }
  }

  @Override
  public String description()
  {
    return "Server configuration elements.";
  }

  @Override
  public String toString()
  {
    return "[EIServerConfigurationFiles 0x%s]"
      .formatted(Long.toUnsignedString((long) this.hashCode(), 16));
  }
}
