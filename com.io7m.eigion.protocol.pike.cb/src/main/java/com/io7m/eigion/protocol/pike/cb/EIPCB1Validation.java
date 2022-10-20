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

package com.io7m.eigion.protocol.pike.cb;

import com.io7m.cedarbridge.runtime.api.CBCore;
import com.io7m.cedarbridge.runtime.api.CBList;
import com.io7m.cedarbridge.runtime.api.CBMap;
import com.io7m.cedarbridge.runtime.api.CBOptionType;
import com.io7m.cedarbridge.runtime.api.CBString;
import com.io7m.eigion.error_codes.EIErrorCode;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIGroupRole;
import com.io7m.eigion.model.EIPermission;
import com.io7m.eigion.model.EIPermissionSet;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.model.EIValidityException;
import com.io7m.eigion.protocol.api.EIProtocolException;
import com.io7m.eigion.protocol.api.EIProtocolMessageValidatorType;
import com.io7m.eigion.protocol.pike.EIPCommandLogin;
import com.io7m.eigion.protocol.pike.EIPCommandType;
import com.io7m.eigion.protocol.pike.EIPMessageType;
import com.io7m.eigion.protocol.pike.EIPResponseError;
import com.io7m.eigion.protocol.pike.EIPResponseLogin;
import com.io7m.eigion.protocol.pike.EIPResponseType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.io7m.cedarbridge.runtime.api.CBCore.string;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.eigion.protocol.pike.cb.internal.EIPCB1ValidationGeneral.fromWireUUID;
import static com.io7m.eigion.protocol.pike.cb.internal.EIPCB1ValidationGeneral.toWireUUID;

/**
 * Functions to translate between the core command set and the Pike v1
 * Cedarbridge encoding command set.
 */

