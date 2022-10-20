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

package com.io7m.eigion.server.internal.pike.security;

import com.io7m.eigion.model.EIPermission;
import com.io7m.eigion.model.EIPermissionSet;
import com.io7m.eigion.server.internal.security.EISecPolicyResultDenied;
import com.io7m.eigion.server.internal.security.EISecPolicyResultPermitted;
import com.io7m.eigion.server.internal.security.EISecPolicyResultType;
import com.io7m.eigion.server.internal.security.EISecPolicyType;

import java.util.Objects;

/**
 * The Pike security policy.
 */

public final class EISecPPolicy implements EISecPolicyType<EISecPActionType>
{
  private static final EISecPolicyType<EISecPActionType> POLICY =
    new EISecPPolicy();

  /**
   * The Pike security policy.
   */

  private EISecPPolicy()
  {

  }

  /**
   * @return The Pike security policy.
   */

  public static EISecPolicyType<EISecPActionType> policy()
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

  @Override
  public EISecPolicyResultType evaluate(
    final EISecPActionType action)
  {
    Objects.requireNonNull(action, "action");

    return new EISecPolicyResultDenied("Operation not permitted.");
  }
}
