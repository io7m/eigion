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


package com.io7m.eigion.server.internal;

import com.io7m.eigion.domaincheck.api.EIDomainCheckerType;
import com.io7m.eigion.model.EIGroupCreationRequest;
import com.io7m.eigion.model.EIGroupCreationRequestStatusType.Succeeded;
import com.io7m.eigion.model.EIGroupRole;
import com.io7m.eigion.server.database.api.EISDatabaseGroupsQueriesType;
import com.io7m.eigion.server.database.api.EISDatabaseType;
import com.io7m.eigion.services.api.EIServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Objects;

import static com.io7m.eigion.server.database.api.EISDatabaseRole.EIGION;

/**
 * A service that calls a domain checker, and records the results in the
 * database.
 */

public final class EISDomainChecking
  implements EIServiceType, AutoCloseable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EISDomainChecking.class);

  private final EIDomainCheckerType checker;
  private final EISDatabaseType database;

  /**
   * A service that calls a domain checker, and records the results in the
   * database.
   *
   * @param inDatabase The database
   * @param inChecker  The checker
   */

  public EISDomainChecking(
    final EISDatabaseType inDatabase,
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
          transaction.queries(EISDatabaseGroupsQueriesType.class);
        groups.groupCreationRequestComplete(result);

        if (result.status() instanceof Succeeded) {
          groups.groupUserUpdate(
            result.groupName(),
            result.userFounder(),
            EnumSet.allOf(EIGroupRole.class)
          );
        }

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

  @Override
  public String toString()
  {
    return "[EISDomainChecking 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode()));
  }
}
