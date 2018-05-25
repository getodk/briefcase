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
import java.util.function.Consumer;
import org.opendatakit.briefcase.reused.RemoteServer;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.http.Response;

public class RemoteServerDialog {
  private final RemoteServerDialogForm form;
  private final List<Consumer<RemoteServer>> onConnectCallbacks = new ArrayList<>();

  private RemoteServerDialog(RemoteServerDialogForm form, Http http) {
    this.form = form;

    this.form.onConnect(server -> {
      Response<Boolean> response = server.testPull(http);
      if (response.isSuccess()) {
        triggerConnect(server);
        form.hideDialog();
      } else {
        String error = response.isRedirection()
            ? "Redirection detected.\n\nPlease, review the connection parameters and try again."
            : response.isUnauthorized()
            ? "Wrong credentials.\n\nPlease, review the connection parameters and try again."
            : response.isNotFound()
            ? "Aggregate not found.\n\nPlease, review the connection parameters and try again."
            : "Please, review the connection parameters and try again.";
        showErrorDialog(form, error, "Wrong connection parameters");
      }
    });
  }

  static RemoteServerDialog empty(Http http) {
    return new RemoteServerDialog(
        new RemoteServerDialogForm(),
        http
    );
  }

  private void triggerConnect(RemoteServer conf) {
    onConnectCallbacks.forEach(callback -> callback.accept(conf));
  }

  public void onConnect(Consumer<RemoteServer> consumer) {
    onConnectCallbacks.add(consumer);
  }

  public RemoteServerDialogForm getForm() {
    return form;
  }

}
