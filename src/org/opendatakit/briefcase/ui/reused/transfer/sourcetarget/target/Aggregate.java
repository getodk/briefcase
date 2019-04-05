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

package org.opendatakit.briefcase.ui.reused.transfer.sourcetarget.target;

import static java.awt.Cursor.HAND_CURSOR;
import static java.awt.Cursor.getPredefinedCursor;
import static java.awt.Desktop.getDesktop;
import static javax.swing.SwingUtilities.invokeLater;
import static org.opendatakit.briefcase.ui.reused.UI.removeAllMouseListeners;

import java.awt.Container;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.function.Consumer;
import javax.swing.JLabel;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.RemoteServer;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.transfer.TransferForms;
import org.opendatakit.briefcase.ui.reused.MouseAdapterBuilder;
import org.opendatakit.briefcase.ui.reused.transfer.sourcetarget.RemoteServerDialog;
import org.opendatakit.briefcase.util.TransferAction;

public class Aggregate implements PushTarget<RemoteServer> {
  private final Http http;
  private final Consumer<PushTarget> consumer;
  private RemoteServer.Test serverTester;
  private String requiredPermission;
  private RemoteServer server;

  Aggregate(Http http, RemoteServer.Test serverTester, String requiredPermission, Consumer<PushTarget> consumer) {
    this.http = http;
    this.serverTester = serverTester;
    this.requiredPermission = requiredPermission;
    this.consumer = consumer;
  }

  static void clearPreferences(BriefcasePreferences prefs) {
    prefs.removeAll(RemoteServer.PREFERENCE_KEYS);
  }

  private static void uncheckedBrowse(URL url) {
    try {
      getDesktop().browse(url.toURI());
    } catch (URISyntaxException | IOException e) {
      throw new BriefcaseException(e);
    }
  }

  @Override
  public void onSelect(Container ignored) {
    RemoteServerDialog dialog = RemoteServerDialog.empty(serverTester, requiredPermission);
    dialog.onConnect(this::set);
    dialog.getForm().setVisible(true);
  }

  @Override
  public void set(RemoteServer server) {
    this.server = server;
    consumer.accept(this);
  }

  @Override
  public boolean accepts(Object o) {
    return o instanceof RemoteServer;
  }

  @Override
  public void storePreferences(BriefcasePreferences prefs, boolean storePasswords) {
    server.storePreferences(prefs, storePasswords);
  }

  @Override
  public void push(TransferForms forms, TerminationFuture terminationFuture) {
    TransferAction.transferBriefcaseToServer(server.asServerConnectionInfo(), terminationFuture, forms, http, server);
  }

  @Override
  public String getDescription() {
    return server.getBaseUrl().toString();
  }

  @Override
  public void decorate(JLabel label) {
    label.setText("<html><a href=\"" + server.getBaseUrl().toString() + "\">" + getDescription() + "</a></html>");
    label.setCursor(getPredefinedCursor(HAND_CURSOR));
    removeAllMouseListeners(label);
    label.addMouseListener(new MouseAdapterBuilder()
        .onClick(__ -> invokeLater(() -> uncheckedBrowse(server.getBaseUrl())))
        .build());
  }

  @Override
  public boolean canBeReloaded() {
    return false;
  }

  @Override
  public String toString() {
    return "Aggregate server";
  }
}
