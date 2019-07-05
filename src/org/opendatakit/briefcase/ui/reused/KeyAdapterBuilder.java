package org.opendatakit.briefcase.ui.reused;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

public class KeyAdapterBuilder {
  private Consumer<KeyEvent> keyTypedConsumer;
  private Consumer<KeyEvent> keyPressedConsumer;
  private Consumer<KeyEvent> keyReleasedConsumer;

  public KeyAdapterBuilder onKeyTyped(Consumer<KeyEvent> consumer) {
    keyTypedConsumer = consumer;
    return this;
  }

  public KeyAdapterBuilder onKeyPressed(Consumer<KeyEvent> consumer) {
    keyPressedConsumer = consumer;
    return this;
  }

  public KeyAdapterBuilder onKeyReleased(Consumer<KeyEvent> consumer) {
    keyReleasedConsumer = consumer;
    return this;
  }

  public KeyAdapter build() {
    return new KeyAdapter() {
      @Override
      public void keyTyped(KeyEvent e) {
        if (keyTypedConsumer != null)
          keyTypedConsumer.accept(e);

      }

      @Override
      public void keyPressed(KeyEvent e) {
        if (keyPressedConsumer != null)
          keyPressedConsumer.accept(e);
      }

      @Override
      public void keyReleased(KeyEvent e) {
        if (keyReleasedConsumer != null)
          keyReleasedConsumer.accept(e);
      }
    };
  }
}
