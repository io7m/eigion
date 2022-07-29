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

package com.io7m.eigion.server.security;

import com.io7m.eigion.model.EIAdminPermission;

import java.util.Objects;
import java.util.Set;

import static com.io7m.eigion.model.EIAdminPermission.ADMIN_CREATE;
import static com.io7m.eigion.model.EIAdminPermission.ADMIN_READ;
import static com.io7m.eigion.model.EIAdminPermission.AUDIT_READ;
import static com.io7m.eigion.model.EIAdminPermission.SERVICE_READ;
import static com.io7m.eigion.model.EIAdminPermission.USER_READ;
import static com.io7m.eigion.model.EIAdminPermission.USER_WRITE;

/**
 * The default security policy.
 */

public final class EISecPolicyDefault implements EISecPolicyType
{
  private static final EISecPolicyDefault INSTANCE =
    new EISecPolicyDefault();

  private EISecPolicyDefault()
  {

  }

  /**
   * @return A reference to this policy
   */

  public static EISecPolicyType get()
  {
    return INSTANCE;
  }

  private static EISecPolicyResultType checkAdminRead(
    final EISecActionAdminRead c)
  {
    return checkOwnedPermission(c.admin().permissions(), ADMIN_READ);
  }

  private static EISecPolicyResultType checkServicesRead(
    final EISecActionServicesRead c)
  {
    return checkOwnedPermission(c.admin().permissions(), SERVICE_READ);
  }

  private static EISecPolicyResultType checkUserRead(
    final EISecActionUserRead c)
  {
    return checkOwnedPermission(c.admin().permissions(), USER_READ);
  }

  private static EISecPolicyResultType checkOwnedPermission(
    final Set<EIAdminPermission> ownedPermissions,
    final EIAdminPermission userRead)
  {
    if (ownedPermissions.contains(userRead)) {
      return new EISecPolicyResultPermitted();
    }
    return new EISecPolicyResultDenied(
      "You do not have the %s permission.".formatted(userRead)
    );
  }

  private static EISecPolicyResultType checkUserCreate(
    final EISecActionUserCreate c)
  {
    return checkOwnedPermission(c.admin().permissions(), USER_WRITE);
  }

  private static EISecPolicyResultType checkAuditGet(
    final EISecActionAuditRead c)
  {
    return checkOwnedPermission(c.admin().permissions(), AUDIT_READ);
  }

  private static EISecPolicyResultType checkUserUserComplaintCreate(
    final EISecActionUserUserComplaintCreate c)
  {
    return new EISecPolicyResultPermitted();
  }

  private static EISecPolicyResultType checkActionImageRead(
    final EISecActionImageRead a)
  {
    return new EISecPolicyResultPermitted();
  }

  private static EISecPolicyResultType checkActionImageCreate(
    final EISecActionImageCreate a)
  {
    return new EISecPolicyResultPermitted();
  }

  private static EISecPolicyResultType checkUserAction(
    final EISecActionUserType action)
  {
    if (action instanceof EISecActionImageRead a) {
      return checkActionImageRead(a);
    }
    if (action instanceof EISecActionImageCreate a) {
      return checkActionImageCreate(a);
    }
    if (action instanceof EISecActionUserUserComplaintCreate c) {
      return checkUserUserComplaintCreate(c);
    }

    return new EISecPolicyResultDenied("Operation not permitted.");
  }

  private static EISecPolicyResultType checkAdmin(
    final EISecActionAdminType action)
  {
    if (action instanceof EISecActionAuditRead c) {
      return checkAuditGet(c);
    }
    if (action instanceof EISecActionUserCreate c) {
      return checkUserCreate(c);
    }
    if (action instanceof EISecActionUserRead c) {
      return checkUserRead(c);
    }
    if (action instanceof EISecActionServicesRead c) {
      return checkServicesRead(c);
    }
    if (action instanceof EISecActionAdminCreate c) {
      return checkAdminCreate(c);
    }
    if (action instanceof EISecActionAdminRead c) {
      return checkAdminRead(c);
    }

    return new EISecPolicyResultDenied("Operation not permitted.");
  }

  private static EISecPolicyResultType checkAdminCreate(
    final EISecActionAdminCreate c)
  {
    /*
     * The ADMIN_CREATE permission is required to even think about creating
     * a new admin.
     */

    final var ownerPerms = c.admin().permissions();
    if (!ownerPerms.contains(ADMIN_CREATE)) {
      return new EISecPolicyResultDenied(
        "You do not have the %s permission.".formatted(ADMIN_CREATE)
      );
    }

    /*
     * The permissions of the created admin must be a subset of the admin
     * doing the creating. Anything else would be a privilege escalation.
     */

    final var targetPerms = c.targetPermissions();
    if (!ownerPerms.containsAll(targetPerms)) {
      return new EISecPolicyResultDenied(
        "An admin cannot be created with more permissions than the creating admin."
      );
    }

    return new EISecPolicyResultPermitted();
  }

  @Override
  public EISecPolicyResultType check(
    final EISecActionType action)
  {
    Objects.requireNonNull(action, "action");

    if (action instanceof EISecActionAdminType admin) {
      return checkAdmin(admin);
    }
    if (action instanceof EISecActionUserType user) {
      return checkUserAction(user);
    }
    return new EISecPolicyResultDenied("Operation not permitted.");
  }
}
