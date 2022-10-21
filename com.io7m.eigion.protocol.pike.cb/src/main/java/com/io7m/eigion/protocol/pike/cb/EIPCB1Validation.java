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
import com.io7m.cedarbridge.runtime.api.CBOptionType;
import com.io7m.cedarbridge.runtime.api.CBString;
import com.io7m.eigion.error_codes.EIErrorCode;
import com.io7m.eigion.model.EIGroupCreationRequest;
import com.io7m.eigion.model.EIGroupCreationRequestStatusType;
import com.io7m.eigion.model.EIGroupMembership;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIGroupRole;
import com.io7m.eigion.model.EIGroupRoleSet;
import com.io7m.eigion.model.EIPermission;
import com.io7m.eigion.model.EIPermissionSet;
import com.io7m.eigion.model.EIToken;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.model.EIValidityException;
import com.io7m.eigion.protocol.api.EIProtocolException;
import com.io7m.eigion.protocol.api.EIProtocolMessageValidatorType;
import com.io7m.eigion.protocol.pike.EIPCommandGroupCreateBegin;
import com.io7m.eigion.protocol.pike.EIPCommandGroupCreateCancel;
import com.io7m.eigion.protocol.pike.EIPCommandGroupCreateReady;
import com.io7m.eigion.protocol.pike.EIPCommandGroupCreateRequestsBegin;
import com.io7m.eigion.protocol.pike.EIPCommandGroupCreateRequestsNext;
import com.io7m.eigion.protocol.pike.EIPCommandGroupCreateRequestsPrevious;
import com.io7m.eigion.protocol.pike.EIPCommandGroupsBegin;
import com.io7m.eigion.protocol.pike.EIPCommandGroupsNext;
import com.io7m.eigion.protocol.pike.EIPCommandGroupsPrevious;
import com.io7m.eigion.protocol.pike.EIPCommandLogin;
import com.io7m.eigion.protocol.pike.EIPCommandType;
import com.io7m.eigion.protocol.pike.EIPMessageType;
import com.io7m.eigion.protocol.pike.EIPResponseError;
import com.io7m.eigion.protocol.pike.EIPResponseGroupCreateBegin;
import com.io7m.eigion.protocol.pike.EIPResponseGroupCreateCancel;
import com.io7m.eigion.protocol.pike.EIPResponseGroupCreateReady;
import com.io7m.eigion.protocol.pike.EIPResponseGroupCreateRequests;
import com.io7m.eigion.protocol.pike.EIPResponseGroups;
import com.io7m.eigion.protocol.pike.EIPResponseLogin;
import com.io7m.eigion.protocol.pike.EIPResponseType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.io7m.cedarbridge.runtime.api.CBCore.string;
import static com.io7m.cedarbridge.runtime.api.CBCore.unsigned16;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.eigion.protocol.pike.cb.internal.EIPCB1ValidationGeneral.fromWirePage;
import static com.io7m.eigion.protocol.pike.cb.internal.EIPCB1ValidationGeneral.fromWireTimestamp;
import static com.io7m.eigion.protocol.pike.cb.internal.EIPCB1ValidationGeneral.fromWireUUID;
import static com.io7m.eigion.protocol.pike.cb.internal.EIPCB1ValidationGeneral.toWirePage;
import static com.io7m.eigion.protocol.pike.cb.internal.EIPCB1ValidationGeneral.toWireTimestamp;
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
    if (r instanceof EIPResponseGroupCreateBegin rr) {
      return toWireResponseGroupCreateBegin(rr);
    }
    if (r instanceof EIPResponseGroupCreateCancel rr) {
      return toWireResponseGroupCreateCancel(rr);
    }
    if (r instanceof EIPResponseGroupCreateReady rr) {
      return toWireResponseGroupCreateReady(rr);
    }
    if (r instanceof EIPResponseGroups rr) {
      return toWireResponseGroups(rr);
    }
    if (r instanceof EIPResponseGroupCreateRequests rr) {
      return toWireResponseGroupCreateRequests(rr);
    }

    throw new EIProtocolException(
      PROTOCOL_ERROR,
      "Unrecognized message: %s".formatted(r)
    );
  }

  private static ProtocolPikev1Type toWireResponseGroupCreateRequests(
    final EIPResponseGroupCreateRequests rr)
  {
    return new EIP1ResponseGroupCreateRequests(
      toWireUUID(rr.requestId()),
      toWirePage(rr.requests(), EIPCB1Validation::toWireGroupCreationRequest)
    );
  }

  private static EIP1GroupCreationRequest toWireGroupCreationRequest(
    final EIGroupCreationRequest r)
  {
    return new EIP1GroupCreationRequest(
      string(r.groupName().value()),
      toWireUUID(r.userFounder()),
      string(r.token().value()),
      toWireGroupCreationStatus(r.status())
    );
  }

  private static EIP1GroupCreationRequestStatus toWireGroupCreationStatus(
    final EIGroupCreationRequestStatusType status)
  {
    if (status instanceof EIGroupCreationRequestStatusType.InProgress s) {
      return new EIP1GroupCreationRequestStatus.InProgress(
        toWireTimestamp(s.timeStarted())
      );
    }
    if (status instanceof EIGroupCreationRequestStatusType.Succeeded s) {
      return new EIP1GroupCreationRequestStatus.Succeeded(
        toWireTimestamp(s.timeStarted()),
        toWireTimestamp(s.timeCompletedValue())
      );
    }
    if (status instanceof EIGroupCreationRequestStatusType.Failed s) {
      return new EIP1GroupCreationRequestStatus.Failed(
        toWireTimestamp(s.timeStarted()),
        toWireTimestamp(s.timeCompletedValue()),
        string(s.message())
      );
    }
    if (status instanceof EIGroupCreationRequestStatusType.Cancelled s) {
      return new EIP1GroupCreationRequestStatus.Cancelled(
        toWireTimestamp(s.timeStarted()),
        toWireTimestamp(s.timeCompletedValue())
      );
    }

    throw new EIValidityException(
      "Unrecognized status message: %s".formatted(status)
    );
  }

  private static ProtocolPikev1Type toWireResponseGroups(
    final EIPResponseGroups rr)
  {
    return new EIP1ResponseGroups(
      toWireUUID(rr.requestId()),
      toWirePage(rr.groups(), EIPCB1Validation::toWireGroupMembership)
    );
  }

  private static EIP1GroupRoles toWireGroupMembership(
    final EIGroupMembership g)
  {
    return new EIP1GroupRoles(
      string(g.group().value()),
      toWireGroupRoleSet(g.roles().roles())
    );
  }

  private static ProtocolPikev1Type toWireResponseGroupCreateReady(
    final EIPResponseGroupCreateReady rr)
  {
    return new EIP1ResponseGroupCreateReady(toWireUUID(rr.requestId()));
  }

  private static ProtocolPikev1Type toWireResponseGroupCreateCancel(
    final EIPResponseGroupCreateCancel rr)
  {
    return new EIP1ResponseGroupCreateCancel(toWireUUID(rr.requestId()));
  }

  private static ProtocolPikev1Type toWireResponseGroupCreateBegin(
    final EIPResponseGroupCreateBegin rr)
  {
    return new EIP1ResponseGroupCreateBegin(
      toWireUUID(rr.requestId()),
      string(rr.groupName().value()),
      string(rr.token().value()),
      string(rr.location().toString())
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
    if (c instanceof EIPCommandGroupCreateBegin cc) {
      return toWireCommandGroupCreateBegin(cc);
    }
    if (c instanceof EIPCommandGroupCreateReady cc) {
      return toWireCommandGroupCreateReady(cc);
    }
    if (c instanceof EIPCommandGroupCreateCancel cc) {
      return toWireCommandGroupCreateCancel(cc);
    }
    if (c instanceof EIPCommandGroupsBegin cc) {
      return toWireCommandGroupsBegin(cc);
    }
    if (c instanceof EIPCommandGroupsNext cc) {
      return toWireCommandGroupsNext(cc);
    }
    if (c instanceof EIPCommandGroupsPrevious cc) {
      return toWireCommandGroupsPrevious(cc);
    }
    if (c instanceof EIPCommandGroupCreateRequestsBegin cc) {
      return toWireCommandGroupCreateRequestsBegin(cc);
    }
    if (c instanceof EIPCommandGroupCreateRequestsNext cc) {
      return toWireCommandGroupCreateRequestsNext(cc);
    }
    if (c instanceof EIPCommandGroupCreateRequestsPrevious cc) {
      return toWireCommandGroupCreateRequestsPrevious(cc);
    }

    throw new EIProtocolException(
      PROTOCOL_ERROR,
      "Unrecognized message: %s".formatted(c)
    );
  }

  private static ProtocolPikev1Type toWireCommandGroupCreateRequestsBegin(
    final EIPCommandGroupCreateRequestsBegin cc)
  {
    return new EIP1CommandGroupCreateRequestsBegin(unsigned16(cc.limit()));
  }

  private static ProtocolPikev1Type toWireCommandGroupCreateRequestsNext(
    final EIPCommandGroupCreateRequestsNext cc)
  {
    return new EIP1CommandGroupCreateRequestsNext();
  }

  private static ProtocolPikev1Type toWireCommandGroupCreateRequestsPrevious(
    final EIPCommandGroupCreateRequestsPrevious cc)
  {
    return new EIP1CommandGroupCreateRequestsPrevious();
  }

  private static ProtocolPikev1Type toWireCommandGroupsBegin(
    final EIPCommandGroupsBegin cc)
  {
    return new EIP1CommandGroupsBegin(unsigned16(cc.limit()));
  }

  private static ProtocolPikev1Type toWireCommandGroupsNext(
    final EIPCommandGroupsNext cc)
  {
    return new EIP1CommandGroupsNext();
  }

  private static ProtocolPikev1Type toWireCommandGroupsPrevious(
    final EIPCommandGroupsPrevious cc)
  {
    return new EIP1CommandGroupsPrevious();
  }

  private static ProtocolPikev1Type toWireCommandGroupCreateReady(
    final EIPCommandGroupCreateReady cc)
  {
    return new EIP1CommandGroupCreateReady(
      string(cc.token().value())
    );
  }

  private static ProtocolPikev1Type toWireCommandGroupCreateCancel(
    final EIPCommandGroupCreateCancel cc)
  {
    return new EIP1CommandGroupCreateCancel(
      string(cc.token().value())
    );
  }

  private static ProtocolPikev1Type toWireCommandGroupCreateBegin(
    final EIPCommandGroupCreateBegin cc)
  {
    return new EIP1CommandGroupCreateBegin(
      string(cc.groupName().value())
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
    if (r instanceof EIP1GroupRole.Member) {
      return EIGroupRole.MEMBER;
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
      if (message instanceof EIP1CommandGroupCreateBegin c) {
        return fromWireCommandGroupCreateBegin(c);
      }
      if (message instanceof EIP1CommandGroupCreateReady c) {
        return fromWireCommandGroupCreateReady(c);
      }
      if (message instanceof EIP1CommandGroupCreateCancel c) {
        return fromWireCommandGroupCreateCancel(c);
      }
      if (message instanceof EIP1CommandGroupsBegin c) {
        return fromWireCommandGroupsBegin(c);
      }
      if (message instanceof EIP1CommandGroupsNext c) {
        return fromWireCommandGroupsNext(c);
      }
      if (message instanceof EIP1CommandGroupsPrevious c) {
        return fromWireCommandGroupsPrevious(c);
      }
      if (message instanceof EIP1CommandGroupCreateRequestsBegin c) {
        return fromWireCommandGroupCreateRequestsBegin(c);
      }
      if (message instanceof EIP1CommandGroupCreateRequestsNext c) {
        return fromWireCommandGroupCreateRequestsNext(c);
      }
      if (message instanceof EIP1CommandGroupCreateRequestsPrevious c) {
        return fromWireCommandGroupCreateRequestsPrevious(c);
      }

      if (message instanceof EIP1ResponseLogin c) {
        return fromWireResponseLogin(c);
      }
      if (message instanceof EIP1ResponseError c) {
        return fromWireResponseError(c);
      }
      if (message instanceof EIP1ResponseGroupCreateBegin c) {
        return fromWireResponseGroupCreateBegin(c);
      }
      if (message instanceof EIP1ResponseGroupCreateReady c) {
        return fromWireResponseGroupCreateReady(c);
      }
      if (message instanceof EIP1ResponseGroupCreateCancel c) {
        return fromWireResponseGroupCreateCancel(c);
      }
      if (message instanceof EIP1ResponseGroups c) {
        return fromWireResponseGroups(c);
      }
      if (message instanceof EIP1ResponseGroupCreateRequests c) {
        return fromWireResponseGroupCreateRequests(c);
      }
    } catch (final Exception e) {
      throw new EIProtocolException(PROTOCOL_ERROR, e.getMessage(), e);
    }

    throw new EIProtocolException(
      PROTOCOL_ERROR,
      "Unrecognized message: %s".formatted(message)
    );
  }

  private static EIPMessageType fromWireCommandGroupCreateRequestsBegin(
    final EIP1CommandGroupCreateRequestsBegin c)
  {
    return new EIPCommandGroupCreateRequestsBegin(
      Integer.toUnsignedLong(c.fieldLimit().value())
    );
  }

  private static EIPMessageType fromWireCommandGroupCreateRequestsNext(
    final EIP1CommandGroupCreateRequestsNext c)
  {
    return new EIPCommandGroupCreateRequestsNext();
  }

  private static EIPMessageType fromWireCommandGroupCreateRequestsPrevious(
    final EIP1CommandGroupCreateRequestsPrevious c)
  {
    return new EIPCommandGroupCreateRequestsPrevious();
  }

  private static EIPMessageType fromWireCommandGroupsBegin(
    final EIP1CommandGroupsBegin c)
  {
    return new EIPCommandGroupsBegin(
      Integer.toUnsignedLong(c.fieldLimit().value())
    );
  }

  private static EIPMessageType fromWireCommandGroupsNext(
    final EIP1CommandGroupsNext c)
  {
    return new EIPCommandGroupsNext();
  }

  private static EIPMessageType fromWireCommandGroupsPrevious(
    final EIP1CommandGroupsPrevious c)
  {
    return new EIPCommandGroupsPrevious();
  }

  private static EIPMessageType fromWireResponseGroups(
    final EIP1ResponseGroups c)
  {
    return new EIPResponseGroups(
      fromWireUUID(c.fieldRequestId()),
      fromWirePage(c.fieldGroups(), EIPCB1Validation::fromWireGroupMembership)
    );
  }

  private static EIPMessageType fromWireResponseGroupCreateRequests(
    final EIP1ResponseGroupCreateRequests c)
  {
    return new EIPResponseGroupCreateRequests(
      fromWireUUID(c.fieldRequestId()),
      fromWirePage(c.fieldRequests(), EIPCB1Validation::fromWireGroupCreationRequest)
    );
  }

  private static EIGroupCreationRequest fromWireGroupCreationRequest(
    final EIP1GroupCreationRequest r)
  {
    return new EIGroupCreationRequest(
      new EIGroupName(r.fieldGroupName().value()),
      fromWireUUID(r.fieldUser()),
      new EIToken(r.fieldToken().value()),
      fromWireGroupCreationStatus(r.fieldStatus())
    );
  }

  private static EIGroupCreationRequestStatusType fromWireGroupCreationStatus(
    final EIP1GroupCreationRequestStatus status)
  {
    if (status instanceof EIP1GroupCreationRequestStatus.InProgress s) {
      return new EIGroupCreationRequestStatusType.InProgress(
        fromWireTimestamp(s.fieldTimeStarted())
      );
    }
    if (status instanceof EIP1GroupCreationRequestStatus.Cancelled s) {
      return new EIGroupCreationRequestStatusType.Cancelled(
        fromWireTimestamp(s.fieldTimeStarted()),
        fromWireTimestamp(s.fieldTimeCompleted())
      );
    }
    if (status instanceof EIP1GroupCreationRequestStatus.Succeeded s) {
      return new EIGroupCreationRequestStatusType.Succeeded(
        fromWireTimestamp(s.fieldTimeStarted()),
        fromWireTimestamp(s.fieldTimeCompleted())
      );
    }
    if (status instanceof EIP1GroupCreationRequestStatus.Failed s) {
      return new EIGroupCreationRequestStatusType.Failed(
        fromWireTimestamp(s.fieldTimeStarted()),
        fromWireTimestamp(s.fieldTimeCompleted()),
        s.fieldMessage().value()
      );
    }
    throw new EIValidityException(
      "Unrecognized status message: %s".formatted(status)
    );
  }

  private static EIGroupMembership fromWireGroupMembership(
    final EIP1GroupRoles g)
  {
    return new EIGroupMembership(
      new EIGroupName(g.fieldGroupName().value()),
      EIGroupRoleSet.of(fromWireGroupRoleSet(g.fieldRoles()))
    );
  }

  private static EIPMessageType fromWireCommandGroupCreateReady(
    final EIP1CommandGroupCreateReady c)
  {
    return new EIPCommandGroupCreateReady(
      fromWireToken(c.fieldToken())
    );
  }

  private static EIPMessageType fromWireCommandGroupCreateCancel(
    final EIP1CommandGroupCreateCancel c)
  {
    return new EIPCommandGroupCreateCancel(
      fromWireToken(c.fieldToken())
    );
  }

  private static EIToken fromWireToken(final CBString s)
  {
    return new EIToken(s.value());
  }

  private static EIPMessageType fromWireResponseGroupCreateCancel(
    final EIP1ResponseGroupCreateCancel c)
  {
    return new EIPResponseGroupCreateCancel(fromWireUUID(c.fieldRequestId()));
  }

  private static EIPMessageType fromWireResponseGroupCreateReady(
    final EIP1ResponseGroupCreateReady c)
  {
    return new EIPResponseGroupCreateReady(fromWireUUID(c.fieldRequestId()));
  }

  private static EIPMessageType fromWireResponseGroupCreateBegin(
    final EIP1ResponseGroupCreateBegin c)
    throws URISyntaxException
  {
    return new EIPResponseGroupCreateBegin(
      fromWireUUID(c.fieldRequestId()),
      new EIGroupName(c.fieldGroupName().value()),
      new EIToken(c.fieldToken().value()),
      new URI(c.fieldLocation().value())
    );
  }

  private static EIPMessageType fromWireCommandGroupCreateBegin(
    final EIP1CommandGroupCreateBegin c)
  {
    return new EIPCommandGroupCreateBegin(
      new EIGroupName(c.fieldGroupName().value())
    );
  }
}
