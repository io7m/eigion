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


package com.io7m.eigion.amberjack.api;

import com.io7m.eigion.model.EIAdmin;
import com.io7m.eigion.model.EIAdminPermission;
import com.io7m.eigion.model.EIAdminSummary;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Commands related to admins.
 */

public interface EIAClientAdminType
{
  /**
   * Retrieve the admin with the given ID.
   *
   * @param id The admin
   *
   * @return A admin, if one exists
   *
   * @throws EIAClientException   On errors
   * @throws InterruptedException On interruption
   */

  Optional<EIAdmin> adminById(String id)
    throws EIAClientException, InterruptedException;

  /**
   * Retrieve the admin with the given name.
   *
   * @param name The admin
   *
   * @return A admin, if one exists
   *
   * @throws EIAClientException   On errors
   * @throws InterruptedException On interruption
   */

  Optional<EIAdmin> adminByName(String name)
    throws EIAClientException, InterruptedException;

  /**
   * Retrieve the admin with the given email.
   *
   * @param email The admin email
   *
   * @return A admin, if one exists
   *
   * @throws EIAClientException   On errors
   * @throws InterruptedException On interruption
   */

  Optional<EIAdmin> adminByEmail(String email)
    throws EIAClientException, InterruptedException;

  /**
   * Search admins.
   *
   * @param query The search query
   *
   * @return The matching admins
   *
   * @throws EIAClientException   On errors
   * @throws InterruptedException On interruption
   */

  List<EIAdminSummary> adminSearch(String query)
    throws EIAClientException, InterruptedException;

  /**
   * Create a new admin.
   *
   * @param name        The admin name
   * @param email       The admin email
   * @param password    The password
   * @param permissions The admin permissions
   *
   * @return The created admin
   *
   * @throws EIAClientException   On errors
   * @throws InterruptedException On interruption
   */

  EIAdmin adminCreate(
    String name,
    String email,
    String password,
    Set<EIAdminPermission> permissions)
    throws EIAClientException, InterruptedException;
}
