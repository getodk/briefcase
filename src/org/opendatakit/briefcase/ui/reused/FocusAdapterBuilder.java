package org.opendatakit.briefcase.ui.reused;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.function.Consumer;

public class FocusAdapterBuilder {
  private Consumer<FocusEvent> focusGainedConsumer;
  private Consumer<FocusEvent> focusLostConsumer;

  public FocusAdapterBuilder onFocusGained(Consumer<FocusEvent> consumer) {
    focusGainedConsumer = consumer;
    return this;
  }

  public FocusAdapterBuilder onFocusLost(Consumer<FocusEvent> consumer) {
    focusLostConsumer = consumer;
    return this;
  }

  public FocusAdapter build() {
    return new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        if (focusGainedConsumer != null)
          focusGainedConsumer.accept(e);
      }

      @Override
      public void focusLost(FocusEvent e) {
        if (focusLostConsumer != null)
          focusLostConsumer.accept(e);
      }
    };
  }
}
