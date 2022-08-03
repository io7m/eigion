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


package com.io7m.eigion.server.vanilla.internal.admin_api;

import com.io7m.eigion.model.EIAdmin;
import com.io7m.eigion.model.EIPassword;
import com.io7m.eigion.model.EIPasswordException;
import com.io7m.eigion.protocol.admin_api.v1.EISA1Admin;
import com.io7m.eigion.protocol.admin_api.v1.EISA1AdminPermission;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAdminCreate;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseAdminCreate;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseType;
import com.io7m.eigion.server.database.api.EIServerDatabaseAdminsQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.security.EISecAdminActionAdminCreate;
import com.io7m.eigion.server.security.EISecPolicyResultDenied;
import com.io7m.eigion.server.security.EISecurity;
import com.io7m.eigion.server.security.EISecurityException;
import com.io7m.eigion.server.vanilla.internal.EIHTTPErrorStatusException;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandExecutionResult;
import com.io7m.eigion.server.vanilla.internal.command_exec.EICommandExecutorType;

import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.eclipse.jetty.http.HttpStatus.FORBIDDEN_403;

/**
 * A command to create admins.
 */

public final class EIACmdAdminCreate
  implements EICommandExecutorType<EIACommandContext, EISA1CommandAdminCreate, EISA1ResponseType>
{
  /**
   * A command to create admins.
   */

  public EIACmdAdminCreate()
  {

  }

  @Override
  public EICommandExecutionResult<EISA1ResponseType> execute(
    final EIACommandContext context,
    final EISA1CommandAdminCreate command)
    throws
    EIServerDatabaseException,
    EISecurityException,
    EIHTTPErrorStatusException
  {
    final var targetPermissions =
      command.permissions()
        .stream()
        .map(EISA1AdminPermission::toAdmin)
        .collect(Collectors.toUnmodifiableSet());

    if (EISecurity.check(
      new EISecAdminActionAdminCreate(context.admin(), targetPermissions))
      instanceof EISecPolicyResultDenied denied) {
      throw new EIHTTPErrorStatusException(
        FORBIDDEN_403,
        "admin-create",
        denied.message()
      );
    }

    final var transaction =
      context.transaction();
    final var adminQueries =
      transaction.queries(EIServerDatabaseAdminsQueriesType.class);

    transaction.adminIdSet(context.admin().id());

    final EIPassword password;
    try {
      password = command.password().toPassword();
    } catch (final EIPasswordException e) {
      return context.resultError(400, "protocol", e.getMessage());
    }

    final EIAdmin createdAdmin;
    try {
      createdAdmin = adminQueries.adminCreate(
        UUID.randomUUID(),
        command.name(),
        command.email(),
        context.now(),
        password,
        targetPermissions
      );
    } catch (final EIServerDatabaseException e) {
      if (Objects.equals(e.errorCode(), "admin-duplicate-name")) {
        return context.resultError(
          400,
          e.errorCode(),
          e.getMessage()
        );
      }
      throw e;
    }

    return new EICommandExecutionResult<>(
      200,
      new EISA1ResponseAdminCreate(
        context.requestId(),
        EISA1Admin.ofAdmin(createdAdmin)
      )
    );
  }
}
