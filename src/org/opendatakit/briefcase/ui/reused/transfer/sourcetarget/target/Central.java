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
import static javax.swing.SwingUtilities.invokeLater;
import static org.opendatakit.briefcase.ui.reused.SwingUtils.uncheckedBrowse;
import static org.opendatakit.briefcase.ui.reused.UI.removeAllMouseListeners;

import java.awt.Container;
import java.nio.file.Path;
import java.util.function.Consumer;
import javax.swing.JLabel;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.push.PushEvent;
import org.opendatakit.briefcase.push.central.PushToCentral;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.transfer.CentralServer;
import org.opendatakit.briefcase.reused.transfer.RemoteServer.Test;
import org.opendatakit.briefcase.transfer.TransferForms;
import org.opendatakit.briefcase.ui.reused.MouseAdapterBuilder;
import org.opendatakit.briefcase.ui.reused.transfer.sourcetarget.CentralServerDialog;

public class Central implements PushTarget<CentralServer> {
  private final Http http;
  private final Test<CentralServer> serverTester;
  private final Consumer<PushTarget> onSourceCallback;
  private CentralServer server;

  Central(Http http, Test<CentralServer> serverTester, Consumer<PushTarget> onSourceCallback) {
    this.http = http;
    this.serverTester = serverTester;
    this.onSourceCallback = onSourceCallback;
  }

  @Override
  public void storePreferences(BriefcasePreferences prefs, boolean storePasswords) {
    if (storePasswords)
      server.storePreferences(prefs);
  }

  public static void clearPreferences(BriefcasePreferences prefs) {
    CentralServer.clearPreferences(prefs);
  }

  @Override
  public JobsRunner push(TransferForms forms, Path briefcaseDir) {
    forms.filter(FormStatus::isEncrypted)
        .forEach(form -> form.setStatusString("Skipping. Encrypted forms can't be pushed to ODK Central yet"));

    String token = http.execute(server.getSessionTokenRequest()).orElseThrow(() -> new BriefcaseException("Can't authenticate with ODK Central"));
    PushToCentral pushOp = new PushToCentral(http, server, briefcaseDir, token, EventBus::publish);

    return JobsRunner.launchAsync(
        forms.filter(f -> !f.isEncrypted()).map(pushOp::push),
        __ -> onPushSuccess(forms),
        error -> {}
    );
  }

  private static void onPushSuccess(TransferForms forms) {
    EventBus.publish(new PushEvent.Success(forms, null));
  }

  @Override
  public String getDescription() {
    return server.getBaseUrl().toString();

  }

  @Override
  public void onSelect(Container container) {
    CentralServerDialog dialog = CentralServerDialog.empty(serverTester);
    dialog.onConnect(this::set);
    dialog.getForm().setVisible(true);
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
  public boolean accepts(Object o) {
    return o instanceof CentralServer;
  }

  @Override
  public void set(CentralServer server) {
    this.server = server;
    onSourceCallback.accept(this);
  }

  @Override
  public String toString() {
    return "Central server";
  }
}