public final class EIPCB1Validation
  implements EIProtocolMessageValidatorType<EIPMessageType, ProtocolPikev1Type>
{
  /**
   * Functions to translate between the core command set and the Admin v1
   * Cedarbridge encoding command set.
   */

  public EIPCB1Validation()
  {

  }

  private static ProtocolPikev1Type toWireResponse(
    final EIPResponseType r)
    throws EIProtocolException
  {
    if (r instanceof EIPResponseLogin rr) {
      return toWireResponseLogin(rr);
    }
    if (r instanceof EIPResponseError rr) {
      return toWireResponseError(rr);
    }

    throw new EIProtocolException(
      PROTOCOL_ERROR,
      "Unrecognized message: %s".formatted(r)
    );
  }

  private static ProtocolPikev1Type toWireResponseError(
    final EIPResponseError rr)
  {
    return new EIP1ResponseError(
      toWireUUID(rr.requestId()),
      string(rr.errorCode().id()),
      string(rr.message())
    );
  }

  private static ProtocolPikev1Type toWireResponseLogin(
    final EIPResponseLogin rr)
  {
    return new EIP1ResponseLogin(
      toWireUUID(rr.requestId()),
      toWireUser(rr.user())
    );
  }

  private static EIP1User toWireUser(
    final EIUser user)
  {
    return new EIP1User(
      toWireUUID(user.id()),
      toWirePermissionSet(user.permissions())
    );
  }

  private static CBMap<CBString, CBList<EIP1GroupRole>> toWireGroupMembership(
    final Map<EIGroupName, Set<EIGroupRole>> groupMembership)
  {
    final var r = new HashMap<CBString, CBList<EIP1GroupRole>>();
    for (final var e : groupMembership.entrySet()) {
      r.put(string(e.getKey().value()), toWireGroupRoleSet(e.getValue()));
    }
    return new CBMap<>(r);
  }

  private static CBList<EIP1GroupRole> toWireGroupRoleSet(
    final Set<EIGroupRole> value)
  {
    return new CBList<>(
      value.stream()
        .map(EIPCB1Validation::toWireGroupRole)
        .toList()
    );
  }

  private static EIP1GroupRole toWireGroupRole(
    final EIGroupRole r)
  {
    return switch (r) {
      case USER_INVITE -> new EIP1GroupRole.UserInvite();
      case USER_DISMISS -> new EIP1GroupRole.UserDismiss();
      case FOUNDER -> new EIP1GroupRole.Founder();
      case MEMBER -> new EIP1GroupRole.Member();
    };
  }

  private static CBList<EIP1Permission> toWirePermissionSet(
    final EIPermissionSet permissions)
  {
    return new CBList<>(
      permissions.permissions()
        .stream()
        .map(EIPCB1Validation::toWirePermission)
        .toList()
    );
  }

  private static EIP1Permission toWirePermission(
    final EIPermission p)
  {
    return switch (p) {
      case AMBERJACK_ACCESS -> new EIP1Permission.AmberjackAccess();
      case AUDIT_READ -> new EIP1Permission.AuditRead();
      case GROUP_CREATE -> new EIP1Permission.GroupCreate();
      case GROUP_READ -> new EIP1Permission.GroupRead();
      case GROUP_WRITE -> new EIP1Permission.GroupWrite();
    };
  }

  private static ProtocolPikev1Type toWireCommand(
    final EIPCommandType<?> c)
    throws EIProtocolException
  {
    if (c instanceof EIPCommandLogin cc) {
      return toWireCommandLogin(cc);
    }

    throw new EIProtocolException(
      PROTOCOL_ERROR,
      "Unrecognized message: %s".formatted(c)
    );
  }

  private static CBOptionType<CBString> toWireOptionalString(
    final Optional<String> s)
  {
    return CBOptionType.fromOptional(s.map(CBCore::string));
  }

  private static ProtocolPikev1Type toWireCommandLogin(
    final EIPCommandLogin cc)
  {
    return new EIP1CommandLogin(
      string(cc.userName()),
      string(cc.password())
    );
  }

  private static EIPMessageType fromWireResponseError(
    final EIP1ResponseError c)
  {
    return new EIPResponseError(
      fromWireUUID(c.fieldRequestId()),
      new EIErrorCode(c.fieldErrorCode().value()),
      c.fieldMessage().value()
    );
  }

  private static EIPMessageType fromWireResponseLogin(
    final EIP1ResponseLogin c)
  {
    return new EIPResponseLogin(
      fromWireUUID(c.fieldRequestId()),
      fromWireUser(c.fieldUser())
    );
  }

  private static EIUser fromWireUser(
    final EIP1User u)
  {
    return new EIUser(
      fromWireUUID(u.fieldId()),
      fromWirePermissionSet(u.fieldPermissions())
    );
  }

  private static Map<EIGroupName, Set<EIGroupRole>> fromWireGroupMembership(
    final CBMap<CBString, CBList<EIP1GroupRole>> m)
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
    final CBList<EIP1GroupRole> value)
  {
    return value.values()
      .stream()
      .map(EIPCB1Validation::fromWireGroupRole)
      .collect(Collectors.toUnmodifiableSet());
  }

  private static EIGroupRole fromWireGroupRole(
    final EIP1GroupRole r)
  {
    if (r instanceof EIP1GroupRole.Founder) {
      return EIGroupRole.FOUNDER;
    }
    if (r instanceof EIP1GroupRole.UserDismiss) {
      return EIGroupRole.USER_DISMISS;
    }
    if (r instanceof EIP1GroupRole.UserInvite) {
      return EIGroupRole.USER_INVITE;
    }

    throw new EIValidityException(
      "Unrecognized group role: %s".formatted(r)
    );
  }

  private static EIPermissionSet fromWirePermissionSet(
    final CBList<EIP1Permission> ps)
  {
    return EIPermissionSet.of(
      ps.values()
        .stream()
        .map(EIPCB1Validation::fromWirePermission)
        .toList()
    );
  }

  private static EIPermission fromWirePermission(
    final EIP1Permission p)
  {
    if (p instanceof EIP1Permission.AmberjackAccess) {
      return EIPermission.AMBERJACK_ACCESS;
    }
    if (p instanceof EIP1Permission.AuditRead) {
      return EIPermission.AUDIT_READ;
    }
    if (p instanceof EIP1Permission.GroupCreate) {
      return EIPermission.GROUP_CREATE;
    }
    if (p instanceof EIP1Permission.GroupWrite) {
      return EIPermission.GROUP_WRITE;
    }
    if (p instanceof EIP1Permission.GroupRead) {
      return EIPermission.GROUP_READ;
    }

    throw new EIValidityException(
      "Unrecognized permission: %s".formatted(p)
    );
  }

  private static EIPMessageType fromWireCommandLogin(
    final EIP1CommandLogin c)
  {
    return new EIPCommandLogin(
      c.fieldUserName().value(),
      c.fieldPassword().value()
    );
  }

  private static Optional<String> fromWireOptionalString(
    final CBOptionType<CBString> f)
  {
    return f.asOptional().map(CBString::value);
  }

  @Override
  public ProtocolPikev1Type convertToWire(
    final EIPMessageType message)
    throws EIProtocolException
  {
    try {
      if (message instanceof EIPCommandType<?> c) {
        return toWireCommand(c);
      }
      if (message instanceof EIPResponseType r) {
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
  public EIPMessageType convertFromWire(
    final ProtocolPikev1Type message)
    throws EIProtocolException
  {
    try {
      if (message instanceof EIP1CommandLogin c) {
        return fromWireCommandLogin(c);
      }
      if (message instanceof EIP1ResponseLogin c) {
        return fromWireResponseLogin(c);
      }
      if (message instanceof EIP1ResponseError c) {
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
