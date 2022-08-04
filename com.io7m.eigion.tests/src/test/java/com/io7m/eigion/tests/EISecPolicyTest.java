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

package com.io7m.eigion.tests;

import com.io7m.eigion.model.EIAdmin;
import com.io7m.eigion.model.EIAdminPermission;
import com.io7m.eigion.model.EIGroupCreationRequest;
import com.io7m.eigion.model.EIGroupCreationRequestStatusType.Cancelled;
import com.io7m.eigion.model.EIGroupInvite;
import com.io7m.eigion.model.EIGroupInviteStatus;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIGroupRole;
import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.server.security.EISecActionType;
import com.io7m.eigion.server.security.EISecAdminActionAdminCreate;
import com.io7m.eigion.server.security.EISecAdminActionAdminRead;
import com.io7m.eigion.server.security.EISecAdminActionAuditRead;
import com.io7m.eigion.server.security.EISecAdminActionGroupInviteSetStatus;
import com.io7m.eigion.server.security.EISecAdminActionGroupInvites;
import com.io7m.eigion.server.security.EISecAdminActionServicesRead;
import com.io7m.eigion.server.security.EISecAdminActionUserBan;
import com.io7m.eigion.server.security.EISecAdminActionUserCreate;
import com.io7m.eigion.server.security.EISecAdminActionUserRead;
import com.io7m.eigion.server.security.EISecAdminActionUserUnban;
import com.io7m.eigion.server.security.EISecAdminActionUserUpdate;
import com.io7m.eigion.server.security.EISecPolicyDefault;
import com.io7m.eigion.server.security.EISecPolicyResultDenied;
import com.io7m.eigion.server.security.EISecPolicyResultPermitted;
import com.io7m.eigion.server.security.EISecUserActionGroupCreateBegin;
import com.io7m.eigion.server.security.EISecUserActionGroupCreateCancel;
import com.io7m.eigion.server.security.EISecUserActionGroupCreateReady;
import com.io7m.eigion.server.security.EISecUserActionGroupGrant;
import com.io7m.eigion.server.security.EISecUserActionGroupInvite;
import com.io7m.eigion.server.security.EISecUserActionGroupInviteCancel;
import com.io7m.eigion.server.security.EISecUserActionGroupLeave;
import com.io7m.eigion.server.security.EISecUserActionImageCreate;
import com.io7m.eigion.server.security.EISecUserActionImageRead;
import com.io7m.eigion.server.security.EISecurityException;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.io7m.eigion.model.EIAdminPermission.ADMIN_CREATE;
import static com.io7m.eigion.model.EIAdminPermission.ADMIN_READ;
import static com.io7m.eigion.model.EIAdminPermission.AUDIT_READ;
import static com.io7m.eigion.model.EIAdminPermission.GROUP_INVITES_READ;
import static com.io7m.eigion.model.EIAdminPermission.GROUP_INVITES_WRITE;
import static com.io7m.eigion.model.EIAdminPermission.SERVICE_READ;
import static com.io7m.eigion.model.EIAdminPermission.USER_READ;
import static com.io7m.eigion.model.EIAdminPermission.USER_WRITE;
import static com.io7m.eigion.model.EIGroupRole.FOUNDER;
import static com.io7m.eigion.model.EIGroupRole.USER_DISMISS;
import static com.io7m.eigion.model.EIGroupRole.USER_INVITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * The security policy tests.
 */

public final class EISecPolicyTest
{
  private static void permitted(
    final EISecActionType action)
    throws EISecurityException
  {
    final var policy = EISecPolicyDefault.get();
    assertEquals(
      EISecPolicyResultPermitted.class,
      policy.check(action).getClass()
    );
  }

  private static void denied(
    final EISecActionType action)
    throws EISecurityException
  {
    final var policy = EISecPolicyDefault.get();
    assertEquals(
      EISecPolicyResultDenied.class,
      policy.check(action).getClass()
    );
  }

  /**
   * An admin with the right permissions can create admins with the same
   * permissions.
   *
   * @param admin The admin
   *
   * @throws Exception On errors
   */

  @Property
  public void testAdminActionAdminCreatePermitted(
    @ForAll final EIAdmin admin)
    throws Exception
  {
    final var withPerm =
      new EIAdmin(
        admin.id(),
        admin.name(),
        admin.email(),
        admin.created(),
        admin.lastLoginTime(),
        admin.password(),
        Set.of(ADMIN_CREATE)
      );

    permitted(new EISecAdminActionAdminCreate(
      withPerm,
      withPerm.permissions()));
  }

  /**
   * An admin with the right permissions cannot create admins with more
   * permissions.
   *
   * @param admin The admin
   *
   * @throws Exception On errors
   */

  @Property
  public void testAdminActionAdminCreateDeniedMore(
    @ForAll final EIAdmin admin)
    throws Exception
  {
    final var withPerm =
      new EIAdmin(
        admin.id(),
        admin.name(),
        admin.email(),
        admin.created(),
        admin.lastLoginTime(),
        admin.password(),
        Set.of(ADMIN_CREATE)
      );

    denied(new EISecAdminActionAdminCreate(
      withPerm,
      EnumSet.allOf(EIAdminPermission.class)));
  }

