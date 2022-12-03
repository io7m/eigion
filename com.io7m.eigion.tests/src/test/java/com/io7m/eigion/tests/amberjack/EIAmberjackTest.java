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

package com.io7m.eigion.tests.amberjack;

import com.io7m.eigion.amberjack.EIAJClients;
import com.io7m.eigion.amberjack.api.EIAJClientException;
import com.io7m.eigion.amberjack.api.EIAJClientType;
import com.io7m.eigion.error_codes.EIStandardErrorCodes;
import com.io7m.eigion.model.EIAuditSearchParameters;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIGroupSearchByNameParameters;
import com.io7m.eigion.model.EIPermission;
import com.io7m.eigion.model.EIPermissionSet;
import com.io7m.eigion.model.EITimeRange;
import com.io7m.eigion.server.api.EIServerConfiguratorType;
import com.io7m.eigion.server.api.EIServerType;
import com.io7m.eigion.server.database.api.EISDatabaseAuditQueriesType;
import com.io7m.eigion.tests.extensions.EIIdStoreExtension;
import com.io7m.eigion.tests.extensions.EIIdStoreType;
import com.io7m.eigion.tests.extensions.EIServerExtension;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.providers.TypeUsage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.OPERATION_NOT_PERMITTED;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.SECURITY_POLICY_DENIED;
import static com.io7m.eigion.model.EIPermission.AMBERJACK_ACCESS;
import static com.io7m.eigion.model.EIPermission.AUDIT_READ;
import static com.io7m.eigion.model.EIPermission.GROUP_CREATE;
import static com.io7m.eigion.model.EIPermission.GROUP_READ;
import static com.io7m.eigion.server.database.api.EISDatabaseRole.EIGION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers(disabledWithoutDocker = true)
@ExtendWith({EIIdStoreExtension.class, EIServerExtension.class})
public final class EIAmberjackTest
{
  private EIAJClients clients;
  private EIAJClientType client;

  @BeforeEach
  public void setup()
  {
    this.clients = new EIAJClients();
    this.client = this.clients.create(Locale.ROOT);
  }

  @AfterEach
  public void tearDown()
    throws IOException
  {
    this.client.close();
  }

