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
 * A security policy that permits all actions.
 */

public final class EISecPolicyPermitAll implements EISecPolicyType
{
  private static final EISecPolicyPermitAll INSTANCE =
    new EISecPolicyPermitAll();

  private EISecPolicyPermitAll()
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
    return new EISecPolicyResultPermitted();
  }
}
