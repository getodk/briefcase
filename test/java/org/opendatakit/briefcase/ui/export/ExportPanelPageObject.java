/*
 * Copyright (C) 2018 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.briefcase.ui.export;

import static org.opendatakit.briefcase.ui.export.components.FormsTableView.SELECTED_CHECKBOX_COL;

import java.nio.file.Paths;
import javax.swing.JButton;
import javax.swing.JFrame;
import org.assertj.swing.core.MouseButton;
import org.assertj.swing.core.Robot;
import org.assertj.swing.data.TableCell;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.InMemoryPreferences;
import org.opendatakit.briefcase.model.TerminationFuture;

class ExportPanelPageObject {
  private final ExportPanel component;
  private final FrameFixture fixture;

  private ExportPanelPageObject(ExportPanel component, FrameFixture fixture) {
    this.component = component;
    this.fixture = fixture;
  }

  static ExportPanelPageObject setUp(Robot robot) {
    ExportPanel exportPanel = GuiActionRunner.execute(() -> {
      ExportPanel ep = ExportPanel.from(new TerminationFuture(), new BriefcasePreferences(InMemoryPreferences.empty()), new BriefcasePreferences(InMemoryPreferences.empty()), Runnable::run);
      ep.updateForms();
      return ep;
    });
    JFrame testFrame = GuiActionRunner.execute(() -> {
      JFrame f = new JFrame();
      f.add(exportPanel.getForm().getContainer());
      return f;
    });
    FrameFixture window = new FrameFixture(robot, testFrame);
    return new ExportPanelPageObject(exportPanel, window);
  }

  void show() {
    fixture.show();
  }

  void setExportDirectory(String value) {
    GuiActionRunner.execute(() -> component.getForm().getConfPanel().getForm().setExportDir(Paths.get(value)));
  }

  void selectFormATRow(int row) {
    TableCell cell = TableCell.row(row).column(SELECTED_CHECKBOX_COL);
    if (fixture.table("forms").cell(cell).value().equals("false"))
      fixture.table("forms").click(cell, MouseButton.LEFT_BUTTON);
  }

  JButton exportButton() {
    return component.getForm().exportButton;
  }
}
