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


package com.io7m.eigion.taskrecorder;

import org.slf4j.Logger;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A task consisting of a number of steps.
 *
 * @param <T> The type of values that the task will return on completion.
 */

public final class EITask<T> implements EIStepType
{
  private final Logger logger;
  private final String name;
  private final LinkedList<EIStepType> steps;
  private final List<EIStepType> stepsRead;
  private final UUID id;
  private Optional<T> result;

  private EITask(
    final Logger inLogger,
    final UUID inId,
    final String inName)
  {
    this.logger =
      Objects.requireNonNull(inLogger, "logger");
    this.id =
      Objects.requireNonNull(inId, "id");
    this.name =
      Objects.requireNonNull(inName, "name");

    this.steps = new LinkedList<>();
    this.steps.add(new EIStep(this.logger, this.name));
    this.stepsRead = Collections.unmodifiableList(this.steps);
    this.result = Optional.empty();
  }

  /**
   * Create a new task.
   *
   * @param logger The logger
   * @param name   The task name
   * @param <T>    The type of returned values
   *
   * @return A new task
   */

  public static <T> EITask<T> create(
    final Logger logger,
    final String name)
  {
    return new EITask<T>(logger, UUID.randomUUID(), name);
  }

  /**
   * Begin a new step.
   *
   * @param newName The step name
   */

  public void beginStep(
    final String newName)
  {
    this.logger.debug("begin step: {}", newName);
    this.steps.add(new EIStep(this.logger, newName));
  }

  /**
   * Begin a new subtask.
   *
   * @param newName The task name
   * @param <U>     The type of returned values
   *
   * @return The new task
   */

  public <U> EITask<U> beginSubtask(
    final String newName)
  {
    this.logger.debug("begin subtask: {}", newName);
    final var subtask = new EITask<U>(this.logger, this.id, newName);
    this.steps.add(subtask);
    return subtask;
  }

  @Override
  public void setSucceeded(
    final String message)
  {
    this.steps.peekLast().setSucceeded(message);
  }

  @Override
  public void setFailed(
    final String message,
    final Optional<Throwable> exception)
  {
    this.steps.peekLast().setFailed(message, exception);
  }

  @Override
  public EIResolutionType resolution()
  {
    return this.steps.peekLast().resolution();
  }

  @Override
  public String name()
  {
    return this.name;
  }

  /**
   * @return A read-only view of the current task steps
   */

  public List<EIStepType> steps()
  {
    return this.stepsRead;
  }

  /**
   * Set the task result.
   *
   * @param inResult The result
   */

  public void setResult(
    final T inResult)
  {
    this.result =
      Optional.of(Objects.requireNonNull(inResult, "result"));
  }

  /**
   * @return The result
   */

  public Optional<T> result()
  {
    return this.result;
  }
}
