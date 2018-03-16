package org.opendatakit.briefcase.ui.reused;

import static org.opendatakit.briefcase.ui.ODKOptionPane.showErrorDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.opendatakit.briefcase.ui.MessageStrings;

public class AggregateServerConnectionDialog {
  private final AggregateServerConnectionDialogForm form;
  private final List<Consumer<AggregateServerConnectionConfiguration>> onConnectCallbacks = new ArrayList<>();

  AggregateServerConnectionDialog(AggregateServerConnectionDialogForm form, Predicate<AggregateServerConnectionConfiguration> confValidator, AggregateServerConnectionConfiguration initialConfiguration) {
    this.form = form;

    this.form.onConnect(conf -> {
      if (confValidator.test(conf)) {
        triggerConnect(conf);
        form.hideDialog();
      } else {
        // Show an error
        showErrorDialog(form, MessageStrings.PROXY_SET_ADVICE, "Wrong connection parameters");
        form.enableUI();
      }
    });
  }

  private void triggerConnect(AggregateServerConnectionConfiguration conf) {
    onConnectCallbacks.forEach(callback -> callback.accept(conf));
  }

  void onConnect(Consumer<AggregateServerConnectionConfiguration> consumer) {
    onConnectCallbacks.add(consumer);
  }

  public AggregateServerConnectionDialogForm getForm() {
    return form;
  }
}
