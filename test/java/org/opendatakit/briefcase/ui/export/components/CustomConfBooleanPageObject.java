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
package org.opendatakit.briefcase.ui.export.components;

import static org.assertj.swing.edt.GuiActionRunner.execute;

import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.JFrame;
import javax.swing.JRadioButton;
import org.assertj.swing.core.Robot;
import org.assertj.swing.fixture.FrameFixture;

class CustomConfBooleanPageObject {
  private final CustomConfBooleanForm component;
  private final FrameFixture fixture;

  private CustomConfBooleanPageObject(CustomConfBooleanForm component, FrameFixture fixture) {
    this.component = component;
    this.fixture = fixture;
  }

  static CustomConfBooleanPageObject setUp(Robot robot, Optional<CustomConfBooleanForm.Value> initialValue) {
    CustomConfBooleanForm component = execute(() -> new CustomConfBooleanForm(initialValue));
    JFrame frame = execute(() -> {
      JFrame f = new JFrame();
      f.add(component.$$$getRootComponent$$$());
      return f;
    });
    FrameFixture fixture = new FrameFixture(robot, frame);
    return new CustomConfBooleanPageObject(component, fixture);
  }

  void show() {
    fixture.show();
  }

  public void onChange(Consumer<CustomConfBooleanForm.Value> callback) {
    component.onChange(callback);
  }

  JRadioButton inherit() {
    return component.inherit;
  }

  JRadioButton yes() {
    return component.yes;
  }

  JRadioButton no() {
    return component.no;
  }

  void set(CustomConfBooleanForm.Value value) {
    execute(() -> {
      component.set(value);
      component.setInternal(value);
    });
  }

  private JRadioButton getRadioButton(CustomConfBooleanForm.Value value) {
    switch (value) {
      case INHERIT:
        return component.inherit;
      case YES:
        return component.yes;
      case NO:
        return component.no;
      default:
        throw new IllegalArgumentException("Unknown radio button for " + value);
    }
  }
}
