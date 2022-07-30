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

import com.io7m.eigion.tests.arbitraries.EIArbEISP1HashProvider;
import net.jqwik.api.providers.ArbitraryProvider;

/**
 * Eigion platform (Arbitrary instances)
 */

module com.io7m.eigion.tests.arbitraries
{
  requires static org.osgi.annotation.bundle;
  requires static org.osgi.annotation.versioning;

  requires transitive com.io7m.eigion.protocol.admin_api.v1;
  requires transitive com.io7m.eigion.protocol.public_api.v1;
  requires transitive com.io7m.eigion.model;
  requires transitive net.jqwik.api;

  provides ArbitraryProvider
    with
      com.io7m.eigion.tests.arbitraries.EIArbAdminProvider,
      com.io7m.eigion.tests.arbitraries.EIArbAdminSummaryProvider,
      com.io7m.eigion.tests.arbitraries.EIArbAuditEventProvider,
      com.io7m.eigion.tests.arbitraries.EIArbCreationProvider,
      com.io7m.eigion.tests.arbitraries.EIArbEISA1AdminProvider,
      com.io7m.eigion.tests.arbitraries.EIArbEISA1AdminSummaryProvider,
      com.io7m.eigion.tests.arbitraries.EIArbEISA1AuditEventProvider,
      com.io7m.eigion.tests.arbitraries.EIArbEISA1PasswordProvider,
      com.io7m.eigion.tests.arbitraries.EIArbEISA1SubsetMatchProvider,
      com.io7m.eigion.tests.arbitraries.EIArbEISA1UserBanProvider,
      com.io7m.eigion.tests.arbitraries.EIArbEISA1UserProvider,
      com.io7m.eigion.tests.arbitraries.EIArbEISA1UserSummaryProvider,
      com.io7m.eigion.tests.arbitraries.EIArbEISP1HashProvider,
      com.io7m.eigion.tests.arbitraries.EIArbGroupCreationRequestProvider,
      com.io7m.eigion.tests.arbitraries.EIArbGroupCreationRequestStatusProvider,
      com.io7m.eigion.tests.arbitraries.EIArbGroupNameProvider,
      com.io7m.eigion.tests.arbitraries.EIArbGroupPrefixProvider,
      com.io7m.eigion.tests.arbitraries.EIArbHashProvider,
      com.io7m.eigion.tests.arbitraries.EIArbOffsetDateTimeProvider,
      com.io7m.eigion.tests.arbitraries.EIArbPasswordProvider,
      com.io7m.eigion.tests.arbitraries.EIArbRedactionProvider,
      com.io7m.eigion.tests.arbitraries.EIArbRedactionRequestProvider,
      com.io7m.eigion.tests.arbitraries.EIArbSubsetMatchProvider,
      com.io7m.eigion.tests.arbitraries.EIArbTokenProvider,
      com.io7m.eigion.tests.arbitraries.EIArbUUIDProvider,
      com.io7m.eigion.tests.arbitraries.EIArbUserBanProvider,
      com.io7m.eigion.tests.arbitraries.EIArbUserDisplayNameProvider,
      com.io7m.eigion.tests.arbitraries.EIArbUserEmailProvider,
      com.io7m.eigion.tests.arbitraries.EIArbUserProvider,
      com.io7m.eigion.tests.arbitraries.EIArbUserSummaryProvider,
      com.io7m.eigion.tests.arbitraries.EIArbUserUserComplaintProvider,
      com.io7m.eigion.tests.arbitraries.EIArbUserUserComplaintResolutionProvider
    ;

  exports com.io7m.eigion.tests.arbitraries;
}
