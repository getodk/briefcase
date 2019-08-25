package org.opendatakit.briefcase.delivery.ui.reused.events;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.function.Consumer;

public class FocusAdapterBuilder {
  private Consumer<FocusEvent> focusLostConsumer;

  public FocusAdapterBuilder onFocusLost(Consumer<FocusEvent> consumer) {
    focusLostConsumer = consumer;
    return this;
  }

  public FocusAdapter build() {
    return new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        if (focusLostConsumer != null)
          focusLostConsumer.accept(e);
      }
    };
  }
}
