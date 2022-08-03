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
import com.io7m.eigion.model.EIGroupRole;

import java.util.Objects;
import java.util.Set;

import static com.io7m.eigion.model.EIAdminPermission.ADMIN_CREATE;
import static com.io7m.eigion.model.EIAdminPermission.ADMIN_READ;
import static com.io7m.eigion.model.EIAdminPermission.AUDIT_READ;
import static com.io7m.eigion.model.EIAdminPermission.GROUP_INVITES_READ;
import static com.io7m.eigion.model.EIAdminPermission.GROUP_INVITES_WRITE;
import static com.io7m.eigion.model.EIAdminPermission.SERVICE_READ;
import static com.io7m.eigion.model.EIAdminPermission.USER_READ;
import static com.io7m.eigion.model.EIAdminPermission.USER_WRITE;
import static com.io7m.eigion.model.EIGroupRole.USER_DISMISS;
import static com.io7m.eigion.model.EIGroupRole.USER_INVITE;

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

  private static EISecPolicyResultType checkAdminActionAdminRead(
    final EISecAdminActionAdminRead c)
  {
    return checkOwnedPermission(c.admin().permissions(), ADMIN_READ);
  }

  private static EISecPolicyResultType checkAdminActionServicesRead(
    final EISecAdminActionServicesRead c)
  {
    return checkOwnedPermission(c.admin().permissions(), SERVICE_READ);
  }

  private static EISecPolicyResultType checkAdminActionUserRead(
    final EISecAdminActionUserRead c)
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

  private static EISecPolicyResultType checkAdminActionUserCreate(
    final EISecAdminActionUserCreate c)
  {
    return checkOwnedPermission(c.admin().permissions(), USER_WRITE);
  }

  private static EISecPolicyResultType checkAdminActionAuditRead(
    final EISecAdminActionAuditRead c)
  {
    return checkOwnedPermission(c.admin().permissions(), AUDIT_READ);
  }

  private static EISecPolicyResultType checkUserActionUserUserComplaintCreate(
    final EISecUserActionUserUserComplaintCreate c)
  {
    return new EISecPolicyResultPermitted();
  }

  private static EISecPolicyResultType checkUserActionImageRead(
    final EISecUserActionImageRead a)
  {
    return new EISecPolicyResultPermitted();
  }

  private static EISecPolicyResultType checkUserActionImageCreate(
    final EISecUserActionImageCreate a)
  {
    return new EISecPolicyResultPermitted();
  }

  private static EISecPolicyResultType checkUserAction(
    final EISecUserActionType action)
  {
    if (action instanceof EISecUserActionImageRead a) {
      return checkUserActionImageRead(a);
    }
    if (action instanceof EISecUserActionImageCreate a) {
      return checkUserActionImageCreate(a);
    }
    if (action instanceof EISecUserActionUserUserComplaintCreate c) {
      return checkUserActionUserUserComplaintCreate(c);
    }
    if (action instanceof EISecUserActionGroupCreateBegin c) {
      return checkUserActionGroupCreateBegin(c);
    }
    if (action instanceof EISecUserActionGroupCreateCancel c) {
      return checkUserActionGroupCreateCancel(c);
    }
    if (action instanceof EISecUserActionGroupCreateReady c) {
      return checkUserActionGroupCreateReady(c);
    }
    if (action instanceof EISecUserActionGroupInvite c) {
      return checkUserActionGroupInvite(c);
    }
    if (action instanceof EISecUserActionGroupInviteCancel c) {
      return checkUserActionGroupInviteCancel(c);
    }

    return new EISecPolicyResultDenied("Operation not permitted.");
  }

  private static EISecPolicyResultType checkAdminAction(
    final EISecAdminActionType action)
  {
    if (action instanceof EISecAdminActionAuditRead c) {
      return checkAdminActionAuditRead(c);
    }
    if (action instanceof EISecAdminActionUserCreate c) {
      return checkAdminActionUserCreate(c);
    }
    if (action instanceof EISecAdminActionUserRead c) {
      return checkAdminActionUserRead(c);
    }
    if (action instanceof EISecAdminActionServicesRead c) {
      return checkAdminActionServicesRead(c);
    }
    if (action instanceof EISecAdminActionAdminCreate c) {
      return checkAdminActionAdminCreate(c);
    }
    if (action instanceof EISecAdminActionAdminRead c) {
      return checkAdminActionAdminRead(c);
    }
    if (action instanceof EISecAdminActionGroupInvites c) {
      return checkAdminActionGroupInvites(c);
    }
    if (action instanceof EISecAdminActionGroupInviteSetStatus c) {
      return checkAdminActionGroupInviteSetStatus(c);
    }
    if (action instanceof EISecAdminActionUserUpdate c) {
      return checkAdminActionUserUpdate(c);
    }
    if (action instanceof EISecAdminActionUserUnban c) {
      return checkAdminActionUserUnban(c);
    }
    if (action instanceof EISecAdminActionUserBan c) {
      return checkAdminActionUserBan(c);
    }

    return new EISecPolicyResultDenied("Operation not permitted.");
  }

  private static EISecPolicyResultType checkAdminActionUserBan(
    final EISecAdminActionUserBan c)
  {
    return checkOwnedPermission(c.admin().permissions(), USER_WRITE);
  }

  private static EISecPolicyResultType checkAdminActionUserUnban(
    final EISecAdminActionUserUnban c)
  {
    return checkOwnedPermission(c.admin().permissions(), USER_WRITE);
  }

  private static EISecPolicyResultType checkAdminActionUserUpdate(
    final EISecAdminActionUserUpdate c)
  {
    return checkOwnedPermission(c.admin().permissions(), USER_WRITE);
  }

  private static EISecPolicyResultType checkAdminActionGroupInviteSetStatus(
    final EISecAdminActionGroupInviteSetStatus c)
  {
    return checkOwnedPermission(c.admin().permissions(), GROUP_INVITES_WRITE);
  }

  private static EISecPolicyResultType checkAdminActionGroupInvites(
    final EISecAdminActionGroupInvites c)
  {
    return checkOwnedPermission(c.admin().permissions(), GROUP_INVITES_READ);
  }

  private static EISecPolicyResultType checkAdminActionAdminCreate(
    final EISecAdminActionAdminCreate c)
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

  private static EISecPolicyResultType checkUserActionGroupCreateBegin(
    final EISecUserActionGroupCreateBegin c)
  {
    /*
     * Group creation requests are rate-limited.
     */

    final var existingRequests =
      c.existingRequests();
    final var lastHour =
      c.timeNow().minusHours(1L);

    final var recentRequests =
      existingRequests.stream()
        .filter(r -> r.status().timeStarted().isAfter(lastHour))
        .count();

    if (recentRequests >= 5L) {
      final var nextHour =
        c.timeNow().plusHours(1L);

      return new EISecPolicyResultDenied(
        "Too many requests have been made recently. Please wait until %s before making another request."
          .formatted(nextHour)
      );
    }

    return new EISecPolicyResultPermitted();
  }

  private static EISecPolicyResultType checkUserActionGroupInvite(
    final EISecUserActionGroupInvite c)
  {
    /*
     * Invites are rate-limited.
     */

    final var existingRequests =
      c.userInvitingInvites();
    final var lastHour =
      c.timeNow().minusHours(1L);

    final var recentRequests =
      existingRequests.stream()
        .filter(r -> r.timeStarted().isAfter(lastHour))
        .count();

    if (recentRequests >= 5L) {
      final var nextHour =
        c.timeNow().plusHours(1L);

      return new EISecPolicyResultDenied(
        "Too many requests have been made recently. Please wait until %s before making another request."
          .formatted(nextHour)
      );
    }

    /*
     * Group invites require membership and an appropriate role.
     */

    final var invitingMembership =
      c.userInviting().groupMembership();

    final var roles =
      invitingMembership.get(c.group());

    if (roles == null) {
      return new EISecPolicyResultDenied(
        "You must be a member of a group in order to invite users to it."
      );
    }

    if (!EIGroupRole.roleSetImplies(roles, USER_INVITE)) {
      return new EISecPolicyResultDenied(
        "You must have the %s role to invite users to this group."
          .formatted(USER_INVITE)
      );
    }

    return new EISecPolicyResultPermitted();
  }

  private static EISecPolicyResultType checkUserActionGroupInviteCancel(
    final EISecUserActionGroupInviteCancel c)
  {
    /*
     * In order to cancel an invite, the user must have the USER_INVITE
     * or USER_DISMISS role in the group in question.
     */

    final var membership =
      c.user().groupMembership();
    final var roles =
      membership.getOrDefault(c.invite().group(), Set.of());

    final var hasRoles =
      EIGroupRole.roleSetImplies(roles, USER_INVITE)
       || EIGroupRole.roleSetImplies(roles, USER_DISMISS);

    if (!hasRoles) {
      return new EISecPolicyResultDenied(
        "You must have the %s or %s roles to cancel invites to this group."
          .formatted(USER_INVITE, USER_DISMISS)
      );
    }

    return new EISecPolicyResultPermitted();
  }

  private static EISecPolicyResultType checkUserActionGroupCreateCancel(
    final EISecUserActionGroupCreateCancel c)
  {
    if (!Objects.equals(c.user().id(), c.request().userFounder())) {
      return new EISecPolicyResultDenied(
        "Users may only cancel their own group creation requests."
      );
    }

    return new EISecPolicyResultPermitted();
  }

  private static EISecPolicyResultType checkUserActionGroupCreateReady(
    final EISecUserActionGroupCreateReady c)
  {
    if (!Objects.equals(c.user().id(), c.request().userFounder())) {
      return new EISecPolicyResultDenied(
        "Users may only ready their own group creation requests."
      );
    }

    return new EISecPolicyResultPermitted();
  }

  @Override
  public EISecPolicyResultType check(
    final EISecActionType action)
  {
    Objects.requireNonNull(action, "action");

    if (action instanceof EISecAdminActionType admin) {
      return checkAdminAction(admin);
    }
    if (action instanceof EISecUserActionType user) {
      return checkUserAction(user);
    }
    return new EISecPolicyResultDenied("Operation not permitted.");
  }
}
