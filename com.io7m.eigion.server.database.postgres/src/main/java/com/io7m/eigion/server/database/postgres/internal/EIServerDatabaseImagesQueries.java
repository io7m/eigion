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

package com.io7m.eigion.server.database.postgres.internal;

import com.io7m.eigion.hash.EIHash;
import com.io7m.eigion.model.EICreation;
import com.io7m.eigion.model.EIImage;
import com.io7m.eigion.model.EIRedaction;
import com.io7m.eigion.model.EIRedactionRequest;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseImagesQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseIncludeRedacted;
import com.io7m.eigion.server.database.api.EIServerDatabaseRequiresUser;
import com.io7m.eigion.server.database.api.EIServerDatabaseUsersQueriesType;
import com.io7m.jaffirm.core.Preconditions;
import org.jooq.exception.DataAccessException;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.IMAGE_DUPLICATE;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.IMAGE_NONEXISTENT;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.USER_NONEXISTENT;
import static com.io7m.eigion.server.database.api.EIServerDatabaseIncludeRedacted.EXCLUDE_REDACTED;
import static com.io7m.eigion.server.database.api.EIServerDatabaseIncludeRedacted.INCLUDE_REDACTED;
import static com.io7m.eigion.server.database.postgres.internal.EIServerDatabaseExceptions.handleDatabaseException;
import static com.io7m.eigion.server.database.postgres.internal.Tables.IMAGES;
import static com.io7m.eigion.server.database.postgres.internal.Tables.IMAGE_REDACTIONS;
import static com.io7m.eigion.server.database.postgres.internal.tables.Audit.AUDIT;

final class EIServerDatabaseImagesQueries
  extends EIBaseQueries
  implements EIServerDatabaseImagesQueriesType
{
  private final EIServerDatabaseUsersQueriesType users;

  EIServerDatabaseImagesQueries(
    final EIServerDatabaseTransaction inTransaction,
    final EIServerDatabaseUsersQueriesType inUsers)
  {
    super(inTransaction);
    this.users = Objects.requireNonNull(inUsers, "users");
  }

  @Override
  @EIServerDatabaseRequiresUser
  public EIImage imageCreate(
    final UUID id,
    final EIHash hash)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(hash, "hash");

    final var owner =
      this.transaction().userId();
    final var context =
      this.transaction().createContext();

    try {
      if (this.imageGet(id, INCLUDE_REDACTED).isPresent()) {
        throw new EIServerDatabaseException(
          "Image already exists",
          IMAGE_DUPLICATE
        );
      }

      this.fetchUserOrFail(owner);

      final var time =
        this.currentTime();

      final var inserted =
        context.insertInto(IMAGES)
          .set(IMAGES.ID, id)
          .set(IMAGES.CREATED, time)
          .set(IMAGES.CREATOR, owner)
          .set(IMAGES.HASH_ALGO, hash.algorithm())
          .set(IMAGES.HASH_VALUE, hash.hash())
          .execute();

      Preconditions.checkPreconditionV(
        inserted == 1,
        "Expected to insert 1 record (inserted %d)",
        Integer.valueOf(inserted)
      );

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, time)
          .set(AUDIT.TYPE, "IMAGE_CREATED")
          .set(AUDIT.USER_ID, owner)
          .set(AUDIT.MESSAGE, id.toString())
          .set(AUDIT.CONFIDENTIAL, Boolean.FALSE);

      insertAuditRecord(audit);

      return new EIImage(
        id,
        new EICreation(owner, time),
        Optional.empty(),
        hash
      );
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    }
  }

  @Override
  public Optional<EIImage> imageGet(
    final UUID id,
    final EIServerDatabaseIncludeRedacted includeRedacted)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(includeRedacted, "includeRedacted");

    final var context = this.transaction().createContext();

    try {
      final var imageOpt =
        context.fetchOptional(IMAGES, IMAGES.ID.eq(id));
      final var imageRedactionOpt =
        context.fetchOptional(IMAGE_REDACTIONS, IMAGE_REDACTIONS.IMAGE.eq(id));

      if (imageRedactionOpt.isPresent()) {
        if (includeRedacted == EXCLUDE_REDACTED) {
          return Optional.empty();
        }
      }

      return imageOpt.map(ir -> {
        return new EIImage(
          id,
          new EICreation(ir.getCreator(), ir.getCreated()),
          imageRedactionOpt.map(re -> {
            return new EIRedaction(
              re.getCreator(),
              re.getCreated(),
              re.getReason());
          }),
          new EIHash(ir.getHashAlgo(), ir.getHashValue())
        );
      });
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);

    }
  }

  @Override
  @EIServerDatabaseRequiresUser
  public void imageRedact(
    final UUID imageId,
    final Optional<EIRedactionRequest> request)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(imageId, "imageId");
    Objects.requireNonNull(request, "request");

    final var owner =
      this.transaction().userId();
    final var context =
      this.transaction().createContext();

    try {
      if (this.imageGet(imageId, INCLUDE_REDACTED).isEmpty()) {
        throw new EIServerDatabaseException(
          "Image does not exist",
          IMAGE_NONEXISTENT
        );
      }

      /*
       * Delete any existing redaction. If there is one, we're either going
       * to replace it or completely remove it.
       */

      context.delete(IMAGE_REDACTIONS)
        .where(IMAGE_REDACTIONS.IMAGE.eq(imageId))
        .execute();

      final var time = this.currentTime();

      /*
       * Create a new redaction if required.
       */

      if (request.isPresent()) {
        final var redact = request.get();

        final var inserted =
          context.insertInto(IMAGE_REDACTIONS)
            .set(IMAGE_REDACTIONS.IMAGE, imageId)
            .set(IMAGE_REDACTIONS.REASON, redact.reason())
            .set(IMAGE_REDACTIONS.CREATOR, owner)
            .set(IMAGE_REDACTIONS.CREATED, redact.created())
            .execute();

        Preconditions.checkPreconditionV(
          inserted == 1,
          "Expected to insert 1 record (inserted %d)",
          Integer.valueOf(inserted)
        );
      }

      /*
       * Log the redaction.
       */

      final var redactType =
        request.isPresent() ? "IMAGE_REDACTED" : "IMAGE_UNREDACTED";

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, time)
          .set(AUDIT.TYPE, redactType)
          .set(AUDIT.USER_ID, owner)
          .set(AUDIT.MESSAGE, imageId.toString())
          .set(AUDIT.CONFIDENTIAL, Boolean.FALSE);

      insertAuditRecord(audit);
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    }
  }

  private EIUser fetchUserOrFail(
    final UUID creator)
    throws EIServerDatabaseException
  {
    return this.users.userGet(creator).orElseThrow(() -> {
      return new EIServerDatabaseException(
        String.format("User with ID %s does not exist", creator),
        USER_NONEXISTENT
      );
    });
  }
}
