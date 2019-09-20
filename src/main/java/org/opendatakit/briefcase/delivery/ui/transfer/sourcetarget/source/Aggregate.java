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

import static org.opendatakit.briefcase.delivery.ui.reused.UI.makeClickable;
import static org.opendatakit.briefcase.delivery.ui.reused.UI.uncheckedBrowse;
import static org.opendatakit.briefcase.reused.model.form.FormMetadataQueries.lastCursorOf;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.JLabel;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.delivery.ui.transfer.sourcetarget.AggregateServerDialog;
import org.opendatakit.briefcase.operations.transfer.SourceOrTarget;
import org.opendatakit.briefcase.operations.transfer.TransferForms;
import org.opendatakit.briefcase.operations.transfer.pull.PullEvent;
import org.opendatakit.briefcase.operations.transfer.pull.aggregate.PullFromAggregate;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.Container;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.transfer.AggregateServer;
import org.opendatakit.briefcase.reused.model.transfer.RemoteServer.Test;

/**
 * Represents an ODK Aggregate server as a source of forms for the Pull UI Panel.
 */
public class Aggregate implements SourcePanelValueContainer {
  private final Container container;
  private final Consumer<SourcePanelValueContainer> consumer;
  private Test<AggregateServer> serverTester;
  private String usernameHelp;
  private AggregateServer server;

  Aggregate(Container container, Test<AggregateServer> serverTester, String usernameHelp, Consumer<SourcePanelValueContainer> consumer) {
    this.container = container;
    this.serverTester = serverTester;
    this.usernameHelp = usernameHelp;
    this.consumer = consumer;
  }

  @Override
  public void onSelect(java.awt.Container ignored) {
    AggregateServerDialog dialog = AggregateServerDialog.empty(serverTester, usernameHelp);
    dialog.onConnect(this::set);
    dialog.getForm().setVisible(true);
  }

  @Override
  public void set(SourceOrTarget server) {
    this.server = (AggregateServer) server;
    consumer.accept(this);
  }

  @Override
  public SourceOrTarget get() {
    return server;
  }

  @Override
  public SourceOrTarget.Type getType() {
    return SourceOrTarget.Type.AGGREGATE;
  }

  @Override
  public List<FormMetadata> getFormList() {
    return container.http.execute(server.getFormListRequest())
        .orElseThrow(() -> new BriefcaseException("Can't get forms list from server"));
  }

  @Override
  public JobsRunner pull(TransferForms forms, boolean startFromLast) {
    PullFromAggregate pullOp = new PullFromAggregate(container, server, false, EventBus::publish);
    return JobsRunner.launchAsync(forms.map(formMetadata -> pullOp.pull(
        formMetadata,
        container.workspace.buildFormFile(formMetadata),
        startFromLast ? container.formMetadata.query(lastCursorOf(formMetadata.getKey())) : Optional.empty()
    ))).onComplete(() -> EventBus.publish(new PullEvent.PullComplete()));
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
  public boolean canBeReloaded() {
    return true;
  }

  @Override
  public String toString() {
    return "Aggregate server";
  }

}
