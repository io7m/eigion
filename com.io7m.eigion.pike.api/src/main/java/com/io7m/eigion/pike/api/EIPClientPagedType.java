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


package com.io7m.eigion.pike.api;

import com.io7m.eigion.model.EIPage;

/**
 * A paged client command.
 *
 * @param <T> The type of returned values
 */

public interface EIPClientPagedType<T>
{
  /**
   * @return The current page of results
   *
   * @throws EIPClientException  On errors
   * @throws InterruptedException On interruption
   */

  EIPage<T> current()
    throws EIPClientException, InterruptedException;

  /**
   * @return The next page of results
   *
   * @throws EIPClientException  On errors
   * @throws InterruptedException On interruption
   */

  EIPage<T> next()
    throws EIPClientException, InterruptedException;

  /**
   * @return The previous page of results
   *
   * @throws EIPClientException  On errors
   * @throws InterruptedException On interruption
   */

  EIPage<T> previous()
    throws EIPClientException, InterruptedException;
}
