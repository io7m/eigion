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

import com.io7m.eigion.tests.arbitraries.EIArbAmberjackMessageProvider;
import com.io7m.eigion.tests.arbitraries.EIArbAuditEventProvider;
import com.io7m.eigion.tests.arbitraries.EIArbAuditSearchParametersProvider;
import com.io7m.eigion.tests.arbitraries.EIArbCreationProvider;
import com.io7m.eigion.tests.arbitraries.EIArbErrorCodeProvider;
import com.io7m.eigion.tests.arbitraries.EIArbGroupCreationRequestProvider;
import com.io7m.eigion.tests.arbitraries.EIArbGroupCreationRequestStatusProvider;
import com.io7m.eigion.tests.arbitraries.EIArbGroupInviteProvider;
import com.io7m.eigion.tests.arbitraries.EIArbGroupMembershipProvider;
import com.io7m.eigion.tests.arbitraries.EIArbGroupNameProvider;
import com.io7m.eigion.tests.arbitraries.EIArbGroupPrefixProvider;
import com.io7m.eigion.tests.arbitraries.EIArbGroupSearchByNameParametersProvider;
import com.io7m.eigion.tests.arbitraries.EIArbHashProvider;
import com.io7m.eigion.tests.arbitraries.EIArbOffsetDateTimeProvider;
import com.io7m.eigion.tests.arbitraries.EIArbPermissionSetProvider;
import com.io7m.eigion.tests.arbitraries.EIArbPikeMessageProvider;
import com.io7m.eigion.tests.arbitraries.EIArbRedactionProvider;
import com.io7m.eigion.tests.arbitraries.EIArbRedactionRequestProvider;
import com.io7m.eigion.tests.arbitraries.EIArbTimeRangeProvider;
import com.io7m.eigion.tests.arbitraries.EIArbTokenProvider;
import com.io7m.eigion.tests.arbitraries.EIArbURIProvider;
import com.io7m.eigion.tests.arbitraries.EIArbUUIDProvider;
import com.io7m.eigion.tests.arbitraries.EIArbUserDisplayNameProvider;
import com.io7m.eigion.tests.arbitraries.EIArbUserProvider;
import net.jqwik.api.providers.ArbitraryProvider;

/**
 * Eigion platform (Arbitrary instances)
 */

module com.io7m.eigion.tests.arbitraries
{
  requires static org.osgi.annotation.bundle;
  requires static org.osgi.annotation.versioning;

  requires transitive com.io7m.eigion.protocol.pike;
  requires transitive com.io7m.eigion.protocol.amberjack;
  requires transitive com.io7m.eigion.model;
  requires transitive net.jqwik.api;

  provides ArbitraryProvider
    with
      EIArbAmberjackMessageProvider,
      EIArbAuditEventProvider,
      EIArbAuditSearchParametersProvider,
      EIArbCreationProvider,
      EIArbErrorCodeProvider,
      EIArbGroupCreationRequestProvider,
      EIArbGroupCreationRequestStatusProvider,
      EIArbGroupInviteProvider,
      EIArbGroupMembershipProvider,
      EIArbGroupNameProvider,
      EIArbGroupPrefixProvider,
      EIArbGroupSearchByNameParametersProvider,
      EIArbHashProvider,
      EIArbOffsetDateTimeProvider,
      EIArbPermissionSetProvider,
      EIArbPikeMessageProvider,
      EIArbRedactionProvider,
      EIArbRedactionRequestProvider,
      EIArbTimeRangeProvider,
      EIArbTokenProvider,
      EIArbURIProvider,
      EIArbUUIDProvider,
      EIArbUserDisplayNameProvider,
      EIArbUserProvider
    ;

  exports com.io7m.eigion.tests.arbitraries;
}
