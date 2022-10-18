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

package com.io7m.eigion.protocol.amberjack.cb;

import com.io7m.cedarbridge.runtime.api.CBList;
import com.io7m.cedarbridge.runtime.api.CBMap;
import com.io7m.cedarbridge.runtime.api.CBString;
import com.io7m.eigion.error_codes.EIErrorCode;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIGroupRole;
import com.io7m.eigion.model.EIPermission;
import com.io7m.eigion.model.EIPermissionSet;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.model.EIValidityException;
import com.io7m.eigion.protocol.amberjack.EIAJCommandLogin;
import com.io7m.eigion.protocol.amberjack.EIAJCommandType;
import com.io7m.eigion.protocol.amberjack.EIAJMessageType;
import com.io7m.eigion.protocol.amberjack.EIAJResponseError;
import com.io7m.eigion.protocol.amberjack.EIAJResponseLogin;
import com.io7m.eigion.protocol.amberjack.EIAJResponseType;
import com.io7m.eigion.protocol.api.EIProtocolException;
import com.io7m.eigion.protocol.api.EIProtocolMessageValidatorType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.io7m.cedarbridge.runtime.api.CBCore.string;
import static com.io7m.cedarbridge.runtime.api.CBCore.unsigned64;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.PROTOCOL_ERROR;

/**
 * Functions to translate between the core command set and the Amberjack v1
 * Cedarbridge encoding command set.
 */

