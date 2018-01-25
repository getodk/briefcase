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

import java.awt.event.ActionEvent;
import java.util.Arrays;

class FakeConfigurationDialogForm extends ConfigurationDialogForm {
  boolean okEnabled;
  boolean removeEnabled;

  FakeConfigurationDialogForm(ConfigurationPanelForm form) {
    super(form);
  }

  void clickOK() {
    Arrays.asList(okButton.getActionListeners()).forEach(al -> al.actionPerformed(new ActionEvent(this, 1, "something")));
  }

  void clickRemove() {
    Arrays.asList(clearAllButton.getActionListeners()).forEach(al -> al.actionPerformed(new ActionEvent(this, 1, "something")));
  }

  @Override
  public void enableOK() {
    okEnabled = true;
  }

  @Override
  public void disableOK() {
    okEnabled = false;
  }

  @Override
  public void enableClearAll() {
    removeEnabled = true;
  }
}
