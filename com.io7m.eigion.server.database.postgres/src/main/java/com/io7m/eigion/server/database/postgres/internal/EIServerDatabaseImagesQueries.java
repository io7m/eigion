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

import com.io7m.eigion.model.EICreation;
import com.io7m.eigion.model.EIImage;
import com.io7m.eigion.model.EIRedaction;
import com.io7m.eigion.model.EIUser;
import com.io7m.eigion.server.database.api.EIServerDatabaseException;
import com.io7m.eigion.server.database.api.EIServerDatabaseImagesQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseIncludeRedacted;
import com.io7m.eigion.server.database.api.EIServerDatabaseUsersQueriesType;
import com.io7m.eigion.server.database.postgres.internal.tables.records.AuditRecord;
import com.io7m.jaffirm.core.Postconditions;
import com.io7m.jaffirm.core.Preconditions;
import org.jooq.InsertSetMoreStep;
import org.jooq.exception.DataAccessException;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.eigion.server.database.api.EIServerDatabaseIncludeRedacted.EXCLUDE_REDACTED;
import static com.io7m.eigion.server.database.api.EIServerDatabaseIncludeRedacted.INCLUDE_REDACTED;
import static com.io7m.eigion.server.database.postgres.internal.Tables.IMAGES;
import static com.io7m.eigion.server.database.postgres.internal.Tables.IMAGE_REDACTIONS;
import static com.io7m.eigion.server.database.postgres.internal.tables.Audit.AUDIT;

final class EIServerDatabaseImagesQueries
  implements EIServerDatabaseImagesQueriesType
{
  private final EIServerDatabaseTransaction transaction;
  private final EIServerDatabaseUsersQueriesType users;

  EIServerDatabaseImagesQueries(
    final EIServerDatabaseTransaction inTransaction,
    final EIServerDatabaseUsersQueriesType inUsers)
  {
    this.transaction =
      Objects.requireNonNull(inTransaction, "transaction");
    this.users =
      Objects.requireNonNull(inUsers, "users");
  }

  @Override
  public EIImage imageCreate(
    final UUID id,
    final UUID creator,
    final String contentType,
    final byte[] data)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(creator, "creator");
    Objects.requireNonNull(contentType, "contentType");
    Objects.requireNonNull(data, "data");

    final var context = this.transaction.createContext();

    try {
      if (this.imageGet(id, INCLUDE_REDACTED).isPresent()) {
        throw new EIServerDatabaseException(
          "Image already exists",
          "image-duplicate"
        );
      }

      this.fetchUserOrFail(creator);

      final var time =
        this.currentTime();

      final var inserted =
        context.insertInto(IMAGES)
          .set(IMAGES.ID, id)
          .set(IMAGES.CREATED, time)
          .set(IMAGES.CREATOR, creator)
          .set(IMAGES.CONTENT_TYPE, contentType)
          .set(IMAGES.IMAGE_DATA, data)
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
          .set(AUDIT.MESSAGE, id + ":" + creator);

      insertAuditRecord(audit);

      return new EIImage(
        id,
        contentType,
        new EICreation(creator, time),
        Optional.empty()
      );
    } catch (final DataAccessException e) {
      throw new EIServerDatabaseException(e.getMessage(), e, "sql-error");
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

    final var context = this.transaction.createContext();

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
          ir.getContentType(),
          new EICreation(ir.getCreator(), ir.getCreated()),
          imageRedactionOpt.map(re -> {
            return new EIRedaction(
              re.getCreator(),
              re.getCreated(),
              re.getReason());
          })
        );
      });
    } catch (final DataAccessException e) {
      throw new EIServerDatabaseException(e.getMessage(), e, "sql-error");
    }
  }

  @Override
  public void imageRedact(
    final UUID id,
    final Optional<EIRedaction> redacted)
    throws EIServerDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(redacted, "redacted");

    final var context = this.transaction.createContext();

    try {
      if (this.imageGet(id, INCLUDE_REDACTED).isEmpty()) {
        throw new EIServerDatabaseException(
          "Image does not exist",
          "image-nonexistent"
        );
      }

      /*
       * Delete any existing redaction. If there is one, we're either going
       * to replace it or completely remove it.
       */

      context.delete(IMAGE_REDACTIONS)
        .where(IMAGE_REDACTIONS.IMAGE.eq(id))
        .execute();

      final var time = this.currentTime();

      /*
       * Create a new redaction if required.
       */

      if (redacted.isPresent()) {
        final var redact = redacted.get();
        this.fetchUserOrFail(redact.creator());

        final var inserted =
          context.insertInto(IMAGE_REDACTIONS)
            .set(IMAGE_REDACTIONS.IMAGE, id)
            .set(IMAGE_REDACTIONS.REASON, redact.reason())
            .set(IMAGE_REDACTIONS.CREATOR, redact.creator())
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
        redacted.isPresent() ? "IMAGE_REDACTED" : "IMAGE_UNREDACTED";
      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, time)
          .set(AUDIT.TYPE, redactType)
          .set(AUDIT.MESSAGE, id.toString());

      insertAuditRecord(audit);
    } catch (final DataAccessException e) {
      throw new EIServerDatabaseException(e.getMessage(), e, "sql-error");
    }
  }

  private static void insertAuditRecord(
    final InsertSetMoreStep<AuditRecord> audit)
  {
    final var inserted = audit.execute();
    Postconditions.checkPostconditionV(
      inserted == 1,
      "Expected to insert one audit record (inserted %d)",
      Integer.valueOf(inserted)
    );
  }

  private EIUser fetchUserOrFail(
    final UUID creator)
    throws EIServerDatabaseException
  {
    return this.users.userGet(creator).orElseThrow(() -> {
      return new EIServerDatabaseException(
        String.format("User with ID %s does not exist", creator),
        "user-nonexistent"
      );
    });
  }

  private OffsetDateTime currentTime()
  {
    return OffsetDateTime.now(this.transaction.clock())
      .withNano(0);
  }
}
