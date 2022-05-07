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


package com.io7m.eigion.gui.internal.views;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.MultipleSelectionModel;

/**
 * A selection model that effectively disables selections.
 *
 * @param <T> The type of returned items (unused)
 */

public final class EIGNoSelectionModel<T> extends MultipleSelectionModel<T>
{
  /**
   * A selection model that effectively disables selections.
   */

  public EIGNoSelectionModel()
  {

  }

  @Override
  public ObservableList<Integer> getSelectedIndices()
  {
    return FXCollections.emptyObservableList();
  }

  @Override
  public ObservableList<T> getSelectedItems()
  {
    return FXCollections.emptyObservableList();
  }

  @Override
  public void selectIndices(
    final int index,
    final int... indices)
  {

  }

  @Override
  public void selectAll()
  {

  }

  @Override
  public void selectFirst()
  {

  }

  @Override
  public void selectLast()
  {

  }

  @Override
  public void clearAndSelect(final int index)
  {

  }

  @Override
  public void select(final int index)
  {

  }

  @Override
  public void select(final T obj)
  {

  }

  @Override
  public void clearSelection(final int index)
  {

  }

  @Override
  public void clearSelection()
  {

  }

  @Override
  public boolean isSelected(
    final int index)
  {
    return false;
  }

  @Override
  public boolean isEmpty()
  {
    return true;
  }

  @Override
  public void selectPrevious()
  {

  }

  @Override
  public void selectNext()
  {

  }
}
