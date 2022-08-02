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

import com.io7m.eigion.model.EIAdminPermission;
import com.io7m.eigion.protocol.api.EIProtocolFromModel;
import com.io7m.eigion.protocol.api.EIProtocolToModel;

/**
 * The type of admin permissions.
 */

public enum EISA1AdminPermission
{
  /**
   * A permission that allows for creating/editing admins.
   */

  ADMIN_CREATE,

  /**
   * A permission that allows reading admins.
   */

  ADMIN_READ,

  /**
   * A permission that allows reading the audit log.
   */

  AUDIT_READ,

  /**
   * A permission that allows creating/editing users.
   */

  USER_WRITE,

  /**
   * A permission that allows reading users.
   */

  USER_READ,

  /**
   * A permission that allows reading services.
   */

  SERVICE_READ,

  /**
   * A permission that allows reading group invites.
   */

  GROUP_INVITES_READ,

  /**
   * A permission that allows writing group invites.
   */

  GROUP_INVITES_WRITE;

  /**
   * @param p The model permission
   *
   * @return The given model permission as a v1 permission
   */

  @EIProtocolFromModel
  public static EISA1AdminPermission ofAdmin(
    final EIAdminPermission p)
  {
    return switch (p) {
      case ADMIN_CREATE -> ADMIN_CREATE;
      case ADMIN_READ -> ADMIN_READ;
      case AUDIT_READ -> AUDIT_READ;
      case SERVICE_READ -> SERVICE_READ;
      case USER_READ -> USER_READ;
      case USER_WRITE -> USER_WRITE;
      case GROUP_INVITES_READ -> GROUP_INVITES_READ;
      case GROUP_INVITES_WRITE -> GROUP_INVITES_WRITE;
    };
  }

  /**
   * @return This permission as a model permission
   */

  @EIProtocolToModel
  public EIAdminPermission toAdmin()
  {
    return switch (this) {
      case ADMIN_CREATE -> EIAdminPermission.ADMIN_CREATE;
      case ADMIN_READ -> EIAdminPermission.ADMIN_READ;
      case AUDIT_READ -> EIAdminPermission.AUDIT_READ;
      case SERVICE_READ -> EIAdminPermission.SERVICE_READ;
      case USER_READ -> EIAdminPermission.USER_READ;
      case USER_WRITE -> EIAdminPermission.USER_WRITE;
      case GROUP_INVITES_READ -> EIAdminPermission.GROUP_INVITES_READ;
      case GROUP_INVITES_WRITE -> EIAdminPermission.GROUP_INVITES_WRITE;
    };
  }
}
