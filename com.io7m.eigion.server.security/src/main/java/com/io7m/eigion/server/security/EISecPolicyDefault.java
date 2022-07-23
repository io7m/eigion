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

import java.util.Objects;

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

  @Override
  public EISecPolicyResultType check(
    final EISecActionType action)
  {
    Objects.requireNonNull(action, "action");

    if (action instanceof EISecActionImageRead a) {
      return this.checkActionImageRead(a);
    }
    if (action instanceof EISecActionImageCreate a) {
      return this.checkActionImageCreate(a);
    }
    if (action instanceof EISecActionUserUserComplaintCreate c) {
      return this.checkUserUserComplaintCreate(c);
    }

    return new EISecPolicyResultDenied("Operation not permitted.");
  }

  private EISecPolicyResultType checkUserUserComplaintCreate(
    final EISecActionUserUserComplaintCreate c)
  {
    return new EISecPolicyResultPermitted();
  }

  private EISecPolicyResultType checkActionImageRead(
    final EISecActionImageRead a)
  {
    return new EISecPolicyResultPermitted();
  }

  private EISecPolicyResultType checkActionImageCreate(
    final EISecActionImageCreate a)
  {
    return new EISecPolicyResultPermitted();
  }
}
