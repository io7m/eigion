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

package com.io7m.eigion.server.database.api;

import com.io7m.eigion.model.EIAdmin;
import com.io7m.eigion.model.EIAdminPermission;
import com.io7m.eigion.model.EIAdminSummary;
import com.io7m.eigion.model.EIPassword;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The database queries involving admins.
 */

public non-sealed interface EIServerDatabaseAdminsQueriesType
  extends EIServerDatabaseQueriesType
{
  /**
   * Create an (initial) admin. This method behaves exactly the same as
   * {@link #adminCreate(String, String, EIPassword, Set)} except that it does
   * not require an existing admin account and does not write to the audit log.
   * The method will fail if any admin account already exists,
   *
   * @param id        The admin ID
   * @param created   The creation time
   * @param adminName The admin name
   * @param email     The admin email
   * @param password  The hashed password
   *
   * @return The created admin
   *
   * @throws EIServerDatabaseException On errors
   */

  EIAdmin adminCreateInitial(
    UUID id,
    String adminName,
    String email,
    OffsetDateTime created,
    EIPassword password)
    throws EIServerDatabaseException;

  /**
   * Create an admin.
   *
   * @param adminName   The admin name
   * @param email       The admin email
   * @param password    The hashed password
   * @param permissions The permissions the created admin will have
   *
   * @return The created admin
   *
   * @throws EIServerDatabaseException On errors
   */

  @EIServerDatabaseRequiresAdmin
  default EIAdmin adminCreate(
    final String adminName,
    final String email,
    final EIPassword password,
    final Set<EIAdminPermission> permissions)
    throws EIServerDatabaseException
  {
    return this.adminCreate(
      UUID.randomUUID(),
      adminName,
      email,
      OffsetDateTime.now(),
      password,
      permissions
    );
  }

  /**
   * Create an admin.
   *
   * @param id          The admin ID
   * @param created     The creation time
   * @param adminName   The admin name
   * @param email       The admin email
   * @param password    The hashed password
   * @param permissions The permissions the created admin will have
   *
   * @return The created admin
   *
   * @throws EIServerDatabaseException On errors
   */

  @EIServerDatabaseRequiresAdmin
  EIAdmin adminCreate(
    UUID id,
    String adminName,
    String email,
    OffsetDateTime created,
    EIPassword password,
    Set<EIAdminPermission> permissions)
    throws EIServerDatabaseException;

  /**
   * @param id The admin ID
   *
   * @return A admin with the given ID
   *
   * @throws EIServerDatabaseException On errors
   */

  Optional<EIAdmin> adminGet(UUID id)
    throws EIServerDatabaseException;

  /**
   * @param name The admin name
   *
   * @return A admin with the given name
   *
   * @throws EIServerDatabaseException On errors
   */

  Optional<EIAdmin> adminGetForName(String name)
    throws EIServerDatabaseException;

  /**
   * @param email The admin email
   *
   * @return A admin with the given email
   *
   * @throws EIServerDatabaseException On errors
   */

  Optional<EIAdmin> adminGetForEmail(String email)
    throws EIServerDatabaseException;

  /**
   * Record the fact that the given admin has logged in.
   *
   * @param id   The admin ID
   * @param host The host from which the admin logged in
   *
   * @throws EIServerDatabaseException On errors
   */

  void adminLogin(
    UUID id,
    String host)
    throws EIServerDatabaseException;

  /**
   * Search for admins that have an ID, email, or name that contains the given
   * string, case-insensitive.
   *
   * @param query The admin query
   *
   * @return A list of matching admins
   *
   * @throws EIServerDatabaseException On errors
   */

  List<EIAdminSummary> adminSearch(String query)
    throws EIServerDatabaseException;
}
