package org.opendatakit.briefcase.ui.reused;

import java.util.function.Consumer;

import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.ui.MessageStrings;
import org.opendatakit.briefcase.ui.ODKOptionPane;
import org.opendatakit.briefcase.ui.export.AggregateServerConnectionConfiguration;
import org.opendatakit.briefcase.util.ServerConnectionTest;

public class AggregateServerConnectionDialog {
  final AggregateServerConnectionDialogForm form;
  private boolean asTarget;
  private boolean isSuccessful;
  private ServerConnectionInfo serverInfo = null;
  private TerminationFuture terminationFuture = new TerminationFuture();
  private AggregateServerConnectionConfiguration aggregateServerConnectionConfiguration;

  AggregateServerConnectionDialog(AggregateServerConnectionDialogForm aggregateServerConnectionDialogForm, boolean asTarget, AggregateServerConnectionConfiguration aggregateServerConnectionConfiguration) {
    form = aggregateServerConnectionDialogForm;
    this.asTarget = asTarget;
    this.aggregateServerConnectionConfiguration = aggregateServerConnectionConfiguration;
  }

  private void verifyParams() {
    final ServerConnectionInfo info = new ServerConnectionInfo((aggregateServerConnectionConfiguration.getUrl().toString()).trim(),
        aggregateServerConnectionConfiguration.getUsername(), aggregateServerConnectionConfiguration.getPassword());

    form.disableConnectButton();
    form.disableCancelButton();
    String errorString;

    form.setSavedCursor();
    try {
      form.setFormCursor();
      form.paint(form.getGraphics());
      terminationFuture.reset();
      ServerConnectionTest backgroundAction = new ServerConnectionTest(info, terminationFuture, asTarget);

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
    form.onConnect(() -> consumer.accept(aggregateServerConnectionConfiguration));
  }

  public AggregateServerConnectionDialogForm getForm() {
    return form;
  }
}
