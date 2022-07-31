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


package com.io7m.eigion.server.vanilla.internal;

import com.io7m.eigion.domaincheck.api.EIDomainCheckerType;
import com.io7m.eigion.model.EIGroupCreationRequest;
import com.io7m.eigion.server.database.api.EIServerDatabaseGroupsQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseType;
import com.io7m.eigion.services.api.EIServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.EIGION;

/**
 * A service that calls a domain checker, and records the results in the
 * database.
 */

public final class EIServerDomainChecking
  implements EIServiceType, AutoCloseable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIServerDomainChecking.class);

  private final EIDomainCheckerType checker;
  private final EIServerDatabaseType database;

  /**
   * A service that calls a domain checker, and records the results in the
   * database.
   *
   * @param inDatabase The database
   * @param inChecker  The checker
   */

  public EIServerDomainChecking(
    final EIServerDatabaseType inDatabase,
    final EIDomainCheckerType inChecker)
  {
    this.database =
      Objects.requireNonNull(inDatabase, "inDatabase");
    this.checker =
      Objects.requireNonNull(inChecker, "checker");
  }

  @Override
  public String description()
  {
    return "Domain checking service.";
  }

  @Override
  public void close()
    throws Exception
  {
    this.checker.close();
  }

  /**
   * Check a domain request.
   *
   * @param request The request
   */

  public void check(
    final EIGroupCreationRequest request)
  {
    final var future =
      this.checker.check(request);

    future.whenComplete((result, throwable) -> {
      if (throwable != null) {
        this.onRequestException(request, throwable);
      } else {
        this.onRequestCompleted(result);
      }
    });
  }

  private void onRequestCompleted(
    final EIGroupCreationRequest result)
  {
    try (var connection = this.database.openConnection(EIGION)) {
      try (var transaction = connection.openTransaction()) {
        final var groups =
          transaction.queries(EIServerDatabaseGroupsQueriesType.class);
        groups.groupCreationRequestComplete(result);
        transaction.commit();
      }
    } catch (final Exception e) {
      LOG.error("error handling request completion: ", e);
    }
  }

  private void onRequestException(
    final EIGroupCreationRequest request,
    final Throwable throwable)
  {
    LOG.error("error handling request: ", throwable);
  }
}