  /**
   * An admin without the right permissions cannot create admins.
   *
   * @param admin The admin
   *
   * @throws Exception On errors
   */

  @Property
  public void testAdminActionAdminCreateDeniedNone(
    @ForAll final EIAdmin admin)
    throws Exception
  {
    final var withPerm =
      new EIAdmin(
        admin.id(),
        admin.name(),
        admin.email(),
        admin.created(),
        admin.lastLoginTime(),
        admin.password(),
        EnumSet.complementOf(EnumSet.of(ADMIN_CREATE))
      );

    denied(new EISecAdminActionAdminCreate(withPerm, Set.of()));
  }

  /**
   * An admin with the right permissions can read admins.
   *
   * @param admin The admin
   *
   * @throws Exception On errors
   */

  @Property
  public void testAdminActionAdminReadPermitted(
    @ForAll final EIAdmin admin)
    throws Exception
  {
    final var permissions = new HashSet<>(admin.permissions());
    permissions.add(ADMIN_READ);

    final var withPerm =
      new EIAdmin(
        admin.id(),
        admin.name(),
        admin.email(),
        admin.created(),
        admin.lastLoginTime(),
        admin.password(),
        permissions
      );

    permitted(new EISecAdminActionAdminRead(withPerm));
  }

  /**
   * An admin without the right permissions cannot read admins.
   *
   * @param admin The admin
   *
   * @throws Exception On errors
   */

  @Property
  public void testAdminActionAdminReadDenied(
    @ForAll final EIAdmin admin)
    throws Exception
  {
    final var permissions = new HashSet<>(admin.permissions());
    permissions.remove(ADMIN_READ);

    final var withPerm =
      new EIAdmin(
        admin.id(),
        admin.name(),
        admin.email(),
        admin.created(),
        admin.lastLoginTime(),
        admin.password(),
        permissions
      );

    denied(new EISecAdminActionAdminRead(withPerm));
  }

  /**
   * An admin with the right permissions can read audit logs.
   *
   * @param admin The admin
   *
   * @throws Exception On errors
   */

  @Property
  public void testAdminActionAuditReadPermitted(
    @ForAll final EIAdmin admin)
    throws Exception
  {
    final var permissions = new HashSet<>(admin.permissions());
    permissions.add(AUDIT_READ);

    final var withPerm =
      new EIAdmin(
        admin.id(),
        admin.name(),
        admin.email(),
        admin.created(),
        admin.lastLoginTime(),
        admin.password(),
        permissions
      );

    permitted(new EISecAdminActionAuditRead(withPerm));
  }

  /**
   * An admin without the right permissions cannot read audit logs.
   *
   * @param admin The admin
   *
   * @throws Exception On errors
   */

  @Property
  public void testAdminActionAuditReadDenied(
    @ForAll final EIAdmin admin)
    throws Exception
  {
    final var permissions = new HashSet<>(admin.permissions());
    permissions.remove(AUDIT_READ);

    final var withPerm =
      new EIAdmin(
        admin.id(),
        admin.name(),
        admin.email(),
        admin.created(),
        admin.lastLoginTime(),
        admin.password(),
        permissions
      );

    denied(new EISecAdminActionAuditRead(withPerm));
  }

  /**
   * An admin with the right permissions can read group invites.
   *
   * @param admin The admin
   *
   * @throws Exception On errors
   */

  @Property
  public void testAdminActionGroupInvitesReadPermitted(
    @ForAll final EIAdmin admin)
    throws Exception
  {
    final var permissions = new HashSet<>(admin.permissions());
    permissions.add(GROUP_INVITES_READ);

    final var withPerm =
      new EIAdmin(
        admin.id(),
        admin.name(),
        admin.email(),
        admin.created(),
        admin.lastLoginTime(),
        admin.password(),
        permissions
      );

    permitted(new EISecAdminActionGroupInvites(withPerm));
  }

  /**
   * An admin without the right permissions cannot read group invites.
   *
   * @param admin The admin
   *
   * @throws Exception On errors
   */

  @Property
  public void testAdminActionGroupInvitesReadDenied(
    @ForAll final EIAdmin admin)
    throws Exception
  {
    final var permissions = new HashSet<>(admin.permissions());
    permissions.remove(GROUP_INVITES_READ);

    final var withPerm =
      new EIAdmin(
        admin.id(),
        admin.name(),
        admin.email(),
        admin.created(),
        admin.lastLoginTime(),
        admin.password(),
        permissions
      );

    denied(new EISecAdminActionGroupInvites(withPerm));
  }

  /**
   * An admin with the right permissions can read group invites.
   *
   * @param admin The admin
   *
   * @throws Exception On errors
   */

