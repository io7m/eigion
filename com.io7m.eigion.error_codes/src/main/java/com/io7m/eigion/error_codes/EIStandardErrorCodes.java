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

package com.io7m.eigion.error_codes;

/**
 * Standard error codes.
 */

public final class EIStandardErrorCodes
{
  /**
   * The idstore server or clients failed.
   */

  public static final EIErrorCode IDSTORE_ERROR =
    new EIErrorCode("error-idstore");

  /**
   * The server failed to start.
   */

  public static final EIErrorCode SERVER_STARTUP_ERROR =
    new EIErrorCode("error-server-startup");

  /**
   * The server returned an error code for an HTTP request.
   */

  public static final EIErrorCode HTTP_ERROR =
    new EIErrorCode("error-http");

  /**
   * A client sent a broken message of some kind.
   */

  public static final EIErrorCode PROTOCOL_ERROR =
    new EIErrorCode("error-protocol");

  /**
   * The client and server have no supported protocols in common.
   */

  public static final EIErrorCode NO_SUPPORTED_PROTOCOLS =
    new EIErrorCode("error-no-supported-protocols");

  /**
   * Authenticating a user or admin failed.
   */

  public static final EIErrorCode AUTHENTICATION_ERROR =
    new EIErrorCode("error-authentication");
  /**
   * An internal I/O error.
   */

  public static final EIErrorCode IO_ERROR =
    new EIErrorCode("error-io");
  /**
   * An internal serialization error.
   */

  public static final EIErrorCode SERIALIZATION_ERROR =
    new EIErrorCode("error-serialization");
  /**
   * An error raised by the Trasco database versioning library.
   */

  public static final EIErrorCode TRASCO_ERROR =
    new EIErrorCode("error-trasco");
  /**
   * An internal SQL database error.
   */

  public static final EIErrorCode SQL_ERROR =
    new EIErrorCode("error-sql");
  /**
   * An internal SQL database error relating to database revisioning.
   */

  public static final EIErrorCode SQL_REVISION_ERROR =
    new EIErrorCode("error-sql-revision");
  /**
   * A violation of an SQL foreign key integrity constraint.
   */

  public static final EIErrorCode SQL_ERROR_FOREIGN_KEY =
    new EIErrorCode("error-sql-foreign-key");
  /**
   * A violation of an SQL uniqueness constraint.
   */

  public static final EIErrorCode SQL_ERROR_UNIQUE =
    new EIErrorCode("error-sql-unique");
  /**
   * An attempt was made to use a query class that is unsupported.
   */

  public static final EIErrorCode SQL_ERROR_UNSUPPORTED_QUERY_CLASS =
    new EIErrorCode("error-sql-unsupported-query-class");
  /**
   * A generic "operation not permitted" error.
   */

  public static final EIErrorCode OPERATION_NOT_PERMITTED =
    new EIErrorCode("error-operation-not-permitted");
  /**
   * An image is invalid.
   */

  public static final EIErrorCode IMAGE_INVALID =
    new EIErrorCode("error-image-invalid");
  /**
   * An attempt was made to create an image that already exists.
   */

  public static final EIErrorCode IMAGE_DUPLICATE =
    new EIErrorCode("error-image-duplicate");
  /**
   * An attempt was made to reference an image that does not exist.
   */

  public static final EIErrorCode IMAGE_NONEXISTENT =
    new EIErrorCode("error-image-nonexistent");
  /**
   * An attempt was made to create a user that already exists.
   */

  public static final EIErrorCode USER_DUPLICATE =
    new EIErrorCode("error-user-duplicate");
  /**
   * An attempt was made to create a user that already exists (ID conflict).
   */

  public static final EIErrorCode USER_DUPLICATE_ID =
    new EIErrorCode("error-user-duplicate-id");
  /**
   * An attempt was made to create a user that already exists (Name conflict).
   */

  public static final EIErrorCode USER_DUPLICATE_NAME =
    new EIErrorCode("error-user-duplicate-name");
  /**
   * An attempt was made to create a user that already exists (Email conflict).
   */

  public static final EIErrorCode USER_DUPLICATE_EMAIL =
    new EIErrorCode("error-user-duplicate-email");
  /**
   * An attempt was made to reference a user that does not exist.
   */

  public static final EIErrorCode USER_NONEXISTENT =
    new EIErrorCode("error-user-nonexistent");
  /**
   * An attempt was made to perform an operation that requires a user.
   */

  public static final EIErrorCode USER_UNSET =
    new EIErrorCode("error-user-unset");
  /**
   * An attempt was made to create an admin that already exists.
   */

  public static final EIErrorCode ADMIN_DUPLICATE =
    new EIErrorCode("error-admin-duplicate");
  /**
   * An attempt was made to create an admin that already exists (ID conflict).
   */

  public static final EIErrorCode ADMIN_DUPLICATE_ID =
    new EIErrorCode("error-admin-duplicate-id");
  /**
   * An attempt was made to create an admin that already exists (Name
   * conflict).
   */

  public static final EIErrorCode ADMIN_DUPLICATE_NAME =
    new EIErrorCode("error-admin-duplicate-name");
  /**
   * An attempt was made to create an admin that already exists (Email
   * conflict).
   */

  public static final EIErrorCode ADMIN_DUPLICATE_EMAIL =
    new EIErrorCode("error-admin-duplicate-email");
  /**
   * An attempt was made to reference an admin that does not exist.
   */

