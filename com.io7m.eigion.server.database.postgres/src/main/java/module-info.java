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

import com.io7m.eigion.server.database.api.EISDatabaseFactoryType;
import com.io7m.eigion.server.database.postgres.EISDatabases;

/**
 * Eigion platform (Server PostgreSQL database)
 */

module com.io7m.eigion.server.database.postgres
{
  requires static org.osgi.annotation.versioning;
  requires static org.osgi.annotation.bundle;

  requires transitive com.io7m.eigion.error_codes;
  requires transitive com.io7m.eigion.model;
  requires transitive com.io7m.eigion.services.api;
  requires transitive io.opentelemetry.api;

  requires com.io7m.anethum.common;
  requires com.io7m.eigion.server.database.api;
  requires com.io7m.jdeferthrow.core;
  requires com.io7m.jqpage.core;
  requires com.io7m.trasco.api;
  requires com.io7m.trasco.vanilla;
  requires com.zaxxer.hikari;
  requires io.opentelemetry.context;
  requires io.opentelemetry.semconv;
  requires org.jooq;
  requires org.postgresql.jdbc;
  requires org.slf4j;

  provides EISDatabaseFactoryType
    with EISDatabases;

  exports com.io7m.eigion.server.database.postgres;
  exports com.io7m.eigion.server.database.postgres.internal.tables to org.jooq;
  exports com.io7m.eigion.server.database.postgres.internal.tables.records to org.jooq;
  exports com.io7m.eigion.server.database.postgres.internal to org.jooq;
}