public final class EIAJCB1Validation
  implements EIProtocolMessageValidatorType<EIAJMessageType, ProtocolAmberjackv1Type>
{
  /**
   * Functions to translate between the core command set and the Admin v1
   * Cedarbridge encoding command set.
   */

  public EIAJCB1Validation()
  {

  }

  private static ProtocolAmberjackv1Type toWireResponse(
    final EIAJResponseType r)
    throws EIProtocolException
  {
    if (r instanceof EIAJResponseLogin rr) {
      return toWireResponseLogin(rr);
    }
    if (r instanceof EIAJResponseError rr) {
      return toWireResponseError(rr);
    }

    throw new EIProtocolException(
      PROTOCOL_ERROR,
      "Unrecognized message: %s".formatted(r)
    );
  }

  private static ProtocolAmberjackv1Type toWireResponseError(
    final EIAJResponseError rr)
  {
    return new EIAJ1ResponseError(
      toWireUUID(rr.requestId()),
      string(rr.errorCode().id()),
      string(rr.message())
    );
  }

  private static EIAJ1UUID toWireUUID(
    final UUID requestId)
  {
    return new EIAJ1UUID(
      unsigned64(requestId.getMostSignificantBits()),
      unsigned64(requestId.getLeastSignificantBits())
    );
  }

  private static ProtocolAmberjackv1Type toWireResponseLogin(
    final EIAJResponseLogin rr)
  {
    return new EIAJ1ResponseLogin(
      toWireUUID(rr.requestId()),
      toWireUser(rr.user())
    );
  }

  private static EIAJ1User toWireUser(
    final EIUser user)
  {
    return new EIAJ1User(
      toWireUUID(user.id()),
      toWirePermissionSet(user.permissions()),
      toWireGroupMembership(user.groupMembership())
    );
  }

  private static CBMap<CBString, CBList<EIAJ1GroupRole>> toWireGroupMembership(
    final Map<EIGroupName, Set<EIGroupRole>> groupMembership)
  {
    final var r = new HashMap<CBString, CBList<EIAJ1GroupRole>>();
    for (final var e : groupMembership.entrySet()) {
      r.put(string(e.getKey().value()), toWireGroupRoleSet(e.getValue()));
    }
    return new CBMap<>(r);
  }

  private static CBList<EIAJ1GroupRole> toWireGroupRoleSet(
    final Set<EIGroupRole> value)
  {
    return new CBList<>(
      value.stream()
        .map(EIAJCB1Validation::toWireGroupRole)
        .toList()
    );
  }

  private static EIAJ1GroupRole toWireGroupRole(
    final EIGroupRole r)
  {
    return switch (r) {
      case USER_INVITE -> new EIAJ1GroupRole.UserInvite();
      case USER_DISMISS -> new EIAJ1GroupRole.UserDismiss();
      case FOUNDER -> new EIAJ1GroupRole.Founder();
    };
  }

  private static CBList<EIAJ1Permission> toWirePermissionSet(
    final EIPermissionSet permissions)
  {
    return new CBList<>(
      permissions.permissions()
        .stream()
        .map(EIAJCB1Validation::toWirePermission)
        .toList()
    );
  }

  private static EIAJ1Permission toWirePermission(
    final EIPermission p)
  {
    return switch (p) {
      case AMBERJACK_ACCESS -> new EIAJ1Permission.AmberjackAccess();
    };
  }

  private static ProtocolAmberjackv1Type toWireCommand(
    final EIAJCommandType<?> c)
    throws EIProtocolException
  {
    if (c instanceof EIAJCommandLogin cc) {
      return toWireCommandLogin(cc);
    }

    throw new EIProtocolException(
      PROTOCOL_ERROR,
      "Unrecognized message: %s".formatted(c)
    );
  }

  private static ProtocolAmberjackv1Type toWireCommandLogin(
    final EIAJCommandLogin cc)
  {
    return new EIAJ1CommandLogin(
      string(cc.userName()),
      string(cc.password())
    );
  }

  private static EIAJMessageType fromWireResponseError(
    final EIAJ1ResponseError c)
  {
    return new EIAJResponseError(
      fromWireUUID(c.fieldRequestId()),
      new EIErrorCode(c.fieldErrorCode().value()),
      c.fieldMessage().value()
    );
  }

  private static UUID fromWireUUID(
    final EIAJ1UUID id)
  {
    return new UUID(
      id.fieldMsb().value(),
      id.fieldLsb().value()
    );
  }

  private static EIAJMessageType fromWireResponseLogin(
    final EIAJ1ResponseLogin c)
  {
    return new EIAJResponseLogin(
      fromWireUUID(c.fieldRequestId()),
      fromWireUser(c.fieldUser())
    );
  }

  private static EIUser fromWireUser(
    final EIAJ1User u)
  {
    return new EIUser(
      fromWireUUID(u.fieldId()),
      fromWirePermissionSet(u.fieldPermissions()),
      fromWireGroupMembership(u.fieldGroupMembership())
    );
  }

  private static Map<EIGroupName, Set<EIGroupRole>> fromWireGroupMembership(
    final CBMap<CBString, CBList<EIAJ1GroupRole>> m)
  {
    final var r = new HashMap<EIGroupName, Set<EIGroupRole>>();
    for (final var e : m.values().entrySet()) {
      r.put(
        new EIGroupName(e.getKey().value()),
        fromWireGroupRoleSet(e.getValue())
      );
    }
    return r;
  }

  private static Set<EIGroupRole> fromWireGroupRoleSet(
    final CBList<EIAJ1GroupRole> value)
  {
    return value.values()
      .stream()
      .map(EIAJCB1Validation::fromWireGroupRole)
      .collect(Collectors.toUnmodifiableSet());
  }

  private static EIGroupRole fromWireGroupRole(
    final EIAJ1GroupRole r)
  {
    if (r instanceof EIAJ1GroupRole.Founder) {
      return EIGroupRole.FOUNDER;
    }
    if (r instanceof EIAJ1GroupRole.UserDismiss) {
      return EIGroupRole.USER_DISMISS;
    }
    if (r instanceof EIAJ1GroupRole.UserInvite) {
      return EIGroupRole.USER_INVITE;
    }

    throw new EIValidityException(
      "Unrecognized group role: %s".formatted(r)
    );
  }

  private static EIPermissionSet fromWirePermissionSet(
    final CBList<EIAJ1Permission> ps)
  {
    return EIPermissionSet.of(
      ps.values()
        .stream()
        .map(EIAJCB1Validation::fromWirePermission)
        .toList()
    );
  }

  private static EIPermission fromWirePermission(
    final EIAJ1Permission p)
  {
    if (p instanceof EIAJ1Permission.AmberjackAccess) {
      return EIPermission.AMBERJACK_ACCESS;
    }

    throw new EIValidityException(
      "Unrecognized permission: %s".formatted(p)
    );
  }

  private static EIAJMessageType fromWireCommandLogin(
    final EIAJ1CommandLogin c)
  {
    return new EIAJCommandLogin(
      c.fieldUserName().value(),
      c.fieldPassword().value()
    );
  }

  @Override
  public ProtocolAmberjackv1Type convertToWire(
    final EIAJMessageType message)
    throws EIProtocolException
  {
    try {
      if (message instanceof EIAJCommandType<?> c) {
        return toWireCommand(c);
      }
      if (message instanceof EIAJResponseType r) {
        return toWireResponse(r);
      }
    } catch (final Exception e) {
      throw new EIProtocolException(PROTOCOL_ERROR, e.getMessage(), e);
    }

    throw new EIProtocolException(
      PROTOCOL_ERROR,
      "Unrecognized message: %s".formatted(message)
    );
  }

  @Override
  public EIAJMessageType convertFromWire(
    final ProtocolAmberjackv1Type message)
    throws EIProtocolException
  {
    try {
      if (message instanceof EIAJ1CommandLogin c) {
        return fromWireCommandLogin(c);
      }
      if (message instanceof EIAJ1ResponseLogin c) {
        return fromWireResponseLogin(c);
      }
      if (message instanceof EIAJ1ResponseError c) {
        return fromWireResponseError(c);
      }
    } catch (final Exception e) {
      throw new EIProtocolException(PROTOCOL_ERROR, e.getMessage(), e);
    }

    throw new EIProtocolException(
      PROTOCOL_ERROR,
      "Unrecognized message: %s".formatted(message)
    );
  }
}