  @Property
  public void testAdminActionGroupInvitesWritePermitted(
    @ForAll final EIAdmin admin)
    throws Exception
  {
    final var permissions = new HashSet<>(admin.permissions());
    permissions.add(GROUP_INVITES_WRITE);

    final var withPerm =
      new EIAdmin(
        admin.id(),
        admin.name(),
        admin.email(),
        admin.created(),
        admin.lastLoginTime(),
        admin.password(),
        permissions
      );

    permitted(new EISecAdminActionGroupInviteSetStatus(withPerm));
  }

  /**
   * An admin without the right permissions cannot read group invites.
   *
   * @param admin The admin
   *
   * @throws Exception On errors
   */

  @Property
  public void testAdminActionGroupInvitesWriteDenied(
    @ForAll final EIAdmin admin)
    throws Exception
  {
    final var permissions = new HashSet<>(admin.permissions());
    permissions.remove(GROUP_INVITES_WRITE);

    final var withPerm =
      new EIAdmin(
        admin.id(),
        admin.name(),
        admin.email(),
        admin.created(),
        admin.lastLoginTime(),
        admin.password(),
        permissions
      );

    denied(new EISecAdminActionGroupInviteSetStatus(withPerm));
  }

  /**
   * An admin with the right permissions can read services.
   *
   * @param admin The admin
   *
   * @throws Exception On errors
   */

  @Property
  public void testAdminActionServicesReadPermitted(
    @ForAll final EIAdmin admin)
    throws Exception
  {
    final var permissions = new HashSet<>(admin.permissions());
    permissions.add(SERVICE_READ);

    final var withPerm =
      new EIAdmin(
        admin.id(),
        admin.name(),
        admin.email(),
        admin.created(),
        admin.lastLoginTime(),
        admin.password(),
        permissions
      );

    permitted(new EISecAdminActionServicesRead(withPerm));
  }

  /**
   * An admin without the right permissions cannot read services.
   *
   * @param admin The admin
   *
   * @throws Exception On errors
   */

  @Property
  public void testAdminActionServicesReadDenied(
    @ForAll final EIAdmin admin)
    throws Exception
  {
    final var permissions = new HashSet<>(admin.permissions());
    permissions.remove(SERVICE_READ);

    final var withPerm =
      new EIAdmin(
        admin.id(),
        admin.name(),
        admin.email(),
        admin.created(),
        admin.lastLoginTime(),
        admin.password(),
        permissions
      );

    denied(new EISecAdminActionServicesRead(withPerm));
  }

  /**
   * An admin with the right permissions can read users.
   *
   * @param admin The admin
   *
   * @throws Exception On errors
   */

  @Property
  public void testAdminActionUserReadPermitted(
    @ForAll final EIAdmin admin)
    throws Exception
  {
    final var permissions = new HashSet<>(admin.permissions());
    permissions.add(USER_READ);

    final var withPerm =
      new EIAdmin(
        admin.id(),
        admin.name(),
        admin.email(),
        admin.created(),
        admin.lastLoginTime(),
        admin.password(),
        permissions
      );

    permitted(new EISecAdminActionUserRead(withPerm));
  }

  /**
   * An admin without the right permissions cannot read users.
   *
   * @param admin The admin
   *
   * @throws Exception On errors
   */

  @Property
  public void testAdminActionUserReadDenied(
    @ForAll final EIAdmin admin)
    throws Exception
  {
    final var permissions = new HashSet<>(admin.permissions());
    permissions.remove(USER_READ);

    final var withPerm =
      new EIAdmin(
        admin.id(),
        admin.name(),
        admin.email(),
        admin.created(),
        admin.lastLoginTime(),
        admin.password(),
        permissions
      );

    denied(new EISecAdminActionUserRead(withPerm));
  }

  /**
   * An admin with the right permissions can create users.
   *
   * @param admin The admin
   *
   * @throws Exception On errors
   */

  @Property
  public void testAdminActionUserCreatePermitted(
    @ForAll final EIAdmin admin)
    throws Exception
  {
    final var permissions = new HashSet<>(admin.permissions());
    permissions.add(USER_WRITE);

    final var withPerm =
      new EIAdmin(
        admin.id(),
        admin.name(),
        admin.email(),
        admin.created(),
        admin.lastLoginTime(),
        admin.password(),
        permissions
      );

    permitted(new EISecAdminActionUserCreate(withPerm));
  }

  /**
   * An admin without the right permissions cannot create users.
   *
   * @param admin The admin
   *
   * @throws Exception On errors
   */

  @Property
  public void testAdminActionUserCreateDenied(
    @ForAll final EIAdmin admin)
    throws Exception
  {
    final var permissions = new HashSet<>(admin.permissions());
    permissions.remove(USER_WRITE);

    final var withPerm =
      new EIAdmin(
        admin.id(),
        admin.name(),
        admin.email(),
        admin.created(),
        admin.lastLoginTime(),
        admin.password(),
        permissions
      );

    denied(new EISecAdminActionUserCreate(withPerm));
  }

  /**
   * An admin with the right permissions can ban users.
   *
   * @param admin The admin
   *
   * @throws Exception On errors
   */

