package org.opendatakit.briefcase.ui.reused;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.ui.MessageStrings;
import org.opendatakit.briefcase.ui.ODKOptionPane;
import org.opendatakit.briefcase.util.ServerConnectionTest;

public class AggregateServerConnectionDialog {
  final AggregateServerConnectionDialogForm form;
  private boolean isSuccessful;
  private ServerConnectionInfo serverInfo = null;
  private TerminationFuture terminationFuture = new TerminationFuture();
  private AggregateServerConnectionConfiguration configuration;
  Predicate<AggregateServerConnectionConfiguration> confValidator;

  AggregateServerConnectionDialog(AggregateServerConnectionDialogForm form, Predicate<AggregateServerConnectionConfiguration> confValidator, AggregateServerConnectionConfiguration configuration) {
    this.form = form;
    this.configuration = configuration;
    this.confValidator = confValidator;
  }

  private void verifyParams() {
    final ServerConnectionInfo info = new ServerConnectionInfo((configuration.getUrl().toString()).trim(),
        configuration.getUsername(), configuration.getPassword().toCharArray());

    form.disableConnectButton();
    form.disableCancelButton();
    String errorString;

    form.setSavedCursor();
    try {
      form.setFormCursor();
      form.paint(form.getGraphics());
      terminationFuture.reset();
      // asTarget will be handled in the predicate
      ServerConnectionTest2 backgroundAction = new ServerConnectionTest2(info, true);

      backgroundAction.run();
      isSuccessful = backgroundAction.isSuccessful();
      errorString = backgroundAction.getErrorReason();
    } finally {
      form.setFormCursorAsSavedCursor();
    }

    if (isSuccessful) {
      serverInfo = info;
      form.hideForm();
    } else {
      String errorMessage = errorString.trim();

      if (!(errorMessage.endsWith("!") || errorMessage.endsWith("."))) {
        errorMessage += ". ";
      }

      ODKOptionPane.showErrorDialog(form,
          errorMessage + MessageStrings.PROXY_SET_ADVICE, "Invalid Server URL");
      form.enableConnectButton();
      form.enableCancelButton();
    }

  }

  void onOK(Consumer<AggregateServerConnectionConfiguration> consumer) {
    if(confValidator.test(configuration))
      form.onConnect(() -> consumer.accept(configuration));
    else {
      ODKOptionPane.showErrorDialog(form, MessageStrings.PROXY_SET_ADVICE, "Wrong connection parameters");
      form.enableConnectButton();
      form.enableCancelButton();
    }
  }

  public AggregateServerConnectionDialogForm getForm() {
    return form;
  }
}
