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

package com.io7m.eigion.tests.service;

import com.io7m.eigion.services.api.EIServiceType;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Base service contract.
 *
 * @param <T> The type of service
 */

public abstract class EIServiceContract<T extends EIServiceType>
{
  private static final Pattern TO_STRING =
    Pattern.compile("\\[[A-Z0-9]+ 0x[A-F0-9]+\\]", CASE_INSENSITIVE);

  protected abstract T createInstanceA();

  protected abstract T createInstanceB();

  /**
   * The string representation for a service varies with the service instance.
   */

  @Test
  public void testToString()
  {
    final var x = this.createInstanceA();
    final var y = this.createInstanceB();

    assertNotEquals(x.toString(), y.toString());

    assertTrue(TO_STRING.matcher(x.toString()).matches());
    assertTrue(TO_STRING.matcher(y.toString()).matches());
  }

  /**
   * The description for a service does not vary with the service instance.
   */

  @Test
  public void testDescription()
  {
    final var x = this.createInstanceA();
    final var y = this.createInstanceB();

    assertEquals(x.description(), y.description());
  }
}
