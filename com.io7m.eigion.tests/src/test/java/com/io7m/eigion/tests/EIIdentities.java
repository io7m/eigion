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

import com.io7m.eigion.model.EIPasswordException;
import com.io7m.eigion.model.EISubsetMatch;
import com.io7m.eigion.protocol.admin_api.v1.EISA1AuditEvent;
import com.io7m.eigion.protocol.admin_api.v1.EISA1Password;
import com.io7m.eigion.protocol.admin_api.v1.EISA1SubsetMatch;
import com.io7m.eigion.protocol.admin_api.v1.EISA1User;
import com.io7m.eigion.protocol.admin_api.v1.EISA1UserSummary;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class EIIdentities
{
  @Provide
  public Arbitrary<EISubsetMatch<String>> subsets()
  {
    return EIArbitraries.subsetMatches(Arbitraries.strings());
  }

  @Provide
  public Arbitrary<EISA1SubsetMatch<String>> subsetsV1()
  {
    return EIArbitraries.subsetMatchesV1(Arbitraries.strings());
  }

  @Provide
  public Arbitrary<EISA1AuditEvent> auditEventsV1()
  {
    return EIArbitraries.auditEventsV1();
  }

  @Provide
  public Arbitrary<EISA1Password> passwordsV1()
  {
    return EIArbitraries.passwordsV1();
  }

  @Provide
  public Arbitrary<EISA1User> usersV1()
  {
    return EIArbitraries.userV1();
  }

  @Provide
  public Arbitrary<EISA1UserSummary> userSummaryV1()
  {
    return EIArbitraries.userSummaryV1();
  }

  @Property
  public void testEISA1SubsetMatch(
    @ForAll("subsetsV1") final EISA1SubsetMatch<String> x)
  {
    assertEquals(x, EISA1SubsetMatch.ofSubsetMatch(x.toSubsetMatch()));
  }

  @Property
  public void testEISA1AuditEvent(
    @ForAll("auditEventsV1") final EISA1AuditEvent x)
  {
    assertEquals(x, EISA1AuditEvent.ofAuditEvent(x.toAuditEvent()));
  }

  @Property
  public void testEISA1Password(
    @ForAll("passwordsV1") final EISA1Password x)
    throws EIPasswordException
  {
    assertEquals(x, EISA1Password.ofPassword(x.toPassword()));
  }

  @Property
  public void testEISA1User(
    @ForAll("usersV1") final EISA1User x)
    throws EIPasswordException
  {
    assertEquals(x, EISA1User.ofUser(x.toUser()));
  }

  @Property
  public void testEISA1UserSummary(
    @ForAll("userSummaryV1") final EISA1UserSummary x)
  {
    assertEquals(x, EISA1UserSummary.ofUserSummary(x.toUserSummary()));
  }
}
