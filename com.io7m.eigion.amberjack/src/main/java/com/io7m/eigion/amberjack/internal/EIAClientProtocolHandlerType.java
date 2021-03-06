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

package com.io7m.eigion.amberjack.internal;

import com.io7m.eigion.amberjack.api.EIAClientException;
import com.io7m.eigion.model.EIAuditEvent;
import com.io7m.eigion.model.EIService;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.model.EIUserSummary;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * A versioned protocol handler.
 */

public interface EIAClientProtocolHandlerType
{
  /**
   * Attempt to log in.
   *
   * @param user     The user
   * @param password The password
   * @param base     The base URI
   *
   * @return A new protocol handler
   *
   * @throws EIAClientException   On errors
   * @throws InterruptedException On interruption
   */

  EIAClientProtocolHandlerType login(
    String user,
    String password,
    URI base)
    throws EIAClientException, InterruptedException;

  /**
   * List services.
   *
   * @return The services
   *
   * @throws EIAClientException   On errors
   * @throws InterruptedException On interruptions
   */

  List<EIService> services()
    throws EIAClientException, InterruptedException;

  /**
   * Retrieve the user with the given ID.
   *
   * @param id The user
   *
   * @return A user, if one exists
   *
   * @throws EIAClientException   On errors
   * @throws InterruptedException On interruption
   */

  Optional<EIUser> userById(String id)
    throws EIAClientException, InterruptedException;

  /**
   * Retrieve the user with the given name.
   *
   * @param name The user
   *
   * @return A user, if one exists
   *
   * @throws EIAClientException   On errors
   * @throws InterruptedException On interruption
   */

  Optional<EIUser> userByName(String name)
    throws EIAClientException, InterruptedException;

  /**
   * Retrieve the user with the given email.
   *
   * @param email The user email
   *
   * @return A user, if one exists
   *
   * @throws EIAClientException   On errors
   * @throws InterruptedException On interruption
   */

  Optional<EIUser> userByEmail(String email)
    throws EIAClientException, InterruptedException;

  /**
   * Search users.
   *
   * @param query The search query
   *
   * @return The matching users
   *
   * @throws EIAClientException   On errors
   * @throws InterruptedException On interruption
   */

  List<EIUserSummary> userSearch(String query)
    throws EIAClientException, InterruptedException;

  /**
   * Retrieve audit events within the given time range.
   *
   * @param dateLower The lower date bound
   * @param dateUpper The upper date bound
   *
   * @return The audit events
   *
   * @throws EIAClientException   On errors
   * @throws InterruptedException On interruption
   */

  List<EIAuditEvent> auditGetByTime(
    OffsetDateTime dateLower,
    OffsetDateTime dateUpper)
    throws EIAClientException, InterruptedException;
}
