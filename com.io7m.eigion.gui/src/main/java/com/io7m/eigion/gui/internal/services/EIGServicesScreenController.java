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


package com.io7m.eigion.gui.internal.services;

import com.io7m.eigion.gui.internal.main.EIScreenControllerWithServicesType;
import com.io7m.eigion.services.api.EIServiceDirectoryType;
import com.io7m.eigion.services.api.EIServiceType;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * The controller for the services screen.
 */

public final class EIGServicesScreenController
  implements EIScreenControllerWithServicesType
{
  private final EIServiceList serviceList;
  private final EIServiceDirectoryType services;

  @FXML
  private TableView<EIServiceType> serviceTable;

  /**
   * The controller for the services screen.
   *
   * @param inServices The service directory
   */

  public EIGServicesScreenController(
    final EIServiceDirectoryType inServices)
  {
    this.services =
      Objects.requireNonNull(inServices, "services");
    this.serviceList =
      new EIServiceList(service -> true);
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    this.serviceList.setItems(
      List.copyOf(this.services.services())
    );

    final var tableColumns =
      this.serviceTable.getColumns();
    final var tableTypeColumn =
      (TableColumn<EIServiceType, EIServiceType>) tableColumns.get(0);
    final var tableNameColumn =
      (TableColumn<EIServiceType, EIServiceType>) tableColumns.get(1);
    final var tableDescriptionColumn =
      (TableColumn<EIServiceType, EIServiceType>) tableColumns.get(2);

    tableTypeColumn.setSortable(true);
    tableTypeColumn.setReorderable(false);
    tableTypeColumn.setCellFactory(
      column -> new EIServiceTableTypeCell());

    tableNameColumn.setSortable(true);
    tableNameColumn.setReorderable(false);
    tableNameColumn.setCellFactory(
      column -> new EIServiceTableNameCell());

    tableDescriptionColumn.setSortable(true);
    tableDescriptionColumn.setReorderable(false);
    tableDescriptionColumn.setCellFactory(
      column -> new EIServiceTableDescriptionCell());

    tableTypeColumn.setCellValueFactory(
      param -> new ReadOnlyObjectWrapper<>(param.getValue()));
    tableNameColumn.setCellValueFactory(
      param -> new ReadOnlyObjectWrapper<>(param.getValue()));
    tableDescriptionColumn.setCellValueFactory(
      param -> new ReadOnlyObjectWrapper<>(param.getValue()));

    this.serviceTable.setItems(this.serviceList.items());
  }
}
