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

package com.io7m.eigion.tests.service.limits;

import com.io7m.eigion.server.service.limits.EIRequestLimitExceeded;
import com.io7m.eigion.server.service.limits.EIRequestLimits;
import com.io7m.eigion.server.service.limits.EIRequestLimitsType;
import com.io7m.eigion.tests.EIFakeServletInputStream;
import com.io7m.eigion.tests.service.EIServiceContract;
import com.io7m.idstore.server.service.reqlimit.IdRequestLimitExceeded;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class EISRequestLimitsTest
  extends EIServiceContract<EIRequestLimitsType>
{
  private EIRequestLimits limits;

  @Override
  protected EIRequestLimitsType  createInstanceA()
  {
    return new EIRequestLimits(size -> "too big");
  }

  @Override
  protected EIRequestLimitsType  createInstanceB()
  {
    return new EIRequestLimits(size -> "not small enough");
  }

  @BeforeEach
  public void setup()
  {
    this.limits = new EIRequestLimits("size %d is too large"::formatted);
  }

  /**
   * It's only possible to read the limited data, even if the request specifies
   * fewer bytes than are actually provided.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLimitNotExceeded()
    throws Exception
  {
    final var request =
      Mockito.mock(HttpServletRequest.class);

    final var data = new byte[200];
    SecureRandom.getInstanceStrong().nextBytes(data);
    final var slice = new byte[20];
    System.arraycopy(data, 0, slice, 0, 20);

    final var realStream =
      new ByteArrayInputStream(data);

    Mockito.when(Integer.valueOf(request.getContentLength()))
      .thenReturn(Integer.valueOf(20));

    final var stream =
      new EIFakeServletInputStream(realStream);

    Mockito.when(request.getInputStream())
      .thenReturn(stream);

    final var input =
      this.limits.boundedMaximumInput(request, 100);

    assertArrayEquals(
      slice,
      input.readAllBytes()
    );
  }

  /**
   * Exceeding the size limit is not allowed.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLimitExceeded()
    throws Exception
  {
    final var request =
      Mockito.mock(HttpServletRequest.class);
    Mockito.when(Integer.valueOf(request.getContentLength()))
      .thenReturn(Integer.valueOf(101));

    final var ex =
      assertThrows(EIRequestLimitExceeded.class, () -> {
        this.limits.boundedMaximumInput(request, 100);
      });

    assertEquals(101L, ex.sizeProvided());
    assertEquals(100L, ex.sizeLimit());
    assertEquals("size 101 is too large", ex.getMessage());
  }

  /**
   * It's possible to read the full data if no limit is provided.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLimitUnlimited()
    throws Exception
  {
    final var request =
      Mockito.mock(HttpServletRequest.class);

    final var data = new byte[200];
    SecureRandom.getInstanceStrong().nextBytes(data);

    final var realStream =
      new ByteArrayInputStream(data);

    Mockito.when(Integer.valueOf(request.getContentLength()))
      .thenReturn(Integer.valueOf(-1));

    final var stream =
      new EIFakeServletInputStream(realStream);

    Mockito.when(request.getInputStream())
      .thenReturn(stream);

    final var input =
      this.limits.boundedMaximumInput(request, 200);

    assertArrayEquals(
      data,
      input.readAllBytes()
    );
  }
}