  @Property
  public void testAdminActionUserBanPermitted(
    @ForAll final EIAdmin admin)
    throws Exception
  {
    final var permissions = new HashSet<>(admin.permissions());
    permissions.add(USER_WRITE);

    final var withPerm =
      new EIAdmin(
        admin.id(),
        admin.name(),
        admin.email(),
        admin.created(),
        admin.lastLoginTime(),
        admin.password(),
        permissions
      );

    permitted(new EISecAdminActionUserBan(withPerm));
  }

  /**
   * An admin without the right permissions cannot ban users.
   *
   * @param admin The admin
   *
   * @throws Exception On errors
   */

  @Property
  public void testAdminActionUserBanDenied(
    @ForAll final EIAdmin admin)
    throws Exception
  {
    final var permissions = new HashSet<>(admin.permissions());
    permissions.remove(USER_WRITE);

    final var withPerm =
      new EIAdmin(
        admin.id(),
        admin.name(),
        admin.email(),
        admin.created(),
        admin.lastLoginTime(),
        admin.password(),
        permissions
      );

    denied(new EISecAdminActionUserBan(withPerm));
  }

  /**
   * An admin with the right permissions can unban users.
   *
   * @param admin The admin
   *
   * @throws Exception On errors
   */

  @Property
  public void testAdminActionUserUnbanPermitted(
    @ForAll final EIAdmin admin)
    throws Exception
  {
    final var permissions = new HashSet<>(admin.permissions());
    permissions.add(USER_WRITE);

    final var withPerm =
      new EIAdmin(
        admin.id(),
        admin.name(),
        admin.email(),
        admin.created(),
        admin.lastLoginTime(),
        admin.password(),
        permissions
      );

    permitted(new EISecAdminActionUserUnban(withPerm));
  }

  /**
   * An admin without the right permissions cannot unban users.
   *
   * @param admin The admin
   *
   * @throws Exception On errors
   */

  @Property
  public void testAdminActionUserUnbanDenied(
    @ForAll final EIAdmin admin)
    throws Exception
  {
    final var permissions = new HashSet<>(admin.permissions());
    permissions.remove(USER_WRITE);

    final var withPerm =
      new EIAdmin(
        admin.id(),
        admin.name(),
        admin.email(),
        admin.created(),
        admin.lastLoginTime(),
        admin.password(),
        permissions
      );

    denied(new EISecAdminActionUserUnban(withPerm));
  }

  /**
   * An admin with the right permissions can update users.
   *
   * @param admin The admin
   *
   * @throws Exception On errors
   */

  @Property
  public void testAdminActionUserUpdatePermitted(
    @ForAll final EIAdmin admin)
    throws Exception
  {
    final var permissions = new HashSet<>(admin.permissions());
    permissions.add(USER_WRITE);

    final var withPerm =
      new EIAdmin(
        admin.id(),
        admin.name(),
        admin.email(),
        admin.created(),
        admin.lastLoginTime(),
        admin.password(),
        permissions
      );

    permitted(new EISecAdminActionUserUpdate(withPerm));
  }

  /**
   * An admin without the right permissions cannot update users.
   *
   * @param admin The admin
   *
   * @throws Exception On errors
   */

  @Property
  public void testAdminActionUserUpdateDenied(
    @ForAll final EIAdmin admin)
    throws Exception
  {
    final var permissions = new HashSet<>(admin.permissions());
    permissions.remove(USER_WRITE);

    final var withPerm =
      new EIAdmin(
        admin.id(),
        admin.name(),
        admin.email(),
        admin.created(),
        admin.lastLoginTime(),
        admin.password(),
        permissions
      );

    denied(new EISecAdminActionUserUpdate(withPerm));
  }

  /**
   * A user that has not created any group requests recently can create new
   * group requests.
   *
   * @param user  The user
   * @param time  The time
   * @param group The group
   *
   * @throws Exception On errors
   */

  @Property
  public void testUserActionGroupCreateBegin(
    @ForAll final EIUser user,
    @ForAll final OffsetDateTime time,
    @ForAll final EIGroupName group)
    throws Exception
  {
    permitted(new EISecUserActionGroupCreateBegin(
      user,
      time,
      List.of(),
      group
    ));
  }

  /**
   * A user that has created too many group requests recently cannot create new
   * group requests.
   *
   * @param user  The user
   * @param time  The time
   * @param group The group
   *
   * @throws Exception On errors
   */

  @Property
  public void testUserActionGroupCreateBeginTooMany(
    @ForAll final EIUser user,
    @ForAll final OffsetDateTime time,
    @ForAll final EIGroupName group)
    throws Exception
  {
    final var requests =
      List.of(
        new EIGroupCreationRequest(
          new EIGroupName("a.b.c0"),
          user.id(),
          EIToken.generate(),
          new Cancelled(time, time)
        ),
        new EIGroupCreationRequest(
          new EIGroupName("a.b.c1"),
          user.id(),
          EIToken.generate(),
          new Cancelled(time, time)
        ),
        new EIGroupCreationRequest(
          new EIGroupName("a.b.c2"),
          user.id(),
          EIToken.generate(),
          new Cancelled(time, time)
        ),
        new EIGroupCreationRequest(
          new EIGroupName("a.b.c3"),
          user.id(),
          EIToken.generate(),
          new Cancelled(time, time)
        ),
        new EIGroupCreationRequest(
          new EIGroupName("a.b.c4"),
          user.id(),
          EIToken.generate(),
          new Cancelled(time, time)
        )
      );

    denied(new EISecUserActionGroupCreateBegin(
      user,
      time,
      requests,
      group
    ));
  }

