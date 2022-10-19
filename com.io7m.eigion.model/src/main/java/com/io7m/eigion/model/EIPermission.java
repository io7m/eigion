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

package com.io7m.eigion.model;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The type of permissions in the system.
 */

public enum EIPermission
{
  /**
   * Access to the Amberjack API is allowed.
   */

  AMBERJACK_ACCESS {
    @Override
    public int value()
    {
      return 0;
    }
  },

  /**
   * Access to the audit log is allowed.
   */

  AUDIT_READ {
    @Override
    public int value()
    {
      return 1;
    }
  },

  /**
   * Group creation is allowed.
   */

  GROUP_CREATE {
    @Override
    public int value()
    {
      return 2;
    }
  },

  /**
   * Group modification is allowed.
   */

  GROUP_WRITE {
    @Override
    public int value()
    {
      return 3;
    }
  },

  /**
   * Group reading/searching is allowed.
   */

  GROUP_READ {
    @Override
    public int value()
    {
      return 4;
    }
  };

  private static final Map<Integer, EIPermission> VALUES =
    Stream.of(values())
      .collect(Collectors.toMap(
        v -> Integer.valueOf(v.value()), Function.identity())
      );

  /**
   * @return The integer permission value
   */

  public abstract int value();

  /**
   * @param x The integer value
   *
   * @return The permission of the given integer
   *
   * @throws EIValidityException On unrecognized values
   * @see #value()
   */

  public static EIPermission ofInteger(
    final int x)
    throws EIValidityException
  {
    return Optional.ofNullable(VALUES.get(Integer.valueOf(x)))
      .orElseThrow(() -> {
        return new EIValidityException(
          "No such permission: %d".formatted(Integer.valueOf(x))
        );
      });
  }
}
