/*
 * Copyright (C) 2018 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.briefcase.ui.reused.source;

import static org.opendatakit.briefcase.ui.ODKOptionPane.showErrorDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.SwingWorker;
import org.opendatakit.briefcase.reused.RemoteServer;
import org.opendatakit.briefcase.reused.http.Response;

public class RemoteServerDialog {
  final RemoteServerDialogForm form;
  private final List<Consumer<RemoteServer>> onConnectCallbacks = new ArrayList<>();

  private RemoteServerDialog(RemoteServerDialogForm form, RemoteServer.Test serverTester) {
    this.form = form;

    this.form.onConnect(server -> {
      form.setTestingConnection();

      new SwingWorker<Response<Boolean>, Void>() {
        @Override protected Response<Boolean> doInBackground() {
          return serverTester.test(server);
        }

        @Override protected void done() {
          try {
            Response<Boolean> response = get();
            if (response.isSuccess()) {
              triggerConnect(server);
              form.hideDialog();
            } else
              showError(
                  response.isRedirection() ? "Redirection detected" : response.isUnauthorized() ? "Wrong credentials" : response.isNotFound() ? "Aggregate not found" : "",
                  response.isRedirection() ? "Unexpected error" : "Configuration error"
              );
          } catch (InterruptedException ignore) {
            // Ignore
          } catch (ExecutionException e) {
            if (e.getCause() != null) {
              showError(e.getCause().getMessage(), "Unexpected error");
            }
          }
          form.unsetTestingConnection();
        }
      }.execute();
    });
  }

  private void showError(String error, String title) {
    String maybeSeparator = error.isEmpty() ? "" : ".\n\n";
    showErrorDialog(
        form,
        String.format(
            "%s%sPlease review the connection parameters and try again.",
            error,
            maybeSeparator
        ),
        title
    );
  }

  static RemoteServerDialog empty(RemoteServer.Test serverTester, String requiredPermission) {
    return new RemoteServerDialog(
        new RemoteServerDialogForm(requiredPermission),
        serverTester
    );
  }

  private void triggerConnect(RemoteServer conf) {
    onConnectCallbacks.forEach(callback -> callback.accept(conf));
  }

  void onConnect(Consumer<RemoteServer> consumer) {
    onConnectCallbacks.add(consumer);
  }

  public RemoteServerDialogForm getForm() {
    return form;
  }

}
