package org.opendatakit.briefcase.ui.reused;

import java.awt.Cursor;

import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.ui.MessageStrings;
import org.opendatakit.briefcase.ui.ODKOptionPane;
import org.opendatakit.briefcase.util.ServerConnectionTest;

public class AggregateServerConnectionDialog {
  final AggregateServerConnectionDialogForm form;
  private boolean asTarget;
  private boolean isSuccessful;
  private ServerConnectionInfo serverInfo = null;
  private TerminationFuture terminationFuture = new TerminationFuture();

  AggregateServerConnectionDialog(AggregateServerConnectionDialogForm aggregateServerConnectionDialogForm, boolean asTarget) {
    form = aggregateServerConnectionDialogForm;
    this.asTarget = asTarget;

    form.addConnectButtonCallback(e -> verifyParams());
  }

  private void verifyParams() {
    final ServerConnectionInfo info = new ServerConnectionInfo((form.getURLFieldText()).trim(),
        form.getUsernameFieldText(), form.getPasswordFieldPassword());

    form.disableConnectButton();
    form.disableCancelButton();
    String errorString;

    Cursor saved = form.getCursor();
    try {
      form.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      form.paint(form.getGraphics());
      terminationFuture.reset();
      ServerConnectionTest backgroundAction = new ServerConnectionTest(info, terminationFuture, asTarget);

      backgroundAction.run();
      isSuccessful = backgroundAction.isSuccessful();
      errorString = backgroundAction.getErrorReason();
    } finally {
      form.setCursor(saved);
    }

    if (isSuccessful) {
      serverInfo = info;
      form.setVisible(false);
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

  public AggregateServerConnectionDialogForm getForm() {
    return form;
  }
}