  /**
   * It's not possible to log in if the user does not exist.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginNoSuchUser()
    throws Exception
  {
    final var ex =
      assertThrows(EIAJClientException.class, () -> {
        this.client.login(
          "nonexistent",
          "12345678",
          EIServerExtension.AMBERJACK_URI
        );
      });

    assertEquals(AUTHENTICATION_ERROR, ex.errorCode());
  }

  /**
   * It's not possible to log in if the user has no Amberjack permissions.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginUserNotPermitted0(
    final EIIdStoreType idstore)
    throws Exception
  {
    idstore.createUser("noone", "12345678");

    final var ex =
      assertThrows(EIAJClientException.class, () -> {
        this.client.login(
          "noone",
          "12345678",
          EIServerExtension.AMBERJACK_URI
        );
      });

    assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
  }

  /**
   * It's not possible to log in if the user has no Amberjack permissions.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginUserNotPermitted1(
    final EIIdStoreType idstore,
    final EIServerConfiguratorType configurator)
    throws Exception
  {
    final var userId =
      idstore.createUser("noone", "12345678");

    configurator.userSetPermissions(userId, EIPermissionSet.empty());

    final var ex =
      assertThrows(EIAJClientException.class, () -> {
        this.client.login(
          "noone",
          "12345678",
          EIServerExtension.AMBERJACK_URI
        );
      });

    assertEquals(OPERATION_NOT_PERMITTED, ex.errorCode());
  }

  /**
   * Logging in works if the user has Amberjack permissions.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginUserOK(
    final EIIdStoreType idstore,
    final EIServerConfiguratorType configurator)
    throws Exception
  {
    this.setupStandardUserAndLogIn(idstore, configurator, AMBERJACK_ACCESS);
  }

  /**
   * Creating groups requires permissions.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreateDisallowed(
    final EIIdStoreType idstore,
    final EIServerConfiguratorType configurator)
    throws Exception
  {
    this.setupStandardUserAndLogIn(idstore, configurator, AMBERJACK_ACCESS);

    final var ex =
      assertThrows(EIAJClientException.class, () -> {
        this.client.groupCreate(new EIGroupName("com.io7m.example"));
      });

    assertEquals(SECURITY_POLICY_DENIED, ex.errorCode());
  }

  /**
   * Creating groups works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreateOK(
    final EIIdStoreType idstore,
    final EIServerType server,
    final EIServerConfiguratorType configurator)
    throws Exception
  {
    this.setupStandardUserAndLogIn(
      idstore, configurator, AMBERJACK_ACCESS, GROUP_CREATE, GROUP_READ);

    this.client.groupCreate(new EIGroupName("com.io7m.example0"));
    this.client.groupCreate(new EIGroupName("com.io7m.example1"));
    this.client.groupCreate(new EIGroupName("com.io7m.example2"));

    final var search =
      this.client.groupSearchByName(
        new EIGroupSearchByNameParameters(Optional.empty(), 2L)
      );

    {
      final var p = search.current();
      assertEquals(1, p.pageIndex());
      assertEquals(2, p.pageCount());
      final var i = p.items();
      assertEquals("com.io7m.example0", i.get(0).value());
      assertEquals("com.io7m.example1", i.get(1).value());
      assertEquals(2, i.size());
    }

    {
      final var p = search.next();
      assertEquals(2, p.pageIndex());
      assertEquals(2, p.pageCount());
      final var i = p.items();
      assertEquals("com.io7m.example2", i.get(0).value());
      assertEquals(1, i.size());
    }

    {
      final var p = search.previous();
      assertEquals(1, p.pageIndex());
      assertEquals(2, p.pageCount());
      final var i = p.items();
      assertEquals("com.io7m.example0", i.get(0).value());
      assertEquals("com.io7m.example1", i.get(1).value());
      assertEquals(2, i.size());
    }

    this.checkAuditLog(
      server,
      check("USER_LOGGED_IN", null),
      check("GROUP_CREATED", "com.io7m.example0"),
      check("GROUP_CREATED", "com.io7m.example1"),
      check("GROUP_CREATED", "com.io7m.example2")
    );
  }

  /**
   * Searching the audit log works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAuditLogSearch(
    final EIIdStoreType idstore,
    final EIServerType server,
    final EIServerConfiguratorType configurator)
    throws Exception
  {
    final var userId =
      this.setupStandardUserAndLogIn(
        idstore, configurator, AMBERJACK_ACCESS, AUDIT_READ);

    final var time =
      OffsetDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"));

    final var database = server.database();
    try (var c = database.openConnection(EIGION)) {
      try (var t = c.openTransaction()) {
        final var q =
          t.queries(EISDatabaseAuditQueriesType.class);
        for (int index = 0; index < 100; ++index) {
          q.auditPut(
            userId,
            time,
            "AUDIT_EVENT_%03d".formatted(Integer.valueOf(index)),
            "AUDIT_MESSAGE_%03d".formatted(Integer.valueOf(index))
          );
        }
        t.commit();
      }
    }

    {
      final var search =
        this.client.auditSearch(
          new EIAuditSearchParameters(
            EITimeRange.largest(),
            Optional.empty(),
            Optional.of("AUDIT_EVENT_"),
            Optional.empty(),
            30L));

      {
        final var p = search.current();
        assertEquals(1, p.pageIndex());
        assertEquals(4, p.pageCount());

        final var items = p.items();
        for (int index = 0; index < 30; ++index) {
          final var event = items.get(index);
          final var number = index;
          assertEquals("AUDIT_EVENT_%03d".formatted(number), event.type());
          assertEquals("AUDIT_MESSAGE_%03d".formatted(number), event.message());
        }
        assertEquals(30, items.size());
      }

      {
        final var p = search.next();
        assertEquals(2, p.pageIndex());
        assertEquals(4, p.pageCount());

        final var items = p.items();
        for (int index = 0; index < 30; ++index) {
          final var event = items.get(index);
          final var number = 30 + index;
          assertEquals("AUDIT_EVENT_%03d".formatted(number), event.type());
          assertEquals("AUDIT_MESSAGE_%03d".formatted(number), event.message());
        }
        assertEquals(30, items.size());
      }

      {
        final var p = search.next();
        assertEquals(3, p.pageIndex());
        assertEquals(4, p.pageCount());

        final var items = p.items();
        for (int index = 0; index < 30; ++index) {
          final var event = items.get(index);
          final var number = 60 + index;
          assertEquals("AUDIT_EVENT_%03d".formatted(number), event.type());
          assertEquals("AUDIT_MESSAGE_%03d".formatted(number), event.message());
        }
        assertEquals(30, items.size());
      }

      {
        final var p = search.next();
        assertEquals(4, p.pageIndex());
        assertEquals(4, p.pageCount());

        final var items = p.items();
        for (int index = 0; index < 10; ++index) {
          final var event = items.get(index);
          final var number = 90 + index;
          assertEquals("AUDIT_EVENT_%03d".formatted(number), event.type());
          assertEquals("AUDIT_MESSAGE_%03d".formatted(number), event.message());
        }
        assertEquals(10, items.size());
      }

      {
        final var p = search.previous();
        assertEquals(3, p.pageIndex());
        assertEquals(4, p.pageCount());

        final var items = p.items();
        for (int index = 0; index < 30; ++index) {
          final var event = items.get(index);
          final var number = 60 + index;
          assertEquals("AUDIT_EVENT_%03d".formatted(number), event.type());
          assertEquals("AUDIT_MESSAGE_%03d".formatted(number), event.message());
        }
        assertEquals(30, items.size());
      }

      {
        final var p = search.previous();
        assertEquals(2, p.pageIndex());
        assertEquals(4, p.pageCount());

        final var items = p.items();
        for (int index = 0; index < 30; ++index) {
          final var event = items.get(index);
          final var number = 30 + index;
          assertEquals("AUDIT_EVENT_%03d".formatted(number), event.type());
          assertEquals("AUDIT_MESSAGE_%03d".formatted(number), event.message());
        }
        assertEquals(30, items.size());
      }

      {
        final var p = search.previous();
        assertEquals(1, p.pageIndex());
        assertEquals(4, p.pageCount());

        final var items = p.items();
        for (int index = 0; index < 30; ++index) {
          final var event = items.get(index);
          final var number = index;
          assertEquals("AUDIT_EVENT_%03d".formatted(number), event.type());
          assertEquals("AUDIT_MESSAGE_%03d".formatted(number), event.message());
        }
        assertEquals(30, items.size());
      }
    }
  }

  private void checkAuditLog(
    final EIServerType server,
    final AuditCheck... auditCheck)
    throws Exception
  {
    final var database = server.database();
    try (var c = database.openConnection(EIGION)) {
      try (var t = c.openTransaction()) {
        final var q =
          t.queries(EISDatabaseAuditQueriesType.class);

        final var search =
          q.auditEventsSearch(new EIAuditSearchParameters(
            EITimeRange.largest(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            1000L
          ));

        final var events =
          search.pageCurrent(q).items();

        for (int index = 0; index < auditCheck.length; ++index) {
          final var check = auditCheck[index];
          final var event = events.get(index);
          assertEquals(
            check.type,
            event.type(),
            "[%d] type %s == event type %s"
              .formatted(Integer.valueOf(index), check.type, event.type())
          );
          if (check.message != null) {
            assertEquals(
              check.message,
              event.message(),
              "[%d] message %s == event message %s"
                .formatted(
                  Integer.valueOf(index),
                  check.message,
                  event.message())
            );
          }
        }
      }
    }
  }

  private static AuditCheck check(
    final String type,
    final String message)
  {
    return new AuditCheck(type, message);
  }

  private record AuditCheck(
    String type,
    String message)
  {

  }

  private UUID setupStandardUserAndLogIn(
    final EIIdStoreType idstore,
    final EIServerConfiguratorType configurator,
    final EIPermission... permissions)
    throws Exception
  {
    final var userId =
      idstore.createUser("noone", "12345678");

    configurator.userSetPermissions(userId, EIPermissionSet.of(permissions));

    this.client.login(
      "noone",
      "12345678",
      EIServerExtension.AMBERJACK_URI
    );
    return userId;
  }

  /**
   * Every method that requires a login throws an exception if the user is not
   * logged in.
   *
   * @return The tests
   */

