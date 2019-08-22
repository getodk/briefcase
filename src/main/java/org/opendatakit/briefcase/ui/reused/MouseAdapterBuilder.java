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
package org.opendatakit.briefcase.ui.reused;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class MouseAdapterBuilder {
  private Consumer<MouseEvent> onClickCallback = e -> {
  };

  public MouseAdapterBuilder onClick(Consumer<MouseEvent> callback) {
    onClickCallback = callback;
    return this;
  }

  public MouseAdapter build() {
    return new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        onClickCallback.accept(e);
      }
    };
  }
}
