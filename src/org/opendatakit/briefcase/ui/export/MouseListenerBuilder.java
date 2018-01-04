package org.opendatakit.briefcase.ui.export;

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
