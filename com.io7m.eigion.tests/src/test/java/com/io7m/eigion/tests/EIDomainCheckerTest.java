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

import com.io7m.eigion.domaincheck.EIDomainCheckers;
import com.io7m.eigion.domaincheck.api.EIDomainCheckerConfiguration;
import com.io7m.eigion.domaincheck.api.EIDomainCheckerType;
import com.io7m.eigion.model.EIGroupCreationRequest;
import com.io7m.eigion.model.EIGroupCreationRequestStatusType;
import com.io7m.eigion.model.EIGroupCreationRequestStatusType.InProgress;
import com.io7m.eigion.model.EIGroupName;
import com.io7m.eigion.model.EIToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.eigion.tests.EIServerContract.timeNow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public final class EIDomainCheckerTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIDomainCheckerTest.class);

  private EIDomainCheckers checkers;
  private EIFakeClock clock;
  private EIDomainCheckerType checker;
  private EIInterceptHttpClient httpClient;
  private EIFakeServerDomainCheck server;

  @BeforeEach
  public void setup()
    throws Exception
  {
    this.clock = new EIFakeClock();
    this.server = EIFakeServerDomainCheck.create(20000);

    this.httpClient = new EIInterceptHttpClient(
      EIDomainCheckerTest::replaceURI,
      HttpClient.newHttpClient()
    );

    this.checkers = new EIDomainCheckers();
    this.checker = this.checkers.createChecker(
      new EIDomainCheckerConfiguration(this.clock, this.httpClient)
    );
  }

  private static URI replaceURI(
    final URI u)
  {
    try {
      return new URI(
        "http",
        u.getUserInfo(),
        u.getHost(),
        20000,
        u.getPath(),
        u.getQuery(),
        u.getFragment()
      );
    } catch (final URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  @AfterEach
  public void tearDown()
    throws Exception
  {
    this.checker.close();
    this.server.close();
  }

  /**
   * If the server returns the right token, the check succeeds.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCheckSucceeds()
    throws Exception
  {
    final var token =
      "73CB3858A687A8494CA3323053016282F3DAD39D42CF62CA4E79DDA2AAC7D9AC";

    EIFakeServerDomainCheckServlet.RETURN_TOKEN = Optional.of(token);

    final var checkFuture =
      this.checker.check(
        new EIGroupCreationRequest(
          new EIGroupName("localhost"),
          UUID.randomUUID(),
          new EIToken(token),
          new InProgress(timeNow())
        )
      );

    final var result =
      checkFuture.get();
    final var status =
      result.status();

    if (status instanceof EIGroupCreationRequestStatusType.Succeeded succeeded) {
      LOG.debug("succeeded: {}", succeeded);
    } else {
      fail();
    }
  }

  /**
   * If the server returns the wrong token, the check fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCheckFailsWrongToken()
    throws Exception
  {
    final var tokenExpect =
      "73CB3858A687A8494CA3323053016282F3DAD39D42CF62CA4E79DDA2AAC7D9AC";
    final var tokenReceive =
      "3BB2ABB69EBB27FBFE63C7639624C6EC5E331B841A5BC8C3EBC10B9285E90877";

    EIFakeServerDomainCheckServlet.RETURN_TOKEN = Optional.of(tokenReceive);

    final var checkFuture =
      this.checker.check(
        new EIGroupCreationRequest(
          new EIGroupName("localhost"),
          UUID.randomUUID(),
          new EIToken(tokenExpect),
          new InProgress(timeNow())
        )
      );

    final var result =
      checkFuture.get();
    final var status =
      result.status();

    if (status instanceof EIGroupCreationRequestStatusType.Failed failed) {
      LOG.debug("failed: {}", failed);
      assertTrue(failed.message().contains("Token did not match."));
    } else {
      fail();
    }
  }

  /**
   * If the server doesn't return a token, the check fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCheckFails404()
    throws Exception
  {
    EIFakeServerDomainCheckServlet.RETURN_TOKEN = Optional.empty();

    final var checkFuture =
      this.checker.check(
        new EIGroupCreationRequest(
          new EIGroupName("localhost"),
          UUID.randomUUID(),
          new EIToken(
            "73CB3858A687A8494CA3323053016282F3DAD39D42CF62CA4E79DDA2AAC7D9AC"),
          new InProgress(timeNow())
        )
      );

    final var result =
      checkFuture.get();
    final var status =
      result.status();

    if (status instanceof EIGroupCreationRequestStatusType.Failed failed) {
      LOG.debug("failed: {}", failed);
      assertTrue(failed.message().contains("404"));
    } else {
      fail();
    }
  }

  /**
   * If the server connection fails, the check fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCheckFailsConnection()
    throws Exception
  {
    this.server.close();

    final var checkFuture =
      this.checker.check(
        new EIGroupCreationRequest(
          new EIGroupName("localhost"),
          UUID.randomUUID(),
          new EIToken(
            "73CB3858A687A8494CA3323053016282F3DAD39D42CF62CA4E79DDA2AAC7D9AC"),
          new InProgress(timeNow())
        )
      );

    final var result =
      checkFuture.get();
    final var status =
      result.status();

    if (status instanceof EIGroupCreationRequestStatusType.Failed failed) {
      LOG.debug("failed: {}", failed);
      assertTrue(failed.message().contains("java.net.ConnectException"));
    } else {
      fail();
    }
  }
}
