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

package com.io7m.eigion.server.vanilla.internal.public_api;

import com.io7m.eigion.server.database.api.EIServerDatabaseImagesQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseType;
import com.io7m.eigion.server.vanilla.internal.EIHTTPErrorStatusException;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

import static com.io7m.eigion.server.database.api.EIServerDatabaseIncludeRedacted.EXCLUDE_REDACTED;
import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.EIGION;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.NOT_FOUND_404;

/**
 * A servlet for retrieving images.
 */

public final class EIPImageGet extends EIPAuthenticatedServlet
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIPImageGet.class);

  private final EIServerDatabaseType database;

  /**
   * A servlet for retrieving images.
   *
   * @param services The service directory
   */

  public EIPImageGet(
    final EIServiceDirectoryType services)
  {
    super(services);

    this.database =
      services.requireService(EIServerDatabaseType.class);
  }

  @Override
  protected Logger logger()
  {
    return LOG;
  }

  @Override
  protected void serviceAuthenticated(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final HttpSession session)
    throws Exception
  {
    final UUID imageId;
    try {
      imageId =
        Optional.ofNullable(request.getParameter("id"))
          .map(UUID::fromString)
          .orElseThrow(() -> {
            return new EIHTTPErrorStatusException(
              BAD_REQUEST_400,
              "parameter",
              this.strings().format("missingParameter", "id")
            );
          });
    } catch (final IllegalArgumentException e) {
      throw new EIHTTPErrorStatusException(
        BAD_REQUEST_400,
        "parameter",
        this.strings().format("invalidParameter", "ImageID")
      );
    }

    try (var connection =
           this.database.openConnection(EIGION)) {
      try (var transaction =
             connection.openTransaction()) {
        final var images =
          transaction.queries(EIServerDatabaseImagesQueriesType.class);

        final var image =
          images.imageGet(imageId, EXCLUDE_REDACTED)
            .orElseThrow(() -> {
              return new EIHTTPErrorStatusException(
                NOT_FOUND_404,
                "not-found",
                this.strings().format("notFound")
              );
            });

        servletResponse.setContentType(image.contentType());
        servletResponse.setContentLength(image.data().length);
        try (var output = servletResponse.getOutputStream()) {
          output.write(image.data());
        }
      }
    }
  }
}
