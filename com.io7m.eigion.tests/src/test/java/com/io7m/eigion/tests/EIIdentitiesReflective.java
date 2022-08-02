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


package com.io7m.eigion.tests;

import com.io7m.eigion.protocol.admin_api.v1.EISA1Admin;
import com.io7m.eigion.protocol.admin_api.v1.EISA1AdminPermission;
import com.io7m.eigion.protocol.admin_api.v1.EISA1AdminSummary;
import com.io7m.eigion.protocol.admin_api.v1.EISA1AuditEvent;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAdminCreate;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAdminGet;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAdminGetByEmail;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAdminGetByName;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAdminSearch;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAuditGet;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandGroupInvites;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandLogin;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandServicesList;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandType;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserCreate;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserGet;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserGetByEmail;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserGetByName;
import com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserSearch;
import com.io7m.eigion.protocol.admin_api.v1.EISA1GroupInvite;
import com.io7m.eigion.protocol.admin_api.v1.EISA1GroupInviteStatus;
import com.io7m.eigion.protocol.admin_api.v1.EISA1GroupRole;
import com.io7m.eigion.protocol.admin_api.v1.EISA1MessageType;
import com.io7m.eigion.protocol.admin_api.v1.EISA1Messages;
import com.io7m.eigion.protocol.admin_api.v1.EISA1Password;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ProductIdTypeResolver;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseAdminCreate;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseAdminGet;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseAdminList;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseAuditGet;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseError;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseGroupInvites;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseLogin;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseServiceList;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseType;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseUserCreate;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseUserGet;
import com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseUserList;
import com.io7m.eigion.protocol.admin_api.v1.EISA1Service;
import com.io7m.eigion.protocol.admin_api.v1.EISA1SubsetMatch;
import com.io7m.eigion.protocol.admin_api.v1.EISA1Transaction;
import com.io7m.eigion.protocol.admin_api.v1.EISA1TransactionResponse;
import com.io7m.eigion.protocol.admin_api.v1.EISA1User;
import com.io7m.eigion.protocol.admin_api.v1.EISA1UserBan;
import com.io7m.eigion.protocol.admin_api.v1.EISA1UserSummary;
import com.io7m.eigion.protocol.api.EIProtocolFromModel;
import com.io7m.eigion.protocol.api.EIProtocolToModel;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupCreateBegin;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupCreateCancel;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupCreateReady;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroupCreateRequests;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandGroups;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandLogin;
import com.io7m.eigion.protocol.public_api.v1.EISP1CommandType;
import com.io7m.eigion.protocol.public_api.v1.EISP1GroupCreationRequest;
import com.io7m.eigion.protocol.public_api.v1.EISP1GroupInvite;
import com.io7m.eigion.protocol.public_api.v1.EISP1GroupInviteStatus;
import com.io7m.eigion.protocol.public_api.v1.EISP1GroupRole;
import com.io7m.eigion.protocol.public_api.v1.EISP1GroupRoles;
import com.io7m.eigion.protocol.public_api.v1.EISP1Hash;
import com.io7m.eigion.protocol.public_api.v1.EISP1MessageType;
import com.io7m.eigion.protocol.public_api.v1.EISP1Messages;
import com.io7m.eigion.protocol.public_api.v1.EISP1ProductIdTypeResolver;
import com.io7m.eigion.protocol.public_api.v1.EISP1ProductSummary;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseError;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroupCreateBegin;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroupCreateCancel;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroupCreateReady;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroupCreateRequests;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseGroups;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseImageCreated;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseImageGet;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseLogin;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseProductList;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseType;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.CannotFindArbitraryException;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public final class EIIdentitiesReflective
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIIdentitiesReflective.class);

  private static final List<Class<?>> CLASSES = List.of(
    EISA1Admin.class,
    EISA1AdminPermission.class,
    EISA1AdminSummary.class,
    EISA1AuditEvent.class,
    EISA1CommandAdminCreate.class,
    EISA1CommandAdminGet.class,
    EISA1CommandAdminGetByEmail.class,
    EISA1CommandAdminGetByName.class,
    EISA1CommandAdminSearch.class,
    EISA1CommandAuditGet.class,
    EISA1CommandGroupInvites.class,
    EISA1CommandLogin.class,
    EISA1CommandServicesList.class,
    EISA1CommandType.class,
    EISA1CommandUserCreate.class,
    EISA1CommandUserGet.class,
    EISA1CommandUserGetByEmail.class,
    EISA1CommandUserGetByName.class,
    EISA1CommandUserSearch.class,
    EISA1GroupInvite.class,
    EISA1GroupInviteStatus.class,
    EISA1GroupRole.class,
    EISA1MessageType.class,
    EISA1Messages.class,
    EISA1Password.class,
    EISA1ProductIdTypeResolver.class,
    EISA1ResponseAdminCreate.class,
    EISA1ResponseAdminGet.class,
    EISA1ResponseAdminList.class,
    EISA1ResponseAuditGet.class,
    EISA1ResponseError.class,
    EISA1ResponseGroupInvites.class,
    EISA1ResponseLogin.class,
    EISA1ResponseServiceList.class,
    EISA1ResponseType.class,
    EISA1ResponseUserCreate.class,
    EISA1ResponseUserGet.class,
    EISA1ResponseUserList.class,
    EISA1Service.class,
    EISA1SubsetMatch.class,
    EISA1Transaction.class,
    EISA1TransactionResponse.class,
    EISA1User.class,
    EISA1UserBan.class,
    EISA1UserSummary.class,
    EISP1CommandGroupCreateBegin.class,
    EISP1CommandGroupCreateCancel.class,
    EISP1CommandGroupCreateReady.class,
    EISP1CommandGroupCreateRequests.class,
    EISP1CommandGroups.class,
    EISP1CommandLogin.class,
    EISP1CommandType.class,
    EISP1GroupCreationRequest.class,
    EISP1GroupInvite.class,
    EISP1GroupInviteStatus.class,
    EISP1GroupRole.class,
    EISP1GroupRoles.class,
    EISP1Hash.class,
    EISP1MessageType.class,
    EISP1Messages.class,
    EISP1ProductIdTypeResolver.class,
    EISP1ProductSummary.class,
    EISP1ResponseError.class,
    EISP1ResponseGroupCreateBegin.class,
    EISP1ResponseGroupCreateCancel.class,
    EISP1ResponseGroupCreateReady.class,
    EISP1ResponseGroupCreateRequests.class,
    EISP1ResponseGroups.class,
    EISP1ResponseImageCreated.class,
    EISP1ResponseImageGet.class,
    EISP1ResponseLogin.class,
    EISP1ResponseProductList.class,
    EISP1ResponseType.class
  );

  @TestFactory
  public Stream<DynamicTest> identities()
  {
    return CLASSES.stream()
      .map(EIIdentitiesReflective::identityTestFor);
  }

  private static DynamicTest identityTestFor(
    final Class<?> c)
  {
    return DynamicTest.dynamicTest("test_" + c.getSimpleName(), () -> {
      try {

        /*
         * Find the "to model" and "from model" methods.
         */

        final var toModelMethod =
          findToModelMethod(c);
        final var toVersionMethod =
          findFromModelMethod(c);

        /*
         * If only one method is defined, this is likely a mistake!
         */

        if ((toModelMethod == null) == (toVersionMethod != null)) {
          throw new IllegalStateException(
            "Protocol issue: toModelMethod %s, toVersionMethod %s"
              .formatted(toModelMethod, toVersionMethod)
          );
        }

        /*
         * If there are type parameters, instantiate them all to Object.
         */

        final Arbitrary<?> arb;
        final var typeParameters = c.getTypeParameters();
        if (typeParameters.length > 0) {
          final var ps = new Class[typeParameters.length];
          for (int index = 0; index < typeParameters.length; ++index) {
            ps[index] = Object.class;
          }
          arb = Arbitraries.defaultFor(c, ps);
        } else {
          arb = Arbitraries.defaultFor(c);
        }

        /*
         * Try 1000 values.
         */

        LOG.debug("checking 1000 cases for {}", c.getSimpleName());
        for (int index = 0; index < 1000; ++index) {
          final var v0 =
            arb.sample();
          final var vm =
            toModelMethod.invoke(v0);
          final var v1 =
            toVersionMethod.invoke(null, vm);

          assertNotNull(v0);
          assertNotNull(vm);
          assertNotNull(v1);
          assertNotSame(v0, vm);
          assertNotSame(vm, v1);
          assertEquals(v0, v1);
        }

      } catch (final CannotFindArbitraryException e) {
        LOG.debug("No arbitrary for {}", c);
      } catch (final NotApplicable e) {
        LOG.debug("Not applicable {}: {}", c, e.getMessage());
      }
    });
  }

  private static Method findFromModelMethod(
    final Class<?> c)
  {
    return Arrays.stream(c.getMethods())
      .filter(m -> m.isAnnotationPresent(EIProtocolFromModel.class))
      .findFirst()
      .orElseThrow(() -> new NotApplicable("No EIProtocolFromModel method"));
  }

  private static Method findToModelMethod(
    final Class<?> c)
  {
    return Arrays.stream(c.getMethods())
      .filter(m -> m.isAnnotationPresent(EIProtocolToModel.class))
      .findFirst()
      .orElseThrow(() -> new NotApplicable("No EIProtocolToModel method"));
  }

  private static final class NotApplicable extends RuntimeException
  {
    private NotApplicable(
      final String message)
    {
      super(Objects.requireNonNull(message, "message"));
    }
  }
}
