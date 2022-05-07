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


package com.io7m.eigion.client.api;

import com.io7m.eigion.taskrecorder.EITask;

import java.util.Objects;

/**
 * The client status.
 */

public sealed interface EIClientStatusType
{
  /**
   * The client is in the initial state.
   */

  enum EIClientStatusInitial implements EIClientStatusType
  {
    /**
     * The client is in the initial state.
     */

    CLIENT_STATUS_INITIAL
  }

  /**
   * The client is currently attempting to log in.
   */

  enum EIClientStatusLoggingIn implements EIClientStatusType
  {
    /**
     * The client is currently attempting to log in.
     */

    CLIENT_STATUS_LOGGING_IN
  }

  /**
   * The client failed to log in.
   *
   * @param task The login task
   */

  record EIClientStatusLoginFailed(EITask<Void> task)
    implements EIClientStatusType
  {
    /**
     * The client failed to log in.
     */

    public EIClientStatusLoginFailed
    {
      Objects.requireNonNull(task, "task");
    }
  }

  /**
   * The client is currently logged in.
   */

  enum EIClientStatusLoggedIn implements EIClientStatusType
  {
    /**
     * The client is currently logged in.
     */

    CLIENT_STATUS_LOGGED_IN
  }
}