  /**
   * A user can cancel their own group requests.
   *
   * @param user    The user
   * @param request The request
   *
   * @throws Exception On errors
   */

  @Property
  public void testUserActionGroupCreateCancel(
    @ForAll final EIUser user,
    @ForAll final EIGroupCreationRequest request)
    throws Exception
  {
    final var withOwner =
      new EIGroupCreationRequest(
        request.groupName(),
        user.id(),
        request.token(),
        request.status()
      );

    permitted(new EISecUserActionGroupCreateCancel(user, withOwner));
  }

  /**
   * A user cannot cancel other people's group requests.
   *
   * @param user    The user
   * @param request The request
   *
   * @throws Exception On errors
   */

  @Property
  public void testUserActionGroupCreateCancelOthers(
    @ForAll final EIUser user,
    @ForAll final EIGroupCreationRequest request)
    throws Exception
  {
    final var other = UUID.randomUUID();
    assumeFalse(Objects.equals(user.id(), other));

    final var withoutOwner =
      new EIGroupCreationRequest(
        request.groupName(),
        other,
        request.token(),
        request.status()
      );

    denied(new EISecUserActionGroupCreateCancel(user, withoutOwner));
  }

  /**
   * A user can ready their own group requests.
   *
   * @param user    The user
   * @param request The request
   *
   * @throws Exception On errors
   */

  @Property
  public void testUserActionGroupCreateReady(
    @ForAll final EIUser user,
    @ForAll final EIGroupCreationRequest request)
    throws Exception
  {
    final var withOwner =
      new EIGroupCreationRequest(
        request.groupName(),
        user.id(),
        request.token(),
        request.status()
      );

    permitted(new EISecUserActionGroupCreateReady(user, withOwner));
  }

  /**
   * A user cannot ready other people's group requests.
   *
   * @param user    The user
   * @param request The request
   *
   * @throws Exception On errors
   */

  @Property
  public void testUserActionGroupCreateReadyOthers(
    @ForAll final EIUser user,
    @ForAll final EIGroupCreationRequest request)
    throws Exception
  {
    final var other = UUID.randomUUID();
    assumeFalse(Objects.equals(user.id(), other));

    final var withoutOwner =
      new EIGroupCreationRequest(
        request.groupName(),
        other,
        request.token(),
        request.status()
      );

    denied(new EISecUserActionGroupCreateReady(user, withoutOwner));
  }

  /**
   * A user that has not created any group invites recently can create new group
   * invites.
   *
   * @param userInviting     The user
   * @param userBeingInvited The user being invited
   * @param time             The time
   * @param group            The group
   *
   * @throws Exception On errors
   */

  @Property
  public void testUserActionGroupInvite(
    @ForAll final EIUser userInviting,
    @ForAll final EIUser userBeingInvited,
    @ForAll final OffsetDateTime time,
    @ForAll final EIGroupName group)
    throws Exception
  {
    final var membership =
      Map.ofEntries(
        Map.entry(group, Set.of(USER_INVITE))
      );

    final var userInvitingActual =
      new EIUser(
        userInviting.id(),
        userInviting.name(),
        userInviting.email(),
        userInviting.created(),
        userInviting.lastLoginTime(),
        userInviting.password(),
        userInviting.ban(),
        membership
      );

    permitted(new EISecUserActionGroupInvite(
      userInvitingActual,
      userBeingInvited,
      group,
      time,
      List.of()
    ));
  }

  /**
   * A user that has created too many invites recently cannot create new group
   * invites.
   *
   * @param userInviting     The user
   * @param userBeingInvited The user being invited
   * @param time             The time
   * @param group            The group
   *
   * @throws Exception On errors
   */

  @Property
  public void testUserActionGroupInviteTooMany(
    @ForAll final EIUser userInviting,
    @ForAll final EIUser userBeingInvited,
    @ForAll final OffsetDateTime time,
    @ForAll final EIGroupName group)
    throws Exception
  {
    final var invites =
      List.of(
        new EIGroupInvite(
          userInviting.id(),
          userInviting.name(),
          userBeingInvited.id(),
          userBeingInvited.name(),
          group,
          EIToken.generate(),
          EIGroupInviteStatus.CANCELLED,
          time,
          Optional.of(time)
        ),
        new EIGroupInvite(
          userInviting.id(),
          userInviting.name(),
          userBeingInvited.id(),
          userBeingInvited.name(),
          group,
          EIToken.generate(),
          EIGroupInviteStatus.CANCELLED,
          time,
          Optional.of(time)
        ),
        new EIGroupInvite(
          userInviting.id(),
          userInviting.name(),
          userBeingInvited.id(),
          userBeingInvited.name(),
          group,
          EIToken.generate(),
          EIGroupInviteStatus.CANCELLED,
          time,
          Optional.of(time)
        ),
        new EIGroupInvite(
          userInviting.id(),
          userInviting.name(),
          userBeingInvited.id(),
          userBeingInvited.name(),
          group,
          EIToken.generate(),
          EIGroupInviteStatus.CANCELLED,
          time,
          Optional.of(time)
        ),
        new EIGroupInvite(
          userInviting.id(),
          userInviting.name(),
          userBeingInvited.id(),
          userBeingInvited.name(),
          group,
          EIToken.generate(),
          EIGroupInviteStatus.CANCELLED,
          time,
          Optional.of(time)
        )
      );

    denied(new EISecUserActionGroupInvite(
      userInviting,
      userBeingInvited,
      group,
      time,
      invites
    ));
  }

