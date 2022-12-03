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

package com.io7m.eigion.tests.pike;

import com.io7m.eigion.amberjack.api.EIAJClientException;
import com.io7m.eigion.amberjack.api.EIAJClientType;
import com.io7m.eigion.error_codes.EIStandardErrorCodes;
import com.io7m.eigion.model.EIAuditSearchParameters;
import com.io7m.eigion.model.EIGroupCreationRequestStatusType;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIPermission;
import com.io7m.eigion.model.EIPermissionSet;
import com.io7m.eigion.model.EITimeRange;
import com.io7m.eigion.pike.EIPClients;
import com.io7m.eigion.pike.api.EIPClientException;
import com.io7m.eigion.pike.api.EIPClientType;
import com.io7m.eigion.server.api.EIServerConfiguratorType;
import com.io7m.eigion.server.api.EIServerType;
import com.io7m.eigion.server.database.api.EISDatabaseAuditQueriesType;
import com.io7m.eigion.tests.amberjack.EIAmberjackTest;
import com.io7m.eigion.tests.domaincheck.EIFakeServerDomainCheck;
import com.io7m.eigion.tests.domaincheck.EIFakeServerDomainCheckServlet;
import com.io7m.eigion.tests.domaincheck.EIInterceptHttpClient;
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.eigion.model.EIGroupRole.FOUNDER;
import static com.io7m.eigion.server.database.api.EISDatabaseRole.EIGION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@ExtendWith({EIIdStoreExtension.class, EIServerExtension.class})
public final class EIPikeTest
{
  private EIPClients clients;
  private EIPClientType client;
  private EIFakeServerDomainCheck domainCheckServer;

  @BeforeEach
  public void setup()
    throws Exception
  {
    this.clients = new EIPClients();
    this.client = this.clients.create(Locale.ROOT);
    this.domainCheckServer = EIFakeServerDomainCheck.create(20000);
    EIFakeServerDomainCheckServlet.RETURN_TOKEN = Optional.empty();
  }

  @AfterEach
  public void tearDown()
    throws Exception
  {
    this.client.close();
    this.domainCheckServer.close();
  }

  private static URI replaceURI(
    final URI u)
  {
    try {
      return new URI(
        "http",
        u.getUserInfo(),
        "localhost",
        20000,
        u.getPath(),
        u.getQuery(),
        u.getFragment()
      );
    } catch (final URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * It's not possible to log in if the user does not exist.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginNoSuchUser(
    final EIServerType server)
    throws Exception
  {
    final var ex =
      assertThrows(EIPClientException.class, () -> {
        this.client.login(
          "nonexistent",
          "12345678",
          EIServerExtension.PIKE_URI
        );
      });

    assertEquals(AUTHENTICATION_ERROR, ex.errorCode());
  }

  /**
   * Logging in works if the user has Pike permissions.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginUserOK(
    final EIIdStoreType idstore,
    final EIServerConfiguratorType configurator)
    throws Exception
  {
    this.setupStandardUserAndLogIn(idstore, configurator);
  }

  /**
   * Creating a group works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreationOK(
    final EIInterceptHttpClient httpClient,
    final EIIdStoreType idstore,
    final EIServerType server,
    final EIServerConfiguratorType configurator)
    throws Exception
  {
    httpClient.setFilterFunction(EIPikeTest::replaceURI);

    this.setupStandardUserAndLogIn(idstore, configurator);

    final var groupName =
      new EIGroupName("com.example");
    final var challenge =
      this.client.groupCreateBegin(groupName);

    assertEquals(groupName, challenge.groupName());

    EIFakeServerDomainCheckServlet.RETURN_TOKEN =
      Optional.of(challenge.token().value());

    this.client.groupCreateReady(challenge.token());

    /*
     * Wait for everything to settle.
     */

    Thread.sleep(1_00L);

    final var groups =
      this.client.groups()
        .current()
        .items();

    assertEquals(groupName, groups.get(0).group());
    assertTrue(groups.get(0).roles().implies(FOUNDER));

    final var requests =
      this.client.groupCreateRequests()
        .current()
        .items();

    assertEquals(1, requests.size());
    assertEquals(EIGroupCreationRequestStatusType.Succeeded.class, requests.get(0).status().getClass());

    this.checkAuditLog(
      server,
      new AuditCheck("USER_LOGGED_IN", null),
      new AuditCheck("GROUP_CREATION_REQUESTED", null),
      new AuditCheck("GROUP_CREATION_REQUEST_SUCCEEDED", null),
      new AuditCheck("GROUP_CREATED", "com.example")
    );
  }

  /**
   * Cancelling a group creation works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testGroupCreationCancel(
    final EIInterceptHttpClient httpClient,
    final EIIdStoreType idstore,
    final EIServerType server,
    final EIServerConfiguratorType configurator)
    throws Exception
  {
    httpClient.setFilterFunction(EIPikeTest::replaceURI);

    this.setupStandardUserAndLogIn(idstore, configurator);

    final var groupName =
      new EIGroupName("com.example");
    final var challenge =
      this.client.groupCreateBegin(groupName);

    assertEquals(groupName, challenge.groupName());

    EIFakeServerDomainCheckServlet.RETURN_TOKEN =
      Optional.of(challenge.token().value());

    this.client.groupCreateCancel(challenge.token());

    /*
     * Wait a second for everything to settle.
     */

    Thread.sleep(1_00L);

    final var groups =
      this.client.groups()
        .current()
        .items();

    assertEquals(0, groups.size());

    final var requests =
      this.client.groupCreateRequests()
        .current()
        .items();

    assertEquals(1, requests.size());
    assertEquals(EIGroupCreationRequestStatusType.Cancelled.class, requests.get(0).status().getClass());

    this.checkAuditLog(
      server,
      new AuditCheck("USER_LOGGED_IN", null),
      new AuditCheck("GROUP_CREATION_REQUESTED", null),
      new AuditCheck("GROUP_CREATION_REQUEST_CANCELLED", null)
    );
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
      EIServerExtension.PIKE_URI
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
    return disconnectionRelevantMethodsOf(EIPClientType.class)
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
          if (e.getCause() instanceof EIPClientException ex) {
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
    final Class<? extends EIPClientType> clazz)
  {
    return Stream.of(clazz.getMethods())
      .filter(EIPikeTest::isDisconnectionRelevantMethod);
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
