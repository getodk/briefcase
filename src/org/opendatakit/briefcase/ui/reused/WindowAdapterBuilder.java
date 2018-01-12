package org.opendatakit.briefcase.ui.reused;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Optional;
import java.util.function.Consumer;

public class WindowAdapterBuilder {
  private Optional<Consumer<WindowEvent>> onWindowClosing = Optional.empty();
  private Optional<Consumer<WindowEvent>> onWindowOpened = Optional.empty();
  private Optional<Consumer<WindowEvent>> onWindowClosed = Optional.empty();
  private Optional<Consumer<WindowEvent>> onWindowIconified = Optional.empty();
  private Optional<Consumer<WindowEvent>> onWindowDeiconified = Optional.empty();
  private Optional<Consumer<WindowEvent>> onWindowActivated = Optional.empty();
  private Optional<Consumer<WindowEvent>> onWindowDeactivated = Optional.empty();

  public WindowAdapterBuilder onClosing(Consumer<WindowEvent> callback) {
    this.onWindowClosing = Optional.ofNullable(callback);
    return this;
  }

  public WindowListener build() {
    return new WindowListener() {
      @Override
      public void windowOpened(WindowEvent e) {
        onWindowOpened.ifPresent(c -> c.accept(e));
      }

      @Override
      public void windowClosing(WindowEvent e) {
        onWindowClosing.ifPresent(c -> c.accept(e));
      }

      @Override
      public void windowClosed(WindowEvent e) {
        onWindowClosed.ifPresent(c -> c.accept(e));

      }

      @Override
      public void windowIconified(WindowEvent e) {
        onWindowIconified.ifPresent(c -> c.accept(e));

      }

      @Override
      public void windowDeiconified(WindowEvent e) {
        onWindowDeiconified.ifPresent(c -> c.accept(e));

      }

      @Override
      public void windowActivated(WindowEvent e) {
        onWindowActivated.ifPresent(c -> c.accept(e));

      }

      @Override
      public void windowDeactivated(WindowEvent e) {
        onWindowDeactivated.ifPresent(c -> c.accept(e));
      }
    };
  }
}
