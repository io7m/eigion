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

import com.io7m.eigion.protocol.api.EIProtocolFromModel;
import com.io7m.eigion.protocol.api.EIProtocolToModel;
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
    com.io7m.eigion.protocol.public_api.v1.EISP1CommandLogin.class,
    com.io7m.eigion.protocol.public_api.v1.EISP1CommandType.class,
    com.io7m.eigion.protocol.public_api.v1.EISP1Hash.class,
    com.io7m.eigion.protocol.public_api.v1.EISP1Messages.class,
    com.io7m.eigion.protocol.public_api.v1.EISP1MessageType.class,
    com.io7m.eigion.protocol.public_api.v1.EISP1ProductIdTypeResolver.class,
    com.io7m.eigion.protocol.public_api.v1.EISP1ProductSummary.class,
    com.io7m.eigion.protocol.public_api.v1.EISP1ResponseError.class,
    com.io7m.eigion.protocol.public_api.v1.EISP1ResponseImageCreated.class,
    com.io7m.eigion.protocol.public_api.v1.EISP1ResponseImageGet.class,
    com.io7m.eigion.protocol.public_api.v1.EISP1ResponseProductList.class,
    com.io7m.eigion.protocol.public_api.v1.EISP1ResponseType.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1Admin.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1AdminPermission.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1AdminSummary.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1AuditEvent.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAdminCreate.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAdminGet.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAdminGetByEmail.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAdminGetByName.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAdminSearch.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1CommandAuditGet.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1CommandLogin.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1CommandServicesList.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1CommandType.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserCreate.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserGet.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserGetByEmail.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserGetByName.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1CommandUserSearch.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1GroupRole.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1Messages.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1MessageType.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1Password.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1ProductIdTypeResolver.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseAdminCreate.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseAdminGet.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseAdminList.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseAuditGet.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseError.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseLogin.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseServiceList.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseType.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseUserCreate.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseUserGet.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1ResponseUserList.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1Service.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1SubsetMatch.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1Transaction.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1TransactionResponse.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1User.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1UserBan.class,
    com.io7m.eigion.protocol.admin_api.v1.EISA1UserSummary.class
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

  private static final class NotApplicable extends RuntimeException {
    private NotApplicable(
      final String message)
    {
      super(Objects.requireNonNull(message, "message"));
    }
  }
}
