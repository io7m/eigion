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

package com.io7m.eigion.server.vanilla.internal.public_api;

import com.io7m.eigion.hash.EIHash;
import com.io7m.eigion.protocol.public_api.v1.EISP1ResponseImageCreated;
import com.io7m.eigion.server.database.api.EIServerDatabaseImagesQueriesType;
import com.io7m.eigion.server.database.api.EIServerDatabaseType;
import com.io7m.eigion.server.security.EISecPolicyResultDenied;
import com.io7m.eigion.server.security.EISecUserActionImageCreate;
import com.io7m.eigion.server.security.EISecurity;
import com.io7m.eigion.server.vanilla.internal.EIHTTPErrorStatusException;
import com.io7m.eigion.server.vanilla.internal.EIRequestLimits;
import com.io7m.eigion.server.vanilla.internal.EIServerImageStorage;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import com.io7m.eigion.storage.api.EIStorageName;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

import static com.io7m.eigion.error_codes.EIStandardErrorCodes.IMAGE_INVALID;
import static com.io7m.eigion.error_codes.EIStandardErrorCodes.SECURITY_POLICY_DENIED;
import static com.io7m.eigion.server.database.api.EIServerDatabaseRole.EIGION;
import static com.io7m.eigion.server.vanilla.internal.EIServerRequestDecoration.requestIdFor;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.FORBIDDEN_403;

/**
 * A servlet for creating images.
 */

public final class EIPImageCreate extends EIPAuthenticatedServlet
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EIPImageCreate.class);

  private final EIServerDatabaseType database;
  private final EIRequestLimits limits;
  private final EIServerImageStorage imageStorage;

  /**
   * A servlet for creating images.
   *
   * @param services The service directory
   */

  public EIPImageCreate(
    final EIServiceDirectoryType services)
  {
    super(services);

    this.database =
      services.requireService(EIServerDatabaseType.class);
    this.imageStorage =
      services.requireService(EIServerImageStorage.class);
    this.limits =
      services.requireService(EIRequestLimits.class);
  }

  @Override
  protected Logger logger()
  {
    return LOG;
  }

  @Override
  protected void serviceAuthenticated(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final HttpSession session)
    throws Exception
  {
    if (EISecurity.check(new EISecUserActionImageCreate(this.user()))
      instanceof EISecPolicyResultDenied denied) {
      throw new EIHTTPErrorStatusException(
        FORBIDDEN_403,
        SECURITY_POLICY_DENIED,
        denied.message()
      );
    }

    /*
     * Read the image data. Image data is bounded to a sensible maximum
     * to avoid resource exhaustion attacks.
     */

    final var imageData = this.readImageData(request);

    /*
     * Check that the image data can actually be parsed as an image.
     */

    this.checkImage(imageData);

    /*
     * Attempt to store the image.
     */

    final var imageId =
      UUID.randomUUID();
    final var hash =
      EIHash.sha256Of(imageData);

    final var imageName =
      new EIStorageName("/images/%s.jpg".formatted(imageId));

    this.imageStorage.put(
      imageName,
      "image/jpeg",
      hash,
      new ByteArrayInputStream(imageData)
    );

    /*
     * Record that the image was stored. If this fails, the image will
     * be left in the image storage unreferenced.
     */

    try (var connection =
           this.database.openConnection(EIGION)) {
      try (var transaction =
             connection.openTransaction()) {
        transaction.userIdSet(this.userId());

        final var images =
          transaction.queries(EIServerDatabaseImagesQueriesType.class);

        images.imageCreate(imageId, hash);
        transaction.commit();

        this.sends()
          .send(
            servletResponse,
            200,
            new EISP1ResponseImageCreated(requestIdFor(request), imageId)
          );
      }
    }
  }

  private byte[] readImageData(
    final HttpServletRequest request)
    throws EIHTTPErrorStatusException, IOException
  {
    try (var input =
           this.limits.boundedMaximumInput(request, 500_000)) {
      return input.readAllBytes();
    }
  }

  private void checkImage(
    final byte[] data)
    throws EIHTTPErrorStatusException
  {
    final var readers =
      ImageIO.getImageReadersByFormatName("jpg");

    while (readers.hasNext()) {
      final var reader = readers.next();

      try (var input =
             ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
        reader.setInput(input);
        reader.read(0);
        return;
      } catch (final IOException e) {
        // Ignore. This means the image cannot be read.
      }
    }

    throw new EIHTTPErrorStatusException(
      BAD_REQUEST_400,
      IMAGE_INVALID,
      this.strings().format("invalidImage")
    );
  }
}
