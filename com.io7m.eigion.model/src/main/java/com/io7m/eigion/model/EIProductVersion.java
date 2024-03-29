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

package com.io7m.eigion.model;

import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.math.BigInteger.ZERO;
import static java.util.regex.Pattern.UNICODE_CHARACTER_CLASS;

/**
 * A product (semantic) version.
 *
 * @param major     The major version
 * @param minor     The minor version
 * @param patch     The patch version
 * @param qualifier The qualifier
 */

public record EIProductVersion(
  BigInteger major,
  BigInteger minor,
  BigInteger patch,
  Optional<String> qualifier)
  implements Comparable<EIProductVersion>
{
  /**
   * The pattern that defines a valid version.
   */

  public static final Pattern VALID_VERSION =
    Pattern.compile(
      "(\\p{Digit}{1,20})\\.(\\p{Digit}{1,20})\\.(\\p{Digit}{1,20})(-[\\p{Alnum}]{1,64})?",
      UNICODE_CHARACTER_CLASS
    );

  /**
   * The pattern that defines a valid qualifier.
   */

  public static final Pattern VALID_QUALIFIER =
    Pattern.compile(
      "[\\p{Alnum}]{1,64}",
      UNICODE_CHARACTER_CLASS
    );

  private static final Pattern LEADING_HYPHEN =
    Pattern.compile("^-");

  /**
   * A product (semantic) version.
   *
   * @param major     The major version
   * @param minor     The minor version
   * @param patch     The patch version
   * @param qualifier The qualifier
   */

  public EIProductVersion
  {
    Objects.requireNonNull(major, "major");
    Objects.requireNonNull(minor, "minor");
    Objects.requireNonNull(patch, "patch");
    Objects.requireNonNull(qualifier, "qualifier");

    if (major.compareTo(ZERO) < 0) {
      throw new EIValidityException(
        "Major version %s must be non-negative".formatted(major));
    }
    if (minor.compareTo(ZERO) < 0) {
      throw new EIValidityException(
        "Minor version %s must be non-negative".formatted(minor));
    }
    if (patch.compareTo(ZERO) < 0) {
      throw new EIValidityException(
        "Patch version %s must be non-negative".formatted(patch));
    }

    qualifier.ifPresent(q -> {
      final var matcher = VALID_QUALIFIER.matcher(q);
      if (!matcher.matches()) {
        throw new EIValidityException(
          String.format("Qualifier '%s' must match '%s'", q, VALID_QUALIFIER));
      }
    });
  }

  private static int compareQualifiers(
    final Optional<String> q0,
    final Optional<String> q1)
  {
    /*
     * If q0 has a qualifier, and q1 doesn't, then q1 > q0.
     */

    if (q0.isPresent()) {
      if (q1.isEmpty()) {
        return -1;
      }

      /*
       * Both q0 and q1 have qualifiers, so compare them lexically.
       */

      return q0.get().compareTo(q1.get());
    }

    /*
     * Otherwise, there's no q0. If q1 is empty, then the comparison is equal.
     */

    if (q1.isEmpty()) {
      return 0;
    }

    /*
     * Otherwise, q0 > q1.
     */

    return 1;
  }

  /**
   * Parse a version string.
   *
   * @param text The version string
   *
   * @return A parsed version
   *
   * @throws IllegalArgumentException On parse errors
   */

  public static EIProductVersion parse(
    final String text)
    throws IllegalArgumentException
  {
    Objects.requireNonNull(text, "text");

    final var versionMatcher = VALID_VERSION.matcher(text);
    if (!versionMatcher.matches()) {
      throw new EIValidityException(
        String.format("Version '%s' must match %s", text, VALID_VERSION));
    }

    final var newMajor =
      new BigInteger(versionMatcher.group(1));
    final var newMinor =
      new BigInteger(versionMatcher.group(2));
    final var newPatch =
      new BigInteger(versionMatcher.group(3));

    final var qualRaw = versionMatcher.group(4);
    final Optional<String> newQualifier;
    if (qualRaw != null) {
      newQualifier = Optional.of(
        LEADING_HYPHEN.matcher(qualRaw)
          .replaceFirst("")
      );
    } else {
      newQualifier = Optional.empty();
    }

    return new EIProductVersion(newMajor, newMinor, newPatch, newQualifier);
  }

  @Override
  public int compareTo(
    final EIProductVersion other)
  {
    final var cmaj = this.major.compareTo(other.major);
    if (cmaj != 0) {
      return cmaj;
    }
    final var cmin = this.minor.compareTo(other.minor);
    if (cmin != 0) {
      return cmin;
    }
    final var cpat = this.patch.compareTo(other.patch);
    if (cpat != 0) {
      return cpat;
    }
    return compareQualifiers(this.qualifier, other.qualifier);
  }

  /**
   * {@code ∀ v. parse(v.show()) = v}
   *
   * @return The product version as a humanly-readable string
   */

  public String show()
  {
    if (this.qualifier.isPresent()) {
      return String.format(
        "%s.%s.%s-%s",
        this.major,
        this.minor,
        this.patch,
        this.qualifier.get()
      );
    }
    return String.format(
      "%s.%s.%s",
      this.major,
      this.minor,
      this.patch
    );
  }
}
