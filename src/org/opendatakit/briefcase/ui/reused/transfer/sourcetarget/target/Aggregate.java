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
import static org.opendatakit.briefcase.ui.reused.UI.removeAllMouseListeners;

import java.awt.Container;
import java.nio.file.Path;
import java.util.function.Consumer;
import javax.swing.JLabel;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.push.PushEvent;
import org.opendatakit.briefcase.push.aggregate.PushToAggregate;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.transfer.AggregateServer;
import org.opendatakit.briefcase.reused.transfer.RemoteServer.Test;
import org.opendatakit.briefcase.transfer.TransferForms;
import org.opendatakit.briefcase.ui.reused.MouseAdapterBuilder;
import org.opendatakit.briefcase.ui.reused.SwingUtils;
import org.opendatakit.briefcase.ui.reused.transfer.sourcetarget.AggregateServerDialog;

/**
 * Represents an ODK Aggregate server as a target for sending forms for the Push UI Panel.
 */
public class Aggregate implements PushTarget<AggregateServer> {
  private final Http http;
  private final Consumer<PushTarget> consumer;
  private Test<AggregateServer> serverTester;
  private String requiredPermission;
  private AggregateServer server;

  Aggregate(Http http, Test<AggregateServer> serverTester, String requiredPermission, Consumer<PushTarget> consumer) {
    this.http = http;
    this.serverTester = serverTester;
    this.requiredPermission = requiredPermission;
    this.consumer = consumer;
  }

  static void clearPreferences(BriefcasePreferences prefs) {
    prefs.removeAll(AggregateServer.PREFERENCE_KEYS);
  }

  @Override
  public void onSelect(Container ignored) {
    AggregateServerDialog dialog = AggregateServerDialog.empty(serverTester, requiredPermission);
    dialog.onConnect(this::set);
    dialog.getForm().setVisible(true);
  }

  @Override
  public void set(AggregateServer server) {
    this.server = server;
    consumer.accept(this);
  }

  @Override
  public boolean accepts(Object o) {
    return o instanceof AggregateServer;
  }

  @Override
  public void storeSourcePrefs(BriefcasePreferences prefs, boolean storePasswords) {
    server.storeSourcePrefs(prefs, storePasswords);
  }

  @Override
  public JobsRunner push(TransferForms forms, Path briefcaseDir) {
    PushToAggregate pushOp = new PushToAggregate(http, server, briefcaseDir, false, EventBus::publish);

    return JobsRunner.launchAsync(
        forms.map(pushOp::push),
        __ -> onPushSuccess(forms),
        error -> { }
    );
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
        .onClick(__ -> invokeLater(() -> SwingUtils.uncheckedBrowse(server.getBaseUrl())))
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


  private static void onPushSuccess(TransferForms forms) {
    EventBus.publish(new PushEvent.Success(forms, null));
  }
}
