package org.opendatakit.briefcase.delivery.ui.reused.events;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

public class KeyAdapterBuilder {
  private Consumer<KeyEvent> keyReleasedConsumer;

  public KeyAdapterBuilder onKeyReleased(Consumer<KeyEvent> consumer) {
    keyReleasedConsumer = consumer;
    return this;
  }

  public KeyAdapter build() {
    return new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        if (keyReleasedConsumer != null)
          keyReleasedConsumer.accept(e);
      }
    };
  }
}
