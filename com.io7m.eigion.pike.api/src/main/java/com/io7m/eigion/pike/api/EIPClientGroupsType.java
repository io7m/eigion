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

import com.io7m.eigion.model.EIGroupCreationChallenge;
import com.io7m.eigion.model.EIGroupCreationRequest;
import com.io7m.eigion.model.EIGroupMembership;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIToken;

/**
 * Methods for manipulating groups.
 */

public interface EIPClientGroupsType
{
  /**
   * Start the creation of a group.
   *
   * @param groupName The group name
   *
   * @return The challenge that must be fulfilled to create the group
   *
   * @throws EIPClientException   On errors
   * @throws InterruptedException On interruption
   */

  EIGroupCreationChallenge groupCreateBegin(
    EIGroupName groupName)
    throws EIPClientException, InterruptedException;

  /**
   * Indicate that the challenge associated with the given token is ready for
   * checking.
   *
   * @param token The token
   *
   * @throws EIPClientException   On errors
   * @throws InterruptedException On interruption
   */

  void groupCreateReady(EIToken token)
    throws EIPClientException, InterruptedException;

  /**
   * Indicate that the challenge associated with the given token should be
   * cancelled.
   *
   * @param token The token
   *
   * @throws EIPClientException   On errors
   * @throws InterruptedException On interruption
   */

  void groupCreateCancel(EIToken token)
    throws EIPClientException, InterruptedException;

  /**
   * @return The set of groups in which the current user is a member
   *
   * @throws EIPClientException   On errors
   * @throws InterruptedException On interruption
   */

  EIPClientPagedType<EIGroupMembership> groups()
    throws EIPClientException, InterruptedException;

  /**
   * @return The user's group requests
   *
   * @throws EIPClientException   On errors
   * @throws InterruptedException On interruption
   */

  EIPClientPagedType<EIGroupCreationRequest> groupCreateRequests()
    throws EIPClientException, InterruptedException;
}
