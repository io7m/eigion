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


package com.io7m.eigion.server.internal.pike;

import com.io7m.eigion.error_codes.EIException;
import com.io7m.eigion.model.EIValidityException;
import com.io7m.eigion.protocol.api.EIProtocolException;
import com.io7m.eigion.protocol.api.EIProtocolMessageType;
import com.io7m.eigion.protocol.pike.EIPResponseType;
import com.io7m.eigion.server.database.api.EISDatabaseException;
import com.io7m.eigion.server.internal.command_exec.EISCommandExecutionFailure;
import com.io7m.eigion.server.internal.command_exec.EISCommandExecutorType;
import com.io7m.eigion.server.internal.security.EISecurityException;

import java.io.IOException;
import java.util.Objects;

/**
 * The abstract base command class.
 *
 * @param <C> The type of accepted commands
 */

public abstract class EISPCmdAbstract<C extends EIProtocolMessageType> implements
  EISCommandExecutorType<EISPCommandContext, C, EIPResponseType>
{
  protected EISPCmdAbstract()
  {

  }

  @Override
  public final EIPResponseType execute(
    final EISPCommandContext context,
    final C command)
    throws EISCommandExecutionFailure, IOException, InterruptedException
  {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(command, "command");

    try {
      return this.executeActual(context, command);
    } catch (final EIValidityException e) {
      throw context.failValidity(e);
    } catch (final EISDatabaseException e) {
      throw context.failDatabase(e);
    } catch (final EIProtocolException e) {
      throw context.failProtocol(e);
    } catch (final EISecurityException e) {
      throw context.failSecurity(e);
    } catch (final EIException e) {
      throw context.fail(500, e.errorCode(), e.getMessage());
    }
  }

  protected abstract EIPResponseType executeActual(
    EISPCommandContext context,
    C command)
    throws
    EIValidityException,
    EIException,
    EISCommandExecutionFailure,
    EISecurityException;
}
