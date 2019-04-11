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
import java.time.LocalDate;
import java.util.Optional;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.pull.PullEvent;
import org.opendatakit.briefcase.transfer.TransferForms;

public class TransferFromServer implements ITransferFromSourceAction {

  private final ServerConnectionInfo originServerInfo;
  private final TerminationFuture terminationFuture;
  private final TransferForms formsToTransfer;
  private final Boolean pullInParallel;
  private final Boolean includeIncomplete;
  private final Path briefcaseDir;
  private final boolean resumeLastPull;
  private Optional<LocalDate> startFromDate;

  public TransferFromServer(ServerConnectionInfo originServerInfo, TerminationFuture terminationFuture, TransferForms formsToTransfer, Path briefcaseDir, Boolean pullInParallel, Boolean includeIncomplete, boolean resumeLastPull, Optional<LocalDate> startFromDate) {
    this.originServerInfo = originServerInfo;
    this.terminationFuture = terminationFuture;
    this.formsToTransfer = formsToTransfer;
    this.briefcaseDir = briefcaseDir;
    this.pullInParallel = pullInParallel;
    this.includeIncomplete = includeIncomplete;
    this.resumeLastPull = resumeLastPull;
    this.startFromDate = startFromDate;
  }

  public static void pull(ServerConnectionInfo transferSettings, Path briefcaseDir, Boolean pullInParallel, Boolean includeIncomplete, TransferForms forms, boolean resumeLastPull, Optional<LocalDate> startFromDate) {
    TransferFromServer action = new TransferFromServer(transferSettings, new TerminationFuture(), forms, briefcaseDir, pullInParallel, includeIncomplete, resumeLastPull, startFromDate);

    try {
      boolean allSuccessful = action.doAction();
      if (allSuccessful)
        EventBus.publish(new PullEvent.Success(forms, transferSettings));

      if (!allSuccessful)
        throw new PullFromServerException(forms);
    } catch (Exception e) {
      EventBus.publish(new PullEvent.Failure());
      throw new PullFromServerException(forms, e);
    }
  }

  @Override
  public boolean doAction() {

    ServerFetcher fetcher = new ServerFetcher(originServerInfo, terminationFuture, briefcaseDir, pullInParallel, includeIncomplete);

    return fetcher.downloadFormAndSubmissionFiles(formsToTransfer, resumeLastPull, startFromDate);
  }

  @Override
  public Optional<ServerConnectionInfo> getTransferSettings() {
    return Optional.of(originServerInfo);
  }
}
