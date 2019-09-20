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
package org.opendatakit.briefcase.delivery.ui.export;

import javax.swing.JButton;
import javax.swing.JFrame;
import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.opendatakit.briefcase.delivery.ui.reused.NoOpAnalytics;
import org.opendatakit.briefcase.reused.ContainerHelper;

// TODO Adapt to new UI behavior
class ExportPanelPageObject {
  private final ExportPanel component;
  private final FrameFixture fixture;

  private ExportPanelPageObject(ExportPanel component, FrameFixture fixture) {
    this.component = component;
    this.fixture = fixture;
  }

  static ExportPanelPageObject setUp(Robot robot) {
    ExportPanel exportPanel = GuiActionRunner.execute(() -> {
      ExportPanel ep = ExportPanel.from(
          ContainerHelper.inMemory(),
          new NoOpAnalytics()
      );
      ep.updateForms();
      return ep;
    });
    JFrame testFrame = GuiActionRunner.execute(() -> {
      JFrame f = new JFrame();
      f.add(exportPanel.getPanel().getContainer());
      return f;
    });
    FrameFixture window = new FrameFixture(robot, testFrame);
    return new ExportPanelPageObject(exportPanel, window);
  }

  void show() {
    fixture.show();
  }

  JButton exportButton() {
    return component.getPanel().exportButton;
  }
}