  /**
   * A user that is not a member of a group cannot create invites for it.
   *
   * @param userInviting     The user
   * @param userBeingInvited The user being invited
   * @param time             The time
   * @param group            The group
   *
   * @throws Exception On errors
   */

  @Property
  public void testUserActionGroupInviteNotMember(
    @ForAll final EIUser userInviting,
    @ForAll final EIUser userBeingInvited,
    @ForAll final OffsetDateTime time,
    @ForAll final EIGroupName group)
    throws Exception
  {
    final var userInvitingActual =
      new EIUser(
        userInviting.id(),
        userInviting.name(),
        userInviting.email(),
        userInviting.created(),
        userInviting.lastLoginTime(),
        userInviting.password(),
        userInviting.ban(),
        Map.of()
      );

    denied(new EISecUserActionGroupInvite(
      userInvitingActual,
      userBeingInvited,
      group,
      time,
      List.of()
    ));
  }

  /**
   * A user that is a member of a group but doesn't have an invite role cannot
   * create invites for it.
   *
   * @param userInviting     The user
   * @param userBeingInvited The user being invited
   * @param time             The time
   * @param group            The group
   *
   * @throws Exception On errors
   */

  @Property
  public void testUserActionGroupInviteNoRole(
    @ForAll final EIUser userInviting,
    @ForAll final EIUser userBeingInvited,
    @ForAll final OffsetDateTime time,
    @ForAll final EIGroupName group)
    throws Exception
  {
    final var membership =
      Map.ofEntries(
        Map.entry(group, Set.<EIGroupRole>of())
      );

    final var userInvitingActual =
      new EIUser(
        userInviting.id(),
        userInviting.name(),
        userInviting.email(),
        userInviting.created(),
        userInviting.lastLoginTime(),
        userInviting.password(),
        userInviting.ban(),
        membership
      );

    denied(new EISecUserActionGroupInvite(
      userInvitingActual,
      userBeingInvited,
      group,
      time,
      List.of()
    ));
  }

  /**
   * A user that has the correct roles can cancel group invites for the group.
   *
   * @param userInviting The user
   * @param invite       The invite
   *
   * @throws Exception On errors
   */

  @Property
  public void testUserActionGroupInviteCancel(
    @ForAll final EIUser userInviting,
    @ForAll final EIGroupInvite invite)
    throws Exception
  {
    final var membership =
      Map.ofEntries(
        Map.entry(invite.group(), Set.of(USER_INVITE))
      );

    final var userInvitingActual =
      new EIUser(
        userInviting.id(),
        userInviting.name(),
        userInviting.email(),
        userInviting.created(),
        userInviting.lastLoginTime(),
        userInviting.password(),
        userInviting.ban(),
        membership
      );

    permitted(new EISecUserActionGroupInviteCancel(userInvitingActual, invite));
  }

  /**
   * A user that has the correct roles can cancel group invites for the group.
   *
   * @param userInviting The user
   * @param invite       The invite
   *
   * @throws Exception On errors
   */

  @Property
  public void testUserActionGroupInviteCancelOther(
    @ForAll final EIUser userInviting,
    @ForAll final EIGroupInvite invite)
    throws Exception
  {
    final var membership =
      Map.ofEntries(
        Map.entry(invite.group(), Set.of(USER_DISMISS))
      );

    final var userInvitingActual =
      new EIUser(
        userInviting.id(),
        userInviting.name(),
        userInviting.email(),
        userInviting.created(),
        userInviting.lastLoginTime(),
        userInviting.password(),
        userInviting.ban(),
        membership
      );

    permitted(new EISecUserActionGroupInviteCancel(userInvitingActual, invite));
  }

  /**
   * A user that does not have the correct roles cannot cancel group invites for
   * the group.
   *
   * @param userInviting The user
   * @param invite       The invite
   *
   * @throws Exception On errors
   */

  @Property
  public void testUserActionGroupInviteCancelDenied(
    @ForAll final EIUser userInviting,
    @ForAll final EIGroupInvite invite)
    throws Exception
  {
    final var membership =
      Map.ofEntries(
        Map.entry(invite.group(), Set.<EIGroupRole>of())
      );

    final var userInvitingActual =
      new EIUser(
        userInviting.id(),
        userInviting.name(),
        userInviting.email(),
        userInviting.created(),
        userInviting.lastLoginTime(),
        userInviting.password(),
        userInviting.ban(),
        membership
      );

    denied(new EISecUserActionGroupInviteCancel(userInvitingActual, invite));
  }

