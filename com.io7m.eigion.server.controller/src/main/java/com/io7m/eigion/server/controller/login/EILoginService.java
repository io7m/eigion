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


package com.io7m.eigion.server.controller.login;

import com.io7m.eigion.model.EIPermission;
import com.io7m.eigion.model.EIPermissionSet;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.model.EIUserLogin;
import com.io7m.eigion.server.controller.EISStrings;
import com.io7m.eigion.server.controller.command_exec.EISCommandExecutionFailure;
import com.io7m.eigion.server.database.api.EISDatabaseException;
import com.io7m.eigion.server.database.api.EISDatabaseTransactionType;
import com.io7m.eigion.server.database.api.EISDatabaseUsersQueriesType;
import com.io7m.eigion.server.http.EIHTTPErrorStatusException;
import com.io7m.eigion.server.service.clock.EISClock;
import com.io7m.eigion.server.service.idstore.EISIdstoreClientsType;
import com.io7m.eigion.server.service.sessions.EISession;
import com.io7m.eigion.server.service.sessions.EISessionServiceType;
import com.io7m.idstore.error_codes.IdStandardErrorCodes;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.user_client.api.IdUClientException;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.IDSTORE_ERROR;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.OPERATION_NOT_PERMITTED;
import static com.io7m.idstore.model.IdLoginMetadataStandard.remoteHostProxied;
import static com.io7m.idstore.model.IdLoginMetadataStandard.userAgent;

/**
 * A service that handles the logic for user logins.
 */

public final class EILoginService implements EILoginServiceType
{
  private final EISClock clock;
  private final EISStrings strings;
  private final EISessionServiceType sessions;
  private final EISIdstoreClientsType idClients;

  /**
   * A service that handles the logic for user logins.
   *
   * @param inClock     The clock
   * @param inStrings   The string resources
   * @param inSessions  A session service
   * @param inIdClients The idstore clients
   */

  public EILoginService(
    final EISClock inClock,
    final EISStrings inStrings,
    final EISessionServiceType inSessions,
    final EISIdstoreClientsType inIdClients)
  {
    this.clock =
      Objects.requireNonNull(inClock, "clock");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.sessions =
      Objects.requireNonNull(inSessions, "inSessions");
    this.idClients =
      Objects.requireNonNull(inIdClients, "inIdClients");
  }

  @Override
  public EISession userLogin(
    final EISDatabaseTransactionType transaction,
    final UUID requestId,
    final String username,
    final String password,
    final Map<String, String> metadata,
    final Set<EIPermission> requirePermissions)
    throws EISCommandExecutionFailure, InterruptedException
  {
    Objects.requireNonNull(transaction, "transaction");
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(username, "username");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(metadata, "metadata");
    Objects.requireNonNull(requirePermissions, "requirePermissions");

    try {
      final var client =
        this.idClients.createClient();

      final var idUser =
        client.login(
          username,
          password,
          this.idClients.baseURI(),
          metadata
        );

      final var eiUser =
        this.checkUser(
          transaction,
          idUser,
          metadata.getOrDefault(remoteHostProxied(), ""),
          metadata.getOrDefault(userAgent(), ""),
          requirePermissions
        );

      return this.sessions.createSession(client, eiUser);
    } catch (final IdUClientException e) {
      final var code = e.errorCode();
      if (Objects.equals(code, IdStandardErrorCodes.AUTHENTICATION_ERROR)) {
        throw new EISCommandExecutionFailure(
          e.getMessage(),
          e,
          requestId,
          401,
          AUTHENTICATION_ERROR
        );
      }

      throw new EISCommandExecutionFailure(
        e.getMessage(),
        e,
        requestId,
        500,
        IDSTORE_ERROR
      );
    } catch (final EIHTTPErrorStatusException e) {
      throw new EISCommandExecutionFailure(
        e.getMessage(),
        e,
        requestId,
        e.statusCode(),
        e.errorCode()
      );
    } catch (final EISDatabaseException e) {
      throw new EISCommandExecutionFailure(
        e.getMessage(),
        e,
        requestId,
        500,
        e.errorCode()
      );
    }
  }

  private EIUser checkUser(
    final EISDatabaseTransactionType transaction,
    final IdUser idUser,
    final String address,
    final String userAgent,
    final Set<EIPermission> permissionsRequired)
    throws EIHTTPErrorStatusException, EISDatabaseException
  {
    final var users =
      transaction.queries(EISDatabaseUsersQueriesType.class);

    final var userOpt =
      users.userGet(idUser.id());

    final var eiUser =
      userOpt.orElseGet(() -> new EIUser(idUser.id(), EIPermissionSet.of()));

    final var permissionsHeld =
      eiUser.permissions().permissions();

    if (!permissionsHeld.containsAll(permissionsRequired)) {
      final var permissionsMissing = new HashSet<>(permissionsRequired);
      permissionsMissing.removeAll(permissionsHeld);
      throw this.operationNotPermitted(permissionsMissing);
    }

    users.userLogin(
      new EIUserLogin(eiUser.id(), this.clock.now(), address, userAgent)
    );

    return eiUser;
  }

  private EIHTTPErrorStatusException operationNotPermitted(
    final Set<EIPermission> permissionsMissing)
  {
    if (permissionsMissing.isEmpty()) {
      return new EIHTTPErrorStatusException(
        401,
        AUTHENTICATION_ERROR,
        this.strings.format("loginFailed")
      );
    }

    return new EIHTTPErrorStatusException(
      403,
      OPERATION_NOT_PERMITTED,
      this.strings.format(
        "errorPermissionsRequired",
        permissionsMissing,
        "Login",
        "access")
    );
  }

  @Override
  public String toString()
  {
    return "[EILoginService 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }

  @Override
  public String description()
  {
    return "User login service.";
  }
}
