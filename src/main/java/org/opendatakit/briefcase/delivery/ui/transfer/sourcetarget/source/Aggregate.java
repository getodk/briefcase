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

package org.opendatakit.briefcase.delivery.ui.transfer.sourcetarget.source;

import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.delivery.ui.reused.UI.makeClickable;
import static org.opendatakit.briefcase.delivery.ui.reused.UI.uncheckedBrowse;
import static org.opendatakit.briefcase.reused.model.form.FormMetadataQueries.lastCursorOf;

import java.awt.Container;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.JLabel;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.delivery.ui.transfer.sourcetarget.AggregateServerDialog;
import org.opendatakit.briefcase.operations.transfer.TransferForms;
import org.opendatakit.briefcase.operations.transfer.pull.PullEvent;
import org.opendatakit.briefcase.operations.transfer.pull.aggregate.PullFromAggregate;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.Workspace;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormMetadataPort;
import org.opendatakit.briefcase.reused.model.preferences.BriefcasePreferences;
import org.opendatakit.briefcase.reused.model.submission.SubmissionMetadataPort;
import org.opendatakit.briefcase.reused.model.transfer.AggregateServer;
import org.opendatakit.briefcase.reused.model.transfer.RemoteServer.Test;

/**
 * Represents an ODK Aggregate server as a source of forms for the Pull UI Panel.
 */
public class Aggregate implements PullSource<AggregateServer> {
  private final Http http;
  private final Consumer<PullSource> consumer;
  private final Workspace workspace;
  private Test<AggregateServer> serverTester;
  private String usernameHelp;
  private AggregateServer server;

  Aggregate(Http http, Workspace workspace, Test<AggregateServer> serverTester, String usernameHelp, Consumer<PullSource> consumer) {
    this.http = http;
    this.workspace = workspace;
    this.serverTester = serverTester;
    this.usernameHelp = usernameHelp;
    this.consumer = consumer;
  }

  @Override
  public void onSelect(Container ignored) {
    AggregateServerDialog dialog = AggregateServerDialog.empty(serverTester, usernameHelp);
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
  public List<FormMetadata> getFormList() {
    return http.execute(server.getFormListRequest())
        .orElseThrow(() -> new BriefcaseException("Can't get forms list from server"))
        .stream()
        .map(formMetadata -> formMetadata.withFormFile(workspace.buildFormFile(formMetadata)))
        .collect(toList());
  }

  @Override
  public void storeSourcePrefs(BriefcasePreferences prefs, boolean storePasswords) {
    server.storeInPrefs(prefs, storePasswords);
  }

  @Override
  public JobsRunner pull(TransferForms forms, BriefcasePreferences appPreferences, FormMetadataPort formMetadataPort, SubmissionMetadataPort submissionMetadataPort) {
    boolean resumeLastPull = appPreferences.resolveStartFromLast();
    PullFromAggregate pullOp = new PullFromAggregate(
        http,
        formMetadataPort,
        submissionMetadataPort,
        server,
        false,
        EventBus::publish
    );

    return JobsRunner.launchAsync(forms.map(form -> pullOp.pull(
        form,
        resumeLastPull ? formMetadataPort.query(lastCursorOf(form.getKey())) : Optional.empty()
    ))).onComplete(() -> EventBus.publish(new PullEvent.PullComplete()));
  }

  @Override
  public boolean canBeReloaded() {
    return true;
  }

  @Override
  public String getDescription() {
    return server.getBaseUrl().toString();
  }

  @Override
  public void decorate(JLabel label) {
    label.setText("<html>URL: <a href=\"\">" + getDescription() + "</a></html>");
    makeClickable(label, () -> uncheckedBrowse(server.getBaseUrl()));
  }

  @Override
  public String toString() {
    return "Aggregate server";
  }
}
