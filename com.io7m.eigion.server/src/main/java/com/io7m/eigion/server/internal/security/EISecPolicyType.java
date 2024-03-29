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

package com.io7m.eigion.server.internal.security;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.SECURITY_POLICY_DENIED;

/**
 * A security policy.
 *
 * @param <A> The precise type of actions controlled by the policy
 */

public interface EISecPolicyType<A extends EISecActionType>
{
  /**
   * Check that a user is allowed to perform an action by the current policy.
   *
   * @param action The action
   *
   * @return A value indicating if the action is permitted
   *
   * @throws EISecurityException On evaluation errors
   */

  EISecPolicyResultType evaluate(
    A action)
    throws EISecurityException;

  /**
   * Check that a user is allowed to perform an action by the current policy.
   *
   * @param action The action
   *
   * @throws EISecurityException If the action is denied by the policy
   */

  default void check(
    final A action)
    throws EISecurityException
  {
    if (this.evaluate(action) instanceof EISecPolicyResultDenied denied) {
      throw new EISecurityException(denied.message(), SECURITY_POLICY_DENIED);
    }
  }
}