  @TestFactory
  public Stream<DynamicTest> testDisconnected(
    final EIServerType server)
  {
    return disconnectionRelevantMethodsOf(EIAJClientType.class)
      .map(this::disconnectedOf);
  }

  private DynamicTest disconnectedOf(
    final Method method)
  {
    return DynamicTest.dynamicTest(
      "testDisconnected_%s".formatted(method.getName()),
      () -> {
        final var parameterTypes =
          method.getGenericParameterTypes();
        final var parameters =
          new Object[parameterTypes.length];

        for (var index = 0; index < parameterTypes.length; ++index) {
          final var pType = parameterTypes[index];
          if (pType instanceof ParameterizedType param) {
            final List<TypeUsage> typeArgs =
              Arrays.stream(param.getActualTypeArguments())
                .map(TypeUsage::forType)
                .toList();

            final var typeArgsArray = new TypeUsage[typeArgs.size()];
            typeArgs.toArray(typeArgsArray);

            final var mainType =
              TypeUsage.of((Class<?>) param.getRawType(), typeArgsArray);

            parameters[index] = Arbitraries.defaultFor(mainType).sample();
          } else if (pType instanceof Class<?> clazz) {
            parameters[index] = Arbitraries.defaultFor(clazz).sample();
          }
        }

        try {
          method.invoke(this.client, parameters);
        } catch (final IllegalAccessException | IllegalArgumentException e) {
          throw new RuntimeException(e);
        } catch (final InvocationTargetException e) {
          if (e.getCause() instanceof EIAJClientException ex) {
            if (Objects.equals(
              ex.errorCode(),
              EIStandardErrorCodes.NOT_LOGGED_IN)) {
              return;
            }
          }
          throw e;
        }
      });
  }

  private static Stream<Method> disconnectionRelevantMethodsOf(
    final Class<? extends EIAJClientType> clazz)
  {
    return Stream.of(clazz.getMethods())
      .filter(EIAmberjackTest::isDisconnectionRelevantMethod);
  }

  private static boolean isDisconnectionRelevantMethod(
    final Method m)
  {
    return switch (m.getName()) {
      case "toString",
        "equals",
        "hashCode",
        "getClass",
        "close",
        "login",
        "notify",
        "wait",
        "notifyAll" -> false;
      default -> true;
    };
  }
}
