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

package com.io7m.eigion.server.vanilla.internal;

import com.io7m.eigion.server.api.EIServerAdminSharedSecret;
import com.io7m.eigion.services.api.EIServiceType;

import java.util.Objects;

/**
 * A service that exposes the Admin UI shared secret.
 */

public final class EIServerSharedSecretService
  implements EIServiceType
{
  private final EIServerAdminSharedSecret secret;

  /**
   * A service that exposes the Admin UI shared secret.
   *
   * @param inSecret A shared secret
   */

  public EIServerSharedSecretService(
    final EIServerAdminSharedSecret inSecret)
  {
    this.secret = Objects.requireNonNull(inSecret, "secret");
  }

  /**
   * @return The shared secret
   */

  public EIServerAdminSharedSecret secret()
  {
    return this.secret;
  }

  @Override
  public String description()
  {
    return "Admin API shared secret service.";
  }

  @Override
  public String toString()
  {
    return "[EIServerSharedSecretService 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode()));
  }
}
