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

import com.io7m.eigion.model.EIProductIdentifier;
import com.io7m.eigion.server.database.api.EIServerDatabaseProductsQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseType;
import com.io7m.eigion.server.protocol.public_api.v1.EISP1ResponseProductList;
import com.io7m.eigion.server.vanilla.internal.EIHTTPErrorStatusException;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.text.ParseException;
import java.util.Optional;

import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.EIGION;
import static com.io7m.eigion.server.vanilla.internal.EIServerRequestIDs.requestIdFor;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;

/**
 * A servlet for listing products.
 */

public final class EIPProducts extends EIPAuthenticatedServlet
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIPProducts.class);

  private final EIServerDatabaseType database;
  private final EIPSends sends;

  /**
   * A servlet for listing products.
   *
   * @param services The service directory
   */

  public EIPProducts(
    final EIServiceDirectoryType services)
  {
    super(services);

    this.database =
      services.requireService(EIServerDatabaseType.class);
    this.sends =
      services.requireService(EIPSends.class);
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
    final var startOpt =
      Optional.ofNullable(request.getParameter("start"));

    Optional<EIProductIdentifier> idOpt = Optional.empty();
    if (startOpt.isPresent()) {
      try {
        idOpt = Optional.of(EIProductIdentifier.parse(startOpt.get()));
      } catch (final ParseException e) {
        throw new EIHTTPErrorStatusException(
          BAD_REQUEST_400,
          "parameter",
          this.strings().format("invalidParameter", "start")
        );
      }
    }

    try (var connection =
           this.database.openConnection(EIGION)) {
      try (var transaction =
             connection.openTransaction()) {
        final var products =
          transaction.queries(EIServerDatabaseProductsQueriesType.class);
        final var productPage =
          products.productSummaries(idOpt, BigInteger.valueOf(100L));

        this.sends.send(
          servletResponse,
          200,
          new EISP1ResponseProductList(
            requestIdFor(request),
            productPage.items())
        );
      }
    }
  }
}
