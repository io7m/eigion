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

package com.io7m.eigion.tests.arbitraries;

import com.io7m.eigion.model.EIAdmin;
import com.io7m.eigion.model.EIAdminPermission;
import com.io7m.eigion.model.EIGroupCreationRequest;
import com.io7m.eigion.model.EIGroupInvite;
import com.io7m.eigion.model.EIGroupName;
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
import com.io7m.eigion.server.security.EISecUserActionGroupCreateBegin;
import com.io7m.eigion.server.security.EISecUserActionGroupCreateCancel;
import com.io7m.eigion.server.security.EISecUserActionGroupCreateReady;
import com.io7m.eigion.server.security.EISecUserActionGroupInvite;
import com.io7m.eigion.server.security.EISecUserActionGroupInviteCancel;
import com.io7m.eigion.server.security.EISecUserActionImageCreate;
import com.io7m.eigion.server.security.EISecUserActionImageRead;
import com.io7m.eigion.server.security.EISecUserActionUserUserComplaintCreate;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.providers.TypeUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A provider of {@link EISecActionType} values.
 */

public final class EIArbSecActionProvider extends EIArbAbstractProvider
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIArbSecActionProvider.class);

  /**
   * A provider of values.
   */

  public EIArbSecActionProvider()
  {

  }

  @Override
  public boolean canProvideFor(
    final TypeUsage targetType)
  {
    return targetType.isOfType(EISecActionType.class);
  }

  @Override
  public Set<Arbitrary<?>> provideFor(
    final TypeUsage targetType,
    final SubtypeProvider subtypeProvider)
  {
    return Set.of(
      adminActionAdminCreate(),
      adminActionAdminRead(),
      adminActionAuditRead(),
      adminActionGroupInviteSetStatus(),
      adminActionGroupInvites(),
      adminActionServicesRead(),
      adminActionUserBan(),
      adminActionUserCreate(),
      adminActionUserRead(),
      adminActionUserUnban(),
      adminActionUserUpdate(),
      userActionGroupCreateBegin(),
      userActionGroupCreateCancel(),
      userActionGroupCreateReady(),
      userActionGroupInvite(),
      userActionGroupInviteCancel(),
      userActionImageCreate(),
      userActionImageRead(),
      userActionUserUserComplaintCreate()
    );
  }

  private static Arbitrary<EISecUserActionGroupCreateBegin> userActionGroupCreateBegin()
  {
    return Arbitraries.defaultFor(EIUser.class).flatMap(u -> {
      return Arbitraries.defaultFor(OffsetDateTime.class).flatMap(t -> {
        return Arbitraries.defaultFor(EIGroupCreationRequest.class)
          .list()
          .ofMinSize(0)
          .ofMaxSize(3)
          .flatMap(rs -> {
            return Arbitraries.defaultFor(EIGroupName.class).map(n -> {
              return new EISecUserActionGroupCreateBegin(
                u,
                t,
                ownRequests(
                  rs,
                  u.id()),
                n);
            });
          });
      });
    });
  }

  private static List<EIGroupCreationRequest> ownRequests(
    final List<EIGroupCreationRequest> requests,
    final UUID owner)
  {
    final var output = new ArrayList<EIGroupCreationRequest>(requests.size());
    for (int index = 0; index < requests.size(); ++index) {
      final var i =
        requests.get(index);
      final var result =
        new EIGroupCreationRequest(i.groupName(), owner, i.token(), i.status());
      output.add(result);
    }
    return output;
  }

  private static List<EIGroupInvite> ownInvites(
    final List<EIGroupInvite> invites,
    final UUID id)
  {
    final var output = new ArrayList<EIGroupInvite>(invites.size());
    for (int index = 0; index < invites.size(); ++index) {
      final var i =
        invites.get(index);
      final var result =
        new EIGroupInvite(
          id,
          i.userInvitingName(),
          i.userBeingInvited(),
          i.userBeingInvitedName(),
          i.group(),
          i.token(),
          i.status(),
          i.timeStarted(),
          i.timeCompleted());
      output.add(result);
    }
    return output;
  }

  private static Arbitrary<EISecUserActionGroupCreateCancel> userActionGroupCreateCancel()
  {
    return Arbitraries.defaultFor(EIUser.class).flatMap(u -> {
      return Arbitraries.defaultFor(EIGroupCreationRequest.class).map(r -> {
        return new EISecUserActionGroupCreateCancel(u, r);
      });
    });
  }

  private static Arbitrary<EISecUserActionGroupCreateReady> userActionGroupCreateReady()
  {
    return Arbitraries.defaultFor(EIUser.class).flatMap(u -> {
      return Arbitraries.defaultFor(EIGroupCreationRequest.class).map(r -> {
        return new EISecUserActionGroupCreateReady(u, r);
      });
    });
  }

  private static Arbitrary<EISecUserActionGroupInvite> userActionGroupInvite()
  {
    return Arbitraries.defaultFor(EIUser.class).flatMap(u0 -> {
      return Arbitraries.defaultFor(EIUser.class).flatMap(u1 -> {
        return Arbitraries.defaultFor(EIGroupName.class).flatMap(n -> {
          return Arbitraries.defaultFor(OffsetDateTime.class).flatMap(t -> {
            return Arbitraries.defaultFor(EIGroupInvite.class)
              .list()
              .ofMinSize(0)
              .ofMaxSize(5)
              .map(i -> {
                return new EISecUserActionGroupInvite(
                  u0,
                  u1,
                  n,
                  t,
                  ownInvites(i, u0.id())
                );
              });
          });
        });
      });
    });
  }

  private static Arbitrary<EISecUserActionGroupInviteCancel> userActionGroupInviteCancel()
  {
    return Arbitraries.defaultFor(EIUser.class).flatMap(u -> {
      return Arbitraries.defaultFor(EIGroupInvite.class).map(i -> {
        return new EISecUserActionGroupInviteCancel(u, i);
      });
    });
  }

  private static Arbitrary<EISecUserActionImageCreate> userActionImageCreate()
  {
    return Arbitraries.defaultFor(EIUser.class)
      .map(EISecUserActionImageCreate::new);
  }

  private static Arbitrary<EISecUserActionImageRead> userActionImageRead()
  {
    return Arbitraries.defaultFor(EIUser.class)
      .map(EISecUserActionImageRead::new);
  }

  private static Arbitrary<EISecUserActionUserUserComplaintCreate> userActionUserUserComplaintCreate()
  {
    return Arbitraries.defaultFor(EIUser.class).flatMap(u0 -> {
      return Arbitraries.defaultFor(EIUser.class).flatMap(u1 -> {
        return Arbitraries.integers().map(x -> {
          return new EISecUserActionUserUserComplaintCreate(u0, x, u1);
        });
      });
    });
  }

  private static Arbitrary<EISecAdminActionAdminCreate> adminActionAdminCreate()
  {
    return Arbitraries.defaultFor(EIAdmin.class)
      .flatMap(a -> {
        return Arbitraries.defaultFor(Set.class, EIAdminPermission.class)
          .map(ps -> {
            return new EISecAdminActionAdminCreate(
              a,
              (Set<EIAdminPermission>) ps);
          });
      });
  }

  private static Arbitrary<EISecAdminActionAdminRead> adminActionAdminRead()
  {
    return Arbitraries.defaultFor(EIAdmin.class)
      .map(EISecAdminActionAdminRead::new);
  }

  private static Arbitrary<EISecAdminActionAuditRead> adminActionAuditRead()
  {
    return Arbitraries.defaultFor(EIAdmin.class)
      .map(EISecAdminActionAuditRead::new);
  }

  private static Arbitrary<EISecAdminActionGroupInvites> adminActionGroupInvites()
  {
    return Arbitraries.defaultFor(EIAdmin.class)
      .map(EISecAdminActionGroupInvites::new);
  }

  private static Arbitrary<EISecAdminActionGroupInviteSetStatus> adminActionGroupInviteSetStatus()
  {
    return Arbitraries.defaultFor(EIAdmin.class)
      .map(EISecAdminActionGroupInviteSetStatus::new);
  }

  private static Arbitrary<EISecAdminActionServicesRead> adminActionServicesRead()
  {
    return Arbitraries.defaultFor(EIAdmin.class)
      .map(EISecAdminActionServicesRead::new);
  }

  private static Arbitrary<EISecAdminActionUserCreate> adminActionUserCreate()
  {
    return Arbitraries.defaultFor(EIAdmin.class)
      .flatMap(a -> {
        return Arbitraries.defaultFor(Set.class, EIAdminPermission.class)
          .map(ps -> {
            return new EISecAdminActionUserCreate(a);
          });
      });
  }

  private static Arbitrary<EISecAdminActionUserRead> adminActionUserRead()
  {
    return Arbitraries.defaultFor(EIAdmin.class)
      .map(EISecAdminActionUserRead::new);
  }

  private static Arbitrary<EISecAdminActionUserUnban> adminActionUserUnban()
  {
    return Arbitraries.defaultFor(EIAdmin.class)
      .map(EISecAdminActionUserUnban::new);
  }

  private static Arbitrary<EISecAdminActionUserUpdate> adminActionUserUpdate()
  {
    return Arbitraries.defaultFor(EIAdmin.class)
      .map(EISecAdminActionUserUpdate::new);
  }

  private static Arbitrary<EISecAdminActionUserBan> adminActionUserBan()
  {
    return Arbitraries.defaultFor(EIAdmin.class)
      .map(EISecAdminActionUserBan::new);
  }
}