  /**
   * Any user can create images.
   *
   * @param user The user
   *
   * @throws Exception On errors
   */

  @Property
  public void testUserActionImageCreate(
    @ForAll final EIUser user)
    throws Exception
  {
    permitted(new EISecUserActionImageCreate(user));
  }

  /**
   * Any user can read images.
   *
   * @param user The user
   *
   * @throws Exception On errors
   */

  @Property
  public void testUserActionImageRead(
    @ForAll final EIUser user)
    throws Exception
  {
    permitted(new EISecUserActionImageRead(user));
  }

  /**
   * A user that is not in a group cannot grant anything for that group.
   *
   * @param userGranting  The user granting the role
   * @param userReceiving The user receiving the role
   * @param role          The role
   * @param groupName     The group name
   *
   * @throws Exception On errors
   */

  @Property
  public void testUserActionGroupGrantNotInGroupA(
    @ForAll final EIUser userGranting,
    @ForAll final EIUser userReceiving,
    @ForAll final EIGroupName groupName,
    @ForAll final EIGroupRole role)
    throws Exception
  {
    assumeFalse(role == FOUNDER);

    final var membershipA =
      new HashMap<>(userGranting.groupMembership());

    membershipA.remove(groupName);

    final var membershipB =
      new HashMap<>(userReceiving.groupMembership());

    membershipB.put(groupName, EnumSet.allOf(EIGroupRole.class));

    final var userGrantingActual =
      new EIUser(
        userGranting.id(),
        userGranting.name(),
        userGranting.email(),
        userGranting.created(),
        userGranting.lastLoginTime(),
        userGranting.password(),
        userGranting.ban(),
        membershipA
      );

    final var userReceivingActual =
      new EIUser(
        userGranting.id(),
        userGranting.name(),
        userGranting.email(),
        userGranting.created(),
        userGranting.lastLoginTime(),
        userGranting.password(),
        userGranting.ban(),
        membershipB
      );

    denied(new EISecUserActionGroupGrant(
      userGrantingActual,
      groupName,
      role,
      userReceivingActual
    ));
  }

  /**
   * A user cannot grant anything for a user that is not in that group.
   *
   * @param userGranting  The user granting the role
   * @param userReceiving The user receiving the role
   * @param role          The role
   * @param groupName     The group name
   *
   * @throws Exception On errors
   */

  @Property
  public void testUserActionGroupGrantNotInGroupB(
    @ForAll final EIUser userGranting,
    @ForAll final EIUser userReceiving,
    @ForAll final EIGroupName groupName,
    @ForAll final EIGroupRole role)
    throws Exception
  {
    assumeFalse(role == FOUNDER);

    final var membershipA =
      new HashMap<>(userGranting.groupMembership());

    membershipA.put(groupName, Set.of(role));

    final var membershipB =
      new HashMap<>(userReceiving.groupMembership());

    membershipB.remove(groupName);

    final var userGrantingActual =
      new EIUser(
        userGranting.id(),
        userGranting.name(),
        userGranting.email(),
        userGranting.created(),
        userGranting.lastLoginTime(),
        userGranting.password(),
        userGranting.ban(),
        membershipA
      );

    final var userReceivingActual =
      new EIUser(
        userGranting.id(),
        userGranting.name(),
        userGranting.email(),
        userGranting.created(),
        userGranting.lastLoginTime(),
        userGranting.password(),
        userGranting.ban(),
        membershipB
      );

    denied(new EISecUserActionGroupGrant(
      userGrantingActual,
      groupName,
      role,
      userReceivingActual
    ));
  }

  /**
   * A user cannot grant a role that it does not have.
   *
   * @param userGranting  The user granting the role
   * @param userReceiving The user receiving the role
   * @param role          The role
   * @param groupName     The group name
   *
   * @throws Exception On errors
   */

  @Property
  public void testUserActionGroupGrantNotAvailable(
    @ForAll final EIUser userGranting,
    @ForAll final EIUser userReceiving,
    @ForAll final EIGroupName groupName,
    @ForAll final EIGroupRole role)
    throws Exception
  {
    assumeFalse(role == FOUNDER);

    final var membershipA =
      new HashMap<>(userGranting.groupMembership());

    membershipA.put(groupName, Set.of());

    final var membershipB =
      new HashMap<>(userReceiving.groupMembership());

    membershipB.put(groupName, EnumSet.allOf(EIGroupRole.class));

    final var userGrantingActual =
      new EIUser(
        userGranting.id(),
        userGranting.name(),
        userGranting.email(),
        userGranting.created(),
        userGranting.lastLoginTime(),
        userGranting.password(),
        userGranting.ban(),
        membershipA
      );

    final var userReceivingActual =
      new EIUser(
        userGranting.id(),
        userGranting.name(),
        userGranting.email(),
        userGranting.created(),
        userGranting.lastLoginTime(),
        userGranting.password(),
        userGranting.ban(),
        membershipB
      );

    denied(new EISecUserActionGroupGrant(
      userGrantingActual,
      groupName,
      role,
      userReceivingActual
    ));
  }

