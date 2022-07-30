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
import com.io7m.eigion.protocol.admin_api.v1.EISA1Admin;
import com.io7m.eigion.protocol.admin_api.v1.EISA1AdminPermission;
import com.io7m.eigion.protocol.admin_api.v1.EISA1AdminSummary;
import com.io7m.eigion.protocol.admin_api.v1.EISA1AuditEvent;
import com.io7m.eigion.protocol.admin_api.v1.EISA1GroupRole;
import com.io7m.eigion.protocol.admin_api.v1.EISA1Password;
import com.io7m.eigion.protocol.admin_api.v1.EISA1SubsetMatch;
import com.io7m.eigion.protocol.admin_api.v1.EISA1User;
import com.io7m.eigion.protocol.admin_api.v1.EISA1UserSummary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class EIIdentities
{
  @Property
  public void testEISA1SubsetMatch(
    @ForAll final EISA1SubsetMatch<String> x)
  {
    assertEquals(x, EISA1SubsetMatch.ofSubsetMatch(x.toSubsetMatch()));
  }

  @Property
  public void testEISA1AuditEvent(
    @ForAll final EISA1AuditEvent x)
  {
    assertEquals(x, EISA1AuditEvent.ofAuditEvent(x.toAuditEvent()));
  }

  @Property
  public void testEISA1Password(
    @ForAll final EISA1Password x)
    throws EIPasswordException
  {
    assertEquals(x, EISA1Password.ofPassword(x.toPassword()));
  }

  @Property
  public void testEISA1User(
    @ForAll final EISA1User x)
    throws EIPasswordException
  {
    assertEquals(x, EISA1User.ofUser(x.toUser()));
  }

  @Property
  public void testEISA1UserSummary(
    @ForAll final EISA1UserSummary x)
  {
    assertEquals(x, EISA1UserSummary.ofUserSummary(x.toUserSummary()));
  }

  @Property
  public void testEISA1GroupRole(
    @ForAll final EISA1GroupRole x)
  {
    assertEquals(x, EISA1GroupRole.ofGroupRole(x.toGroupRole()));
  }

  @Property
  public void testEISA1AdminPermission(
    @ForAll final EISA1AdminPermission x)
  {
    assertEquals(x, EISA1AdminPermission.ofAdmin(x.toAdmin()));
  }

  @Property
  public void testEISA1Admin(
    @ForAll final EISA1Admin x)
    throws EIPasswordException
  {
    assertEquals(x, EISA1Admin.ofAdmin(x.toAdmin()));
  }

  @Property
  public void testEISA1AdminSummary(
    @ForAll final EISA1AdminSummary x)
  {
    assertEquals(x, EISA1AdminSummary.ofAdminSummary(x.toAdminSummary()));
  }
}
