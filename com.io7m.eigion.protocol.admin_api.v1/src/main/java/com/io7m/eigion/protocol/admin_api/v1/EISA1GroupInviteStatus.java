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

package com.io7m.eigion.protocol.admin_api.v1;

import com.io7m.eigion.model.EIGroupInviteStatus;
import com.io7m.eigion.protocol.api.EIProtocolFromModel;
import com.io7m.eigion.protocol.api.EIProtocolToModel;

/**
 * The status of an invite.
 */

public enum EISA1GroupInviteStatus
{
  /**
   * The invite is in progress.
   */

  IN_PROGRESS,

  /**
   * The invite has been accepted.
   */

  ACCEPTED,

  /**
   * The invite has been rejected.
   */

  REJECTED,

  /**
   * The invite has been cancelled.
   */

  CANCELLED;

  /**
   * @return This status as a model status
   */

  @EIProtocolToModel
  public EIGroupInviteStatus toStatus()
  {
    return switch (this) {
      case ACCEPTED -> EIGroupInviteStatus.ACCEPTED;
      case REJECTED -> EIGroupInviteStatus.REJECTED;
      case CANCELLED -> EIGroupInviteStatus.CANCELLED;
      case IN_PROGRESS -> EIGroupInviteStatus.IN_PROGRESS;
    };
  }

  /**
   * The given status as a v1 status.
   *
   * @param status The status
   *
   * @return A v1 status
   */

  @EIProtocolFromModel
  public static EISA1GroupInviteStatus ofStatus(
    final EIGroupInviteStatus status)
  {
    return switch (status) {
      case IN_PROGRESS -> IN_PROGRESS;
      case ACCEPTED -> ACCEPTED;
      case REJECTED -> REJECTED;
      case CANCELLED -> CANCELLED;
    };
  }
}