  /**
   * A user can grant a role it has to other users in the group.
   *
   * @param userGranting  The user granting the role
   * @param userReceiving The user receiving the role
   * @param role          The role
   * @param groupName     The group name
   *
   * @throws Exception On errors
   */

  @Property
  public void testUserActionGroupGrant(
    @ForAll final EIUser userGranting,
    @ForAll final EIUser userReceiving,
    @ForAll final EIGroupName groupName,
    @ForAll final EIGroupRole role)
    throws Exception
  {
    assumeFalse(role == FOUNDER);

    final var membershipA =
      new HashMap<>(userGranting.groupMembership());

    membershipA.put(groupName, Set.of(role));

    final var membershipB =
      new HashMap<>(userReceiving.groupMembership());

    membershipB.put(groupName, EnumSet.allOf(EIGroupRole.class));

    final var userGrantingActual =
      new EIUser(
        userGranting.id(),
        userGranting.name(),
        userGranting.email(),
        userGranting.created(),
        userGranting.lastLoginTime(),
        userGranting.password(),
        userGranting.ban(),
        membershipA
      );

    final var userReceivingActual =
      new EIUser(
        userGranting.id(),
        userGranting.name(),
        userGranting.email(),
        userGranting.created(),
        userGranting.lastLoginTime(),
        userGranting.password(),
        userGranting.ban(),
        membershipB
      );

    permitted(new EISecUserActionGroupGrant(
      userGrantingActual,
      groupName,
      role,
      userReceivingActual
    ));
  }

  /**
   * A user can grant a role it has to other users in the group.
   *
   * @param userGranting  The user granting the role
   * @param userReceiving The user receiving the role
   * @param role          The role
   * @param groupName     The group name
   *
   * @throws Exception On errors
   */

  @Property
  public void testUserActionGroupGrantFounderAlt(
    @ForAll final EIUser userGranting,
    @ForAll final EIUser userReceiving,
    @ForAll final EIGroupName groupName,
    @ForAll final EIGroupRole role)
    throws Exception
  {
    assumeFalse(role == FOUNDER);

    final var membershipA =
      new HashMap<>(userGranting.groupMembership());

    membershipA.put(groupName, Set.of(FOUNDER));

    final var membershipB =
      new HashMap<>(userReceiving.groupMembership());

    membershipB.put(groupName, EnumSet.allOf(EIGroupRole.class));

    final var userGrantingActual =
      new EIUser(
        userGranting.id(),
        userGranting.name(),
        userGranting.email(),
        userGranting.created(),
        userGranting.lastLoginTime(),
        userGranting.password(),
        userGranting.ban(),
        membershipA
      );

    final var userReceivingActual =
      new EIUser(
        userGranting.id(),
        userGranting.name(),
        userGranting.email(),
        userGranting.created(),
        userGranting.lastLoginTime(),
        userGranting.password(),
        userGranting.ban(),
        membershipB
      );

    permitted(new EISecUserActionGroupGrant(
      userGrantingActual,
      groupName,
      role,
      userReceivingActual
    ));
  }

  /**
   * A user cannot grant the FOUNDER role.
   *
   * @param userGranting  The user granting the role
   * @param userReceiving The user receiving the role
   * @param groupName     The group name
   *
   * @throws Exception On errors
   */

  @Property
  public void testUserActionGroupGrantFounder(
    @ForAll final EIUser userGranting,
    @ForAll final EIUser userReceiving,
    @ForAll final EIGroupName groupName)
    throws Exception
  {
    final var membershipA =
      new HashMap<>(userGranting.groupMembership());

    membershipA.put(groupName, Set.of(FOUNDER));

    final var membershipB =
      new HashMap<>(userReceiving.groupMembership());

    membershipB.put(groupName, Set.of());

    final var userGrantingActual =
      new EIUser(
        userGranting.id(),
        userGranting.name(),
        userGranting.email(),
        userGranting.created(),
        userGranting.lastLoginTime(),
        userGranting.password(),
        userGranting.ban(),
        membershipA
      );

    final var userReceivingActual =
      new EIUser(
        userGranting.id(),
        userGranting.name(),
        userGranting.email(),
        userGranting.created(),
        userGranting.lastLoginTime(),
        userGranting.password(),
        userGranting.ban(),
        membershipB
      );

    denied(new EISecUserActionGroupGrant(
      userGrantingActual,
      groupName,
      FOUNDER,
      userReceivingActual
    ));
  }

  /**
   * A user can always leave a group (subject to integrity constraints).
   *
   * @param user      The user granting the role
   * @param groupName The group name
   *
   * @throws Exception On errors
   */

  @Property
  public void testUserActionGroupGrantFounder(
    @ForAll final EIUser user,
    @ForAll final EIGroupName groupName)
    throws Exception
  {
    permitted(new EISecUserActionGroupLeave(user, groupName));
  }
}
