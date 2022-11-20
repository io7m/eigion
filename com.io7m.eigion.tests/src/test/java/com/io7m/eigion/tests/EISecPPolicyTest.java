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

import com.io7m.eigion.model.EIGroupCreationRequest;
import com.io7m.eigion.model.EIGroupCreationRequestStatusType.InProgress;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.server.internal.pike.security.EISecPActionGroupCreateBegin;
import com.io7m.eigion.server.internal.pike.security.EISecPActionGroupCreateCancel;
import com.io7m.eigion.server.internal.pike.security.EISecPActionGroupCreateReady;
import com.io7m.eigion.server.internal.pike.security.EISecPPolicy;
import com.io7m.eigion.server.internal.security.EISecurityException;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.SECURITY_POLICY_DENIED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class EISecPPolicyTest
{
  /**
   * If no requests have been made in the last hour, group creation is allowed
   * to begin.
   *
   * @param user      The user
   * @param timeNow   The current time
   * @param groupName The group name
   *
   * @throws Exception On errors
   */

  @Property
  public void testGroupCreateBeginOK(
    final @ForAll EIUser user,
    final @ForAll OffsetDateTime timeNow,
    final @ForAll EIGroupName groupName)
    throws Exception
  {
    final var policy =
      EISecPPolicy.policy();

    policy.check(
      new EISecPActionGroupCreateBegin(user, timeNow, List.of(), groupName)
    );
  }

  /**
   * If five or more requests have been made in the last hour, then creating a
   * new group is not allowed.
   *
   * @param user       The user
   * @param timeNow    The current time
   * @param groupNames The names used in requests
   * @param groupName  The group name
   *
   * @throws Exception On errors
   */

  @Property
  public void testGroupCreateBeginTooMany(
    final @ForAll EIUser user,
    final @ForAll OffsetDateTime timeNow,
    final @ForAll @Size(min = 5, max = 5) List<EIGroupName> groupNames,
    final @ForAll EIGroupName groupName)
    throws Exception
  {
    final var policy =
      EISecPPolicy.policy();

    final var status = new InProgress(timeNow);
    final var requests = List.of(
      new EIGroupCreationRequest(
        groupNames.get(0), user.id(), EIToken.generate(), status),
      new EIGroupCreationRequest(
        groupNames.get(1), user.id(), EIToken.generate(), status),
      new EIGroupCreationRequest(
        groupNames.get(2), user.id(), EIToken.generate(), status),
      new EIGroupCreationRequest(
        groupNames.get(3), user.id(), EIToken.generate(), status),
      new EIGroupCreationRequest(
        groupNames.get(4), user.id(), EIToken.generate(), status)
    );

    final var ex =
      assertThrows(EISecurityException.class, () -> {
        policy.check(
          new EISecPActionGroupCreateBegin(user, timeNow, requests, groupName)
        );
      });
    assertEquals(SECURITY_POLICY_DENIED, ex.errorCode());
  }

  /**
   * Users can only "ready" their own group requests.
   *
   * @param user      The user
   * @param timeNow   The current time
   * @param groupName The group name
   *
   * @throws Exception On errors
   */

  @Property
  public void testGroupCreateReadyOwn(
    final @ForAll EIUser user,
    final @ForAll OffsetDateTime timeNow,
    final @ForAll EIGroupName groupName)
    throws Exception
  {
    final var policy =
      EISecPPolicy.policy();

    final var ex =
      assertThrows(EISecurityException.class, () -> {
        policy.check(
          new EISecPActionGroupCreateReady(
            user,
            new EIGroupCreationRequest(
              groupName,
              UUID.randomUUID(),
              EIToken.generate(),
              new InProgress(timeNow)
            )
          )
        );
      });
    assertEquals(SECURITY_POLICY_DENIED, ex.errorCode());
  }

  /**
   * Users can only "cancel" their own group requests.
   *
   * @param user      The user
   * @param timeNow   The current time
   * @param groupName The group name
   *
   * @throws Exception On errors
   */

  @Property
  public void testGroupCreateCancelOwn(
    final @ForAll EIUser user,
    final @ForAll OffsetDateTime timeNow,
    final @ForAll EIGroupName groupName)
    throws Exception
  {
    final var policy =
      EISecPPolicy.policy();

    final var ex =
      assertThrows(EISecurityException.class, () -> {
        policy.check(
          new EISecPActionGroupCreateCancel(
            user,
            new EIGroupCreationRequest(
              groupName,
              UUID.randomUUID(),
              EIToken.generate(),
              new InProgress(timeNow)
            )
          )
        );
      });
    assertEquals(SECURITY_POLICY_DENIED, ex.errorCode());
  }

  /**
   * Users can only "ready" their own group requests.
   *
   * @param user      The user
   * @param timeNow   The current time
   * @param groupName The group name
   *
   * @throws Exception On errors
   */

  @Property
  public void testGroupCreateReadyOwnOK(
    final @ForAll EIUser user,
    final @ForAll OffsetDateTime timeNow,
    final @ForAll EIGroupName groupName)
    throws Exception
  {
    final var policy =
      EISecPPolicy.policy();

    policy.check(
      new EISecPActionGroupCreateReady(
        user,
        new EIGroupCreationRequest(
          groupName,
          user.id(),
          EIToken.generate(),
          new InProgress(timeNow)
        )
      )
    );
  }

  /**
   * Users can only "cancel" their own group requests.
   *
   * @param user      The user
   * @param timeNow   The current time
   * @param groupName The group name
   *
   * @throws Exception On errors
   */

  @Property
  public void testGroupCreateCancelOwnOK(
    final @ForAll EIUser user,
    final @ForAll OffsetDateTime timeNow,
    final @ForAll EIGroupName groupName)
    throws Exception
  {
    final var policy =
      EISecPPolicy.policy();

    policy.check(
      new EISecPActionGroupCreateCancel(
        user,
        new EIGroupCreationRequest(
          groupName,
          user.id(),
          EIToken.generate(),
          new InProgress(timeNow)
        )
      )
    );
  }
}