  public static final EIErrorCode ADMIN_NONEXISTENT =
    new EIErrorCode("error-admin-nonexistent");
  /**
   * An attempt was made to perform an operation that requires an admin.
   */

  public static final EIErrorCode ADMIN_UNSET =
    new EIErrorCode("error-admin-unset");
  /**
   * An attempt was made to create an initial admin in a database, but an admin
   * already existed.
   */

  public static final EIErrorCode ADMIN_NOT_INITIAL =
    new EIErrorCode("error-admin-not-initial");
  /**
   * A problem occurred with the format of a password (such as an unsupported
   * password algorithm).
   */

  public static final EIErrorCode PASSWORD_ERROR =
    new EIErrorCode("error-password");
  /**
   * An attempt was made to create a group that already exists.
   */

  public static final EIErrorCode GROUP_DUPLICATE =
    new EIErrorCode("error-group-duplicate");
  /**
   * An attempt was made to reference a group that does not exist.
   */

  public static final EIErrorCode GROUP_NONEXISTENT =
    new EIErrorCode("error-group-nonexistent");
  /**
   * An attempt was made by a group founder to leave the group.
   */

  public static final EIErrorCode GROUP_LEAVE_FOUNDER =
    new EIErrorCode("error-group-leave-founder");
  /**
   * An attempt was made to construct an invalid group name.
   */

  public static final EIErrorCode GROUP_NAME_INVALID =
    new EIErrorCode("error-group-name-invalid");
  /**
   * An attempt was made to create a group creation request that already
   * exists.
   */

  public static final EIErrorCode GROUP_REQUEST_DUPLICATE =
    new EIErrorCode("error-group-create-request-duplicate");
  /**
   * An attempt was made to reference a group creation request that does not
   * exist.
   */

  public static final EIErrorCode GROUP_REQUEST_NONEXISTENT =
    new EIErrorCode("error-group-create-request-nonexistent");
  /**
   * A group creation request does not belong to the user that tried to do
   * something with it.
   */

  public static final EIErrorCode GROUP_REQUEST_WRONG_USER =
    new EIErrorCode("error-group-create-request-wrong-user");
  /**
   * An attempt was made to perform an operation on group creation request that
   * turned out to be in the wrong state.
   */

  public static final EIErrorCode GROUP_REQUEST_WRONG_STATE =
    new EIErrorCode("error-group-create-request-wrong-state");
  /**
   * An attempt was made to reference a group invite that does not exist.
   */

  public static final EIErrorCode GROUP_INVITE_NONEXISTENT =
    new EIErrorCode("error-group-invite-nonexistent");
  /**
   * The inviter and invitee of an invite must be different.
   */

  public static final EIErrorCode GROUP_INVITE_SELF =
    new EIErrorCode("error-group-invite-self");
  /**
   * An attempt was made to perform an operation on a group invite by someone
   * other than the owner of the invite.
   */

  public static final EIErrorCode GROUP_INVITE_WRONG_USER =
    new EIErrorCode("error-group-invite-wrong-user");
  /**
   * An attempt was made to perform an operation on group invite that turned out
   * to be in the wrong state.
   */

  public static final EIErrorCode GROUP_INVITE_WRONG_STATE =
    new EIErrorCode("error-group-invite-wrong-state");
  /**
   * An attempt was made to reference a category that does not exist.
   */

  public static final EIErrorCode CATEGORY_NONEXISTENT =
    new EIErrorCode("error-category-nonexistent");
  /**
   * An attempt was made to reference a product that does not exist.
   */

  public static final EIErrorCode PRODUCT_NONEXISTENT =
    new EIErrorCode("error-product-nonexistent");
  /**
   * An attempt was made to create a product that already exists.
   */

  public static final EIErrorCode PRODUCT_DUPLICATE =
    new EIErrorCode("error-product-duplicate");
  /**
   * An attempt was made to reference a release that does not exist.
   */

  public static final EIErrorCode RELEASE_NONEXISTENT =
    new EIErrorCode("error-release-nonexistent");
  /**
   * An attempt was made to create a release that already exists.
   */

  public static final EIErrorCode RELEASE_DUPLICATE =
    new EIErrorCode("error-release-duplicate");
  /**
   * An action was denied by the security policy.
   */

  public static final EIErrorCode SECURITY_POLICY_DENIED =
    new EIErrorCode("error-security-policy-denied");
  /**
   * The wrong HTTP method was used.
   */

  public static final EIErrorCode HTTP_METHOD_ERROR =
    new EIErrorCode("error-http-method");
  /**
   * An HTTP parameter was required but missing.
   */

  public static final EIErrorCode HTTP_PARAMETER_NONEXISTENT =
    new EIErrorCode("error-http-parameter-nonexistent");
  /**
   * An HTTP parameter had an invalid value.
   */

  public static final EIErrorCode HTTP_PARAMETER_INVALID =
    new EIErrorCode("error-http-parameter-invalid");
  /**
   * An HTTP request exceeded the size limit.
   */

  public static final EIErrorCode HTTP_SIZE_LIMIT =
    new EIErrorCode("error-http-size-limit");

  /**
   * The client is not logged in.
   */

  public static final EIErrorCode NOT_LOGGED_IN =
    new EIErrorCode("error-not-logged-in");

  /**
   * An API was misused.
   */

  public static final EIErrorCode USAGE_ERROR =
    new EIErrorCode("error-usage");

  private EIStandardErrorCodes()
  {

  }
}
