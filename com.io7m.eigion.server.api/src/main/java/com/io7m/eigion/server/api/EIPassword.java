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

package com.io7m.eigion.server.api;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Pattern;

import static java.util.Locale.ROOT;

/**
 * A hashed password for a user.
 *
 * @param algorithm The hash algorithm
 * @param hash      The hashed password
 * @param salt      The salt value
 */

public record EIPassword(
  String algorithm,
  String hash,
  String salt)
{
  /**
   * The pattern that defines a valid hash.
   */

  public static final Pattern VALID_HEX =
    Pattern.compile("[A-F0-9]+");

  /**
   * A hashed password for a user.
   *
   * @param algorithm The hash algorithm
   * @param hash      The hashed password
   * @param salt      The salt value
   */

  public EIPassword
  {
    Objects.requireNonNull(algorithm, "algorithm");
    Objects.requireNonNull(hash, "hash");
    Objects.requireNonNull(salt, "salt");

    if (!VALID_HEX.matcher(hash).matches()) {
      throw new IllegalArgumentException("Hash must match " + VALID_HEX);
    }
    if (!VALID_HEX.matcher(salt).matches()) {
      throw new IllegalArgumentException("Salt must match " + VALID_HEX);
    }
  }

  /**
   * Create a hashed password from the raw password text. Salt is generated.
   *
   * @param passwordText The password text
   *
   * @return A hashed password
   *
   * @throws NoSuchAlgorithmException If the JVM does not support the hash
   *                                  algorithm
   * @throws InvalidKeySpecException  If the secret key is somehow invalid
   */

  public static EIPassword createHashed(
    final String passwordText)
    throws NoSuchAlgorithmException,
    InvalidKeySpecException
  {
    Objects.requireNonNull(passwordText, "passwordText");

    final var salt = new byte[16];
    final var rng =
      SecureRandom.getInstanceStrong();
    rng.nextBytes(salt);
    return createHashed(
      passwordText,
      salt
    );
  }

  /**
   * Create a hashed password from the raw password text.
   *
   * @param passwordText The password text
   * @param salt         The salt
   *
   * @return A hashed password
   *
   * @throws NoSuchAlgorithmException If the JVM does not support the hash
   *                                  algorithm
   * @throws InvalidKeySpecException  If the secret key is somehow invalid
   */

  public static EIPassword createHashed(
    final String passwordText,
    final byte[] salt)
    throws NoSuchAlgorithmException, InvalidKeySpecException
  {
    Objects.requireNonNull(passwordText, "passwordText");
    Objects.requireNonNull(salt, "salt");

    final var formatter =
      HexFormat.of();
    final var passwordSalt =
      formatter.formatHex(salt);
    final var keyFactory =
      SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    final var keySpec =
      new PBEKeySpec(passwordText.toCharArray(), salt, 10000, 256);
    final var hash =
      keyFactory.generateSecret(keySpec).getEncoded();
    final var passwordHash =
      formatter.formatHex(hash);

    return new EIPassword(
      "PBKDF2WithHmacSHA256:10000:256",
      passwordHash.toUpperCase(ROOT),
      passwordSalt.toUpperCase(ROOT)
    );
  }
}
