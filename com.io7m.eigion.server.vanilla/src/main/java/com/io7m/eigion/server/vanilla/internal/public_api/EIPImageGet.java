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

import com.io7m.eigion.protocol.public_api.v1.EISP1Hash;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseImageGet;
import com.io7m.eigion.server.database.api.EIServerDatabaseImagesQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseType;
import com.io7m.eigion.server.security.EISecPolicyResultDenied;
import com.io7m.eigion.server.security.EISecUserActionImageRead;
import com.io7m.eigion.server.security.EISecurity;
import com.io7m.eigion.server.vanilla.internal.EIHTTPErrorStatusException;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.HTTP_PARAMETER_INVALID;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.HTTP_PARAMETER_NONEXISTENT;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.IMAGE_NONEXISTENT;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.SECURITY_POLICY_DENIED;
import static com.io7m.eigion.server.database.api.EIServerDatabaseIncludeRedacted.EXCLUDE_REDACTED;
import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.EIGION;
import static com.io7m.eigion.server.vanilla.internal.EIServerRequestDecoration.requestIdFor;
import static org.eclipse.jetty.http.HttpStatus.FORBIDDEN_403;

/**
 * A servlet for creating images.
 */

public final class EIPImageGet extends EIPAuthenticatedServlet
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIPImageGet.class);

  private final EIServerDatabaseType database;

  /**
   * A servlet for creating images.
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
    if (EISecurity.check(new EISecUserActionImageRead(this.user()))
      instanceof EISecPolicyResultDenied denied) {
      throw new EIHTTPErrorStatusException(
        FORBIDDEN_403,
        SECURITY_POLICY_DENIED,
        denied.message()
      );
    }

    final var requestId =
      requestIdFor(request);

    final var idRaw =
      request.getParameter("id");

    if (idRaw == null) {
      throw new EIHTTPErrorStatusException(
        400,
        HTTP_PARAMETER_NONEXISTENT,
        this.strings().format("missingParameter", "id")
      );
    }

    final UUID id;
    try {
      id = UUID.fromString(idRaw);
    } catch (final Exception e) {
      throw new EIHTTPErrorStatusException(
        400,
        HTTP_PARAMETER_INVALID,
        this.strings().format("invalidParameter", "id")
      );
    }

    try (var connection =
           this.database.openConnection(EIGION)) {
      try (var transaction =
             connection.openTransaction()) {

        final var images =
          transaction.queries(EIServerDatabaseImagesQueriesType.class);
        final var imageOpt =
          images.imageGet(id, EXCLUDE_REDACTED);

        if (imageOpt.isPresent()) {
          final var image = imageOpt.get();
          this.sends()
            .send(
              servletResponse,
              200,
              new EISP1ResponseImageGet(
                requestId,
                image.id(),
                EISP1Hash.ofHash(image.hash()))
            );
        } else {
          this.sends()
            .sendError(
              servletResponse,
              requestId,
              404,
              IMAGE_NONEXISTENT,
              this.strings().format("notFound")
            );
        }
      }
    }
  }
}
