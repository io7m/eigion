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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;

/**
 * The type of password hashing algorithms.
 */

public sealed interface EIPasswordAlgorithmType
  permits EIPasswordAlgorithmPBKDF2HmacSHA256
{
  /**
   * Check if the given plain text password matches the expected hash.
   *
   * @param expectedHash     The expected hexadecimal uppercase hash
   * @param receivedPassword The received plain text password
   * @param salt             The salt value
   *
   * @return {@code true} if the password matches
   *
   * @throws EIPasswordException On internal errors such as missing algorithm
   *                             support
   */

  boolean check(
    String expectedHash,
    String receivedPassword,
    byte[] salt)
    throws EIPasswordException;

  /**
   * Create a hashed password.
   *
   * @param passwordText The plain text password
   * @param salt         A random salt value
   *
   * @return A hashed password
   *
   * @throws EIPasswordException On internal errors such as missing algorithm
   *                             support
   */

  EIPassword createHashed(
    String passwordText,
    byte[] salt)
    throws EIPasswordException;

  /**
   * Create a hashed password with a random salt.
   *
   * @param passwordText The plain text password
   *
   * @return A hashed password
   *
   * @throws EIPasswordException On internal errors such as missing algorithm
   *                             support
   */

  default EIPassword createHashed(
    final String passwordText)
    throws EIPasswordException
  {
    Objects.requireNonNull(passwordText, "passwordText");

    try {
      final var salt = new byte[16];
      final var rng = SecureRandom.getInstanceStrong();
      rng.nextBytes(salt);
      return this.createHashed(passwordText, salt);
    } catch (final NoSuchAlgorithmException e) {
      throw new EIPasswordException(e.getMessage(), e);
    }
  }

  /**
   * @return The password algorithm identifier
   */

  String identifier();
}
