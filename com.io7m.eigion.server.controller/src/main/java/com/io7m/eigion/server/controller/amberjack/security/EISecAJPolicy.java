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


package com.io7m.eigion.server.controller.amberjack.security;

import com.io7m.eigion.model.EIPermission;
import com.io7m.eigion.model.EIPermissionSet;
import com.io7m.eigion.server.security.EISecPolicyResultDenied;
import com.io7m.eigion.server.security.EISecPolicyResultPermitted;
import com.io7m.eigion.server.security.EISecPolicyResultType;
import com.io7m.eigion.server.security.EISecPolicyType;

import java.util.Objects;

import static com.io7m.eigion.model.EIPermission.AUDIT_READ;
import static com.io7m.eigion.model.EIPermission.GROUP_CREATE;
import static com.io7m.eigion.model.EIPermission.GROUP_READ;

/**
 * The Amberjack security policy.
 */

public final class EISecAJPolicy implements EISecPolicyType<EISecAJActionType>
{
  private static final EISecPolicyType<EISecAJActionType> POLICY =
    new EISecAJPolicy();

  /**
   * The Amberjack security policy.
   */

  private EISecAJPolicy()
  {

  }

  /**
   * @return The Amberjack security policy.
   */

  public static EISecPolicyType<EISecAJActionType> policy()
  {
    return POLICY;
  }

  private static EISecPolicyResultType checkOwnedPermission(
    final EIPermissionSet ownedPermissions,
    final EIPermission userRead)
  {
    if (ownedPermissions.implies(userRead)) {
      return new EISecPolicyResultPermitted();
    }
    return new EISecPolicyResultDenied(
      "You do not have the %s permission.".formatted(userRead)
    );
  }

  private static EISecPolicyResultType checkAuditRead(
    final EISecAJActionAuditRead a)
  {
    return checkOwnedPermission(a.user().permissions(), AUDIT_READ);
  }

  private static EISecPolicyResultType checkGroupCreate(
    final EISecAJActionGroupCreate a)
  {
    return checkOwnedPermission(a.user().permissions(), GROUP_CREATE);
  }

  private static EISecPolicyResultType checkGroupSearch(
    final EISecAJActionGroupSearch a)
  {
    return checkOwnedPermission(a.user().permissions(), GROUP_READ);
  }

  @Override
  public EISecPolicyResultType evaluate(
    final EISecAJActionType action)
  {
    Objects.requireNonNull(action, "action");

    if (action instanceof EISecAJActionAuditRead a) {
      return checkAuditRead(a);
    }
    if (action instanceof EISecAJActionGroupCreate a) {
      return checkGroupCreate(a);
    }
    if (action instanceof EISecAJActionGroupSearch a) {
      return checkGroupSearch(a);
    }

    return new EISecPolicyResultDenied("Operation not permitted.");
  }
}
