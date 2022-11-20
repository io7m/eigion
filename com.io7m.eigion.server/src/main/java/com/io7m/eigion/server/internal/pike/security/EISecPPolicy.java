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

  @Override
  public EISecPolicyResultType evaluate(
    final EISecPActionType action)
  {
    Objects.requireNonNull(action, "action");

    if (action instanceof EISecPActionGroupCreateBegin a) {
      return evaluateGroupCreateBegin(a);
    }
    if (action instanceof EISecPActionGroupCreateReady a) {
      return evaluateGroupCreateReady(a);
    }
    if (action instanceof EISecPActionGroupCreateCancel a) {
      return evaluateGroupCreateCancel(a);
    }

    return new EISecPolicyResultDenied("Operation not permitted.");
  }

  private static EISecPolicyResultType evaluateGroupCreateReady(
    final EISecPActionGroupCreateReady c)
  {
    if (!Objects.equals(c.user().id(), c.request().userFounder())) {
      return new EISecPolicyResultDenied(
        "Users may only ready their own group creation requests."
      );
    }

    return new EISecPolicyResultPermitted();
  }

  private static EISecPolicyResultType evaluateGroupCreateCancel(
    final EISecPActionGroupCreateCancel c)
  {
    if (!Objects.equals(c.user().id(), c.request().userFounder())) {
      return new EISecPolicyResultDenied(
        "Users may only cancel their own group creation requests."
      );
    }

    return new EISecPolicyResultPermitted();
  }

  private static EISecPolicyResultType evaluateGroupCreateBegin(
    final EISecPActionGroupCreateBegin c)
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
}
