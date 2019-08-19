/*
 * Copyright (C) 2019 Nafundi
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

package org.opendatakit.briefcase.ui.reused.transfer.sourcetarget;

import static org.opendatakit.briefcase.ui.reused.UI.errorMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.SwingWorker;
import org.opendatakit.briefcase.reused.http.response.Response;
import org.opendatakit.briefcase.reused.transfer.CentralServer;
import org.opendatakit.briefcase.reused.transfer.RemoteServer.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CentralServerDialog {
  private static final Logger log = LoggerFactory.getLogger(CentralServerDialog.class);
  final CentralServerDialogForm form;
  private final List<Consumer<CentralServer>> onConnectCallbacks = new ArrayList<>();

  private CentralServerDialog(CentralServerDialogForm form, Test<CentralServer> serverTester) {
    this.form = form;

    this.form.onConnect(server -> {
      form.setTestingConnection();

      new SwingWorker<Response, Void>() {
        @Override
        protected Response doInBackground() {
          return serverTester.test(server);
        }

        @Override
        protected void done() {
          try {
            Response response = get();
            if (response.isSuccess()) {
              triggerConnect(server);
              form.hideDialog();
            } else {
              System.out.println(response.getStatusPhrase());
              showErrorMessage(
                  response.isRedirection() ? "Redirection detected" : response.isUnauthorized() ? "Wrong credentials" : response.isNotFound() ? "Project ID does not exist" : "",
                  response.isRedirection() ? "Unexpected error" : "Configuration error"
              );
            }
          } catch (InterruptedException ignore) {
            // Ignore
          } catch (ExecutionException e) {
            log.error("Unexpected error while testing the connection settings", e);
            if (e.getCause() != null) {
              showErrorMessage(e.getCause().getMessage(), "Unexpected error");
            }
          }
          form.unsetTestingConnection();
        }
      }.execute();
    });
  }

  public static CentralServerDialog empty(Test<CentralServer> serverTester) {
    return new CentralServerDialog(
        new CentralServerDialogForm(),
        serverTester
    );
  }

  private void showErrorMessage(String error, String title) {
    String maybeSeparator = error.isEmpty() ? "" : ".\n\n";
    errorMessage(title, String.format(
        "%s%sPlease review the connection parameters and try again.",
        error,
        maybeSeparator
    ));
  }

  private void triggerConnect(CentralServer server) {
    onConnectCallbacks.forEach(callback -> callback.accept(server));
  }

  public void onConnect(Consumer<CentralServer> consumer) {
    onConnectCallbacks.add(consumer);
  }

  public CentralServerDialogForm getForm() {
    return form;
  }

}
