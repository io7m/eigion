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

import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIPassword;
import com.io7m.eigion.model.EIPasswordException;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserCreate;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseUserCreate;
import com.io7m.eigion.protocol.admin_api.v1.EISA1User;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseGroupsQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseUsersQueriesType;
import com.io7m.eigion.server.security.EISecActionUserCreate;
import com.io7m.eigion.server.security.EISecPolicyResultDenied;
import com.io7m.eigion.server.security.EISecurity;
import com.io7m.eigion.server.security.EISecurityException;
import com.io7m.eigion.server.vanilla.internal.EIHTTPErrorStatusException;
import com.io7m.eigion.server.vanilla.internal.EIServerConfigurations;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.io7m.eigion.model.EIGroupRole.FOUNDER;
import static org.eclipse.jetty.http.HttpStatus.FORBIDDEN_403;

/**
 * A command to create users.
 */

public final class EIACmdUserCreate
  implements EIACommandExecutorType<EISA1CommandUserCreate>
{
  /**
   * A command to create users.
   */

  public EIACmdUserCreate()
  {

  }

  @Override
  public EIACommandExecutionResult execute(
    final EIACommandContext context,
    final EISA1CommandUserCreate command)
    throws
    EIServerDatabaseException,
    EISecurityException,
    EIHTTPErrorStatusException
  {
    if (EISecurity.check(new EISecActionUserCreate(context.admin()))
      instanceof EISecPolicyResultDenied denied) {
      throw new EIHTTPErrorStatusException(
        FORBIDDEN_403,
        "user-create",
        denied.message()
      );
    }

    final var userGroupPrefix =
      context.services()
        .requireService(EIServerConfigurations.class)
        .configuration()
        .userGroupPrefix();

    final var transaction =
      context.transaction();
    final var userQueries =
      transaction.queries(EIServerDatabaseUsersQueriesType.class);
    final var groupQueries =
      transaction.queries(EIServerDatabaseGroupsQueriesType.class);

    transaction.adminIdSet(context.admin().id());

    final EIPassword password;
    try {
      password = command.password().toPassword();
    } catch (final EIPasswordException e) {
      return context.resultError(400, "protocol", e.getMessage());
    }

    final EIUser createdUser;
    try {
      createdUser = userQueries.userCreate(
        UUID.randomUUID(),
        command.name(),
        command.email(),
        context.now(),
        password
      );
    } catch (final EIServerDatabaseException e) {
      if (Objects.equals(e.errorCode(), "user-duplicate-name")) {
        return context.resultError(
          400,
          e.errorCode(),
          e.getMessage()
        );
      }
      throw e;
    }

    final var gid =
      Math.addExact(groupQueries.groupIdentifierLast(), 1L);

    final EIGroupName name;
    try {
      name = userGroupPrefix.toGroupName(gid);
    } catch (final IllegalArgumentException e) {
      return context.resultError(
        500,
        "group-name-invalid",
        e.getMessage()
      );
    }

    groupQueries.groupCreate(name, createdUser.id());
    groupQueries.groupMembershipSet(name, createdUser.id(), Set.of(FOUNDER));

    final var userWithGroups =
      new EIUser(
        createdUser.id(),
        createdUser.name(),
        createdUser.email(),
        createdUser.created(),
        createdUser.lastLoginTime(),
        createdUser.password(),
        createdUser.ban(),
        Map.of(name, Set.of(FOUNDER))
      );

    return new EIACommandExecutionResult(
      200,
      new EISA1ResponseUserCreate(
        context.requestId(),
        EISA1User.ofUser(userWithGroups)
      )
    );
  }
}
