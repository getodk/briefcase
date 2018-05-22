/*
 * Copyright (C) 2011 University of Washington.
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

package org.opendatakit.briefcase.util;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.pull.PullEvent;

public class TransferFromServer implements ITransferFromSourceAction {

  final ServerConnectionInfo originServerInfo;
  final TerminationFuture terminationFuture;
  final List<FormStatus> formsToTransfer;
  private Path briefcaseDir;

  public TransferFromServer(ServerConnectionInfo originServerInfo, TerminationFuture terminationFuture, List<FormStatus> formsToTransfer, Path briefcaseDir) {
    this.originServerInfo = originServerInfo;
    this.terminationFuture = terminationFuture;
    this.formsToTransfer = formsToTransfer;
    this.briefcaseDir = briefcaseDir;
  }

  @Override
  public boolean doAction() {

    ServerFetcher fetcher = new ServerFetcher(originServerInfo, terminationFuture, briefcaseDir);

    return fetcher.downloadFormAndSubmissionFiles(formsToTransfer);
  }

  @Override
  public boolean isSourceDeletable() {
    return false;
  }

  public static void pull(ServerConnectionInfo transferSettings, Path briefcaseDir, FormStatus... forms) {
    List<FormStatus> formList = Arrays.asList(forms);
    TransferFromServer action = new TransferFromServer(transferSettings, new TerminationFuture(), formList, briefcaseDir);

    try {
      boolean allSuccessful = action.doAction();
      if (allSuccessful)
        EventBus.publish(new PullEvent.Success(formList, transferSettings));

      if (!allSuccessful)
        throw new PullFromServerException(formList);
    } catch (Exception e) {
      EventBus.publish(new PullEvent.Failure());
      throw new PullFromServerException(formList);
    }
  }

  @Override
  public Optional<ServerConnectionInfo> getTransferSettings() {
    return Optional.of(originServerInfo);
  }
}