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

import com.io7m.eigion.amberjack.EIAJClients;
import com.io7m.eigion.amberjack.api.EIAJClientException;
import com.io7m.eigion.amberjack.api.EIAJClientType;
import com.io7m.eigion.model.EIPermissionSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Locale;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.OPERATION_NOT_PERMITTED;
import static com.io7m.eigion.model.EIPermission.AMBERJACK_ACCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class EIAmberjackTest extends EIWithServerContract
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
          this.server().baseAmberjackURI()
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
  public void testLoginUserNotPermitted0()
    throws Exception
  {
    this.idstore()
      .createUser("noone", "12345678");

    final var ex =
      assertThrows(EIAJClientException.class, () -> {
        this.client.login(
          "noone",
          "12345678",
          this.server().baseAmberjackURI()
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
  public void testLoginUserNotPermitted1()
    throws Exception
  {
    final var userId =
      this.idstore()
        .createUser("noone", "12345678");

    this.server()
      .configurator()
      .userSetPermissions(userId, EIPermissionSet.empty());

    final var ex =
      assertThrows(EIAJClientException.class, () -> {
        this.client.login(
          "noone",
          "12345678",
          this.server().baseAmberjackURI()
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
  public void testLoginUserOK()
    throws Exception
  {
    final var userId =
      this.idstore()
        .createUser("noone", "12345678");

    this.server()
      .configurator()
      .userSetPermissions(userId, EIPermissionSet.of(AMBERJACK_ACCESS));

    this.client.login(
      "noone",
      "12345678",
      this.server().baseAmberjackURI()
    );
  }
}
