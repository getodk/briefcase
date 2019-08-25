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
package org.opendatakit.briefcase.delivery.ui.reused;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentListener;
import javax.swing.JComponent;

public class NoOpAnalytics extends Analytics {

  private static final ComponentListener NO_OP_COMPONENT_LISTENER = new ComponentAdapter() {
  };

  public NoOpAnalytics() {
    super(null, null, null, null, null);
  }

  @Override
  public void enter(String screenName) {
    // Do nothing
  }

  @Override
  public void leave(String screenName) {
    // Do nothing
  }

  @Override
  public void event(String category, String action, String label, Integer value) {
    // Do nothing
  }

  @Override
  public ComponentListener buildComponentListener(String screenName) {
    return NO_OP_COMPONENT_LISTENER;
  }

  @Override
  public void enableTracking(boolean enabled) {
    // Do nothing
  }

  @Override
  public void register(JComponent component) {
    // Do nothing
  }
}
