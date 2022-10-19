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

import com.io7m.cedarbridge.runtime.api.CBCore;
import com.io7m.cedarbridge.runtime.api.CBList;
import com.io7m.cedarbridge.runtime.api.CBMap;
import com.io7m.cedarbridge.runtime.api.CBOptionType;
import com.io7m.cedarbridge.runtime.api.CBString;
import com.io7m.eigion.error_codes.EIErrorCode;
import com.io7m.eigion.model.EIAuditSearchParameters;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIGroupRole;
import com.io7m.eigion.model.EIPermission;
import com.io7m.eigion.model.EIPermissionSet;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.model.EIValidityException;
import com.io7m.eigion.protocol.amberjack.EIAJCommandAuditSearchBegin;
import com.io7m.eigion.protocol.amberjack.EIAJCommandAuditSearchNext;
import com.io7m.eigion.protocol.amberjack.EIAJCommandAuditSearchPrevious;
import com.io7m.eigion.protocol.amberjack.EIAJCommandGroupCreate;
import com.io7m.eigion.protocol.amberjack.EIAJCommandLogin;
import com.io7m.eigion.protocol.amberjack.EIAJCommandType;
import com.io7m.eigion.protocol.amberjack.EIAJMessageType;
import com.io7m.eigion.protocol.amberjack.EIAJResponseAuditSearch;
import com.io7m.eigion.protocol.amberjack.EIAJResponseError;
import com.io7m.eigion.protocol.amberjack.EIAJResponseGroupCreate;
import com.io7m.eigion.protocol.amberjack.EIAJResponseLogin;
import com.io7m.eigion.protocol.amberjack.EIAJResponseType;
import com.io7m.eigion.protocol.amberjack.cb.internal.EIAJCB1ValidationGeneral;
import com.io7m.eigion.protocol.api.EIProtocolException;
import com.io7m.eigion.protocol.api.EIProtocolMessageValidatorType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.io7m.cedarbridge.runtime.api.CBCore.string;
import static com.io7m.cedarbridge.runtime.api.CBCore.unsigned16;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.eigion.protocol.amberjack.cb.internal.EIAJCB1ValidationGeneral.fromWirePage;
import static com.io7m.eigion.protocol.amberjack.cb.internal.EIAJCB1ValidationGeneral.fromWireTimeRange;
import static com.io7m.eigion.protocol.amberjack.cb.internal.EIAJCB1ValidationGeneral.fromWireUUID;
import static com.io7m.eigion.protocol.amberjack.cb.internal.EIAJCB1ValidationGeneral.toWirePage;
import static com.io7m.eigion.protocol.amberjack.cb.internal.EIAJCB1ValidationGeneral.toWireTimeRange;
import static com.io7m.eigion.protocol.amberjack.cb.internal.EIAJCB1ValidationGeneral.toWireUUID;

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
    if (r instanceof EIAJResponseGroupCreate rr) {
      return toWireResponseGroupCreate(rr);
    }
    if (r instanceof EIAJResponseAuditSearch rr) {
      return toWireResponseAuditSearch(rr);
    }

    throw new EIProtocolException(
      PROTOCOL_ERROR,
      "Unrecognized message: %s".formatted(r)
    );
  }

  private static ProtocolAmberjackv1Type toWireResponseAuditSearch(
    final EIAJResponseAuditSearch rr)
  {
    return new EIAJ1ResponseAuditSearch(
      toWireUUID(rr.requestId()),
      toWirePage(rr.page(), EIAJCB1ValidationGeneral::toWireAuditEvent)
    );
  }

  private static ProtocolAmberjackv1Type toWireResponseGroupCreate(
    final EIAJResponseGroupCreate rr)
  {
    return new EIAJ1ResponseGroupCreate(
      toWireUUID(rr.requestId())
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
      toWirePermissionSet(user.permissions())
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
      case MEMBER -> new EIAJ1GroupRole.Member();
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
      case AUDIT_READ -> new EIAJ1Permission.AuditRead();
      case GROUP_CREATE -> new EIAJ1Permission.GroupCreate();
      case GROUP_MODIFY -> new EIAJ1Permission.GroupModify();
    };
  }

  private static ProtocolAmberjackv1Type toWireCommand(
    final EIAJCommandType<?> c)
    throws EIProtocolException
  {
    if (c instanceof EIAJCommandLogin cc) {
      return toWireCommandLogin(cc);
    }
    if (c instanceof EIAJCommandGroupCreate cc) {
      return toWireCommandGroupCreate(cc);
    }
    if (c instanceof EIAJCommandAuditSearchBegin cc) {
      return toWireCommandAuditSearchBegin(cc);
    }
    if (c instanceof EIAJCommandAuditSearchNext cc) {
      return toWireCommandAuditSearchNext(cc);
    }
    if (c instanceof EIAJCommandAuditSearchPrevious cc) {
      return toWireCommandAuditSearchPrevious(cc);
    }

    throw new EIProtocolException(
      PROTOCOL_ERROR,
      "Unrecognized message: %s".formatted(c)
    );
  }

  private static ProtocolAmberjackv1Type toWireCommandAuditSearchNext(
    final EIAJCommandAuditSearchNext cc)
  {
    return new EIAJ1CommandAuditSearchNext();
  }

  private static ProtocolAmberjackv1Type toWireCommandAuditSearchPrevious(
    final EIAJCommandAuditSearchPrevious cc)
  {
    return new EIAJ1CommandAuditSearchPrevious();
  }

  private static ProtocolAmberjackv1Type toWireCommandAuditSearchBegin(
    final EIAJCommandAuditSearchBegin cc)
  {
    return new EIAJ1CommandAuditSearchBegin(
      toWireAuditSearchParameters(cc.parameters())
    );
  }

  private static EIAJ1AuditSearchParameters toWireAuditSearchParameters(
    final EIAuditSearchParameters parameters)
  {
    return new EIAJ1AuditSearchParameters(
      toWireTimeRange(parameters.timeRange()),
      toWireOptionalString(parameters.owner()),
      toWireOptionalString(parameters.message()),
      toWireOptionalString(parameters.type()),
      unsigned16(parameters.limit())
    );
  }

  private static CBOptionType<CBString> toWireOptionalString(
    final Optional<String> s)
  {
    return CBOptionType.fromOptional(s.map(CBCore::string));
  }

  private static ProtocolAmberjackv1Type toWireCommandGroupCreate(
    final EIAJCommandGroupCreate cc)
  {
    return new EIAJ1CommandGroupCreate(
      string(cc.name().value())
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
      fromWirePermissionSet(u.fieldPermissions())
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
    if (p instanceof EIAJ1Permission.AuditRead) {
      return EIPermission.AUDIT_READ;
    }
    if (p instanceof EIAJ1Permission.GroupCreate) {
      return EIPermission.GROUP_CREATE;
    }
    if (p instanceof EIAJ1Permission.GroupModify) {
      return EIPermission.GROUP_MODIFY;
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

  private static EIAJMessageType fromWireCommandGroupCreate(
    final EIAJ1CommandGroupCreate c)
  {
    return new EIAJCommandGroupCreate(
      new EIGroupName(c.fieldName().value())
    );
  }

  private static EIAJMessageType fromWireResponseGroupCreate(
    final EIAJ1ResponseGroupCreate c)
  {
    return new EIAJResponseGroupCreate(
      fromWireUUID(c.fieldRequestId())
    );
  }

  private static EIAJMessageType fromWireCommandAuditSearchPrevious(
    final EIAJ1CommandAuditSearchPrevious c)
  {
    return new EIAJCommandAuditSearchPrevious();
  }

  private static EIAJMessageType fromWireCommandAuditSearchNext(
    final EIAJ1CommandAuditSearchNext c)
  {
    return new EIAJCommandAuditSearchNext();
  }

  private static EIAJMessageType fromWireCommandAuditSearchBegin(
    final EIAJ1CommandAuditSearchBegin c)
  {
    return new EIAJCommandAuditSearchBegin(
      fromWireAuditSearchParameters(c.fieldParameters())
    );
  }

  private static EIAuditSearchParameters fromWireAuditSearchParameters(
    final EIAJ1AuditSearchParameters fieldParameters)
  {
    return new EIAuditSearchParameters(
      fromWireTimeRange(fieldParameters.fieldTimeRange()),
      fromWireOptionalString(fieldParameters.fieldOwner()),
      fromWireOptionalString(fieldParameters.fieldType()),
      fromWireOptionalString(fieldParameters.fieldMessage()),
      Integer.toUnsignedLong(fieldParameters.fieldLimit().value())
    );
  }

  private static Optional<String> fromWireOptionalString(
    final CBOptionType<CBString> f)
  {
    return f.asOptional().map(CBString::value);
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
      if (message instanceof EIAJ1CommandGroupCreate c) {
        return fromWireCommandGroupCreate(c);
      }
      if (message instanceof EIAJ1CommandAuditSearchBegin c) {
        return fromWireCommandAuditSearchBegin(c);
      }
      if (message instanceof EIAJ1CommandAuditSearchNext c) {
        return fromWireCommandAuditSearchNext(c);
      }
      if (message instanceof EIAJ1CommandAuditSearchPrevious c) {
        return fromWireCommandAuditSearchPrevious(c);
      }
      if (message instanceof EIAJ1ResponseLogin c) {
        return fromWireResponseLogin(c);
      }
      if (message instanceof EIAJ1ResponseError c) {
        return fromWireResponseError(c);
      }
      if (message instanceof EIAJ1ResponseGroupCreate c) {
        return fromWireResponseGroupCreate(c);
      }
      if (message instanceof EIAJ1ResponseAuditSearch c) {
        return fromWireResponseAuditSearch(c);
      }
    } catch (final Exception e) {
      throw new EIProtocolException(PROTOCOL_ERROR, e.getMessage(), e);
    }

    throw new EIProtocolException(
      PROTOCOL_ERROR,
      "Unrecognized message: %s".formatted(message)
    );
  }

  private static EIAJMessageType fromWireResponseAuditSearch(
    final EIAJ1ResponseAuditSearch c)
  {
    return new EIAJResponseAuditSearch(
      fromWireUUID(c.fieldRequestId()),
      fromWirePage(c.fieldPage(), EIAJCB1ValidationGeneral::fromWireAuditEvent)
    );
  }
}
