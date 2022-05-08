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
import com.io7m.jattribute.core.AttributeReadableType;

import java.io.Closeable;
import java.util.List;

/**
 * The type of clients.
 */

public interface EIClientType extends Closeable
{
  /**
   * @return A stream of status events
   */

  AttributeReadableType<EIClientLoginStatusType> loginStatus();

  /**
   * @return The client's online/offline mode
   */

  AttributeReadableType<EIClientOnline> onlineStatus();

  /**
   * Set the online/offline mode.
   *
   * @param mode The mode
   */

  void onlineSet(EIClientOnline mode);

  /**
   * Attempt to log into the server.
   *
   * @param username The username
   * @param password The password
   *
   * @return A task representing the login attempt
   */

  EITask<Void> login(
    String username,
    String password);

  /**
   * Retrieve news from the server.
   *
   * @return A task representing the retrieved news
   */

  EITask<List<EIClientNewsItem>> news();
}
