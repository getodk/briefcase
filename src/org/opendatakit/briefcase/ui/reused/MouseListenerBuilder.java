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

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.function.Consumer;

public class MouseListenerBuilder {
  private static final Consumer<MouseEvent> NOP = e -> {
  };
  private Consumer<MouseEvent> onClickCallback = NOP;
  private Consumer<MouseEvent> onPressedCallback = NOP;
  private Consumer<MouseEvent> onRelesedCallback = NOP;
  private Consumer<MouseEvent> onEnteredCallback = NOP;
  private Consumer<MouseEvent> onExitedCallback = NOP;

  public MouseListenerBuilder onClick(Consumer<MouseEvent> callback) {
    onClickCallback = callback;
    return this;
  }

  public MouseListener build() {
    return new MouseListener() {
      @Override
      public void mouseClicked(MouseEvent e) {
        onClickCallback.accept(e);
      }

      @Override
      public void mousePressed(MouseEvent e) {
        onPressedCallback.accept(e);
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        onRelesedCallback.accept(e);
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        onEnteredCallback.accept(e);

      }

      @Override
      public void mouseExited(MouseEvent e) {
        onExitedCallback.accept(e);
      }
    };
  }
}
