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

package org.opendatakit.briefcase.transfer;

import java.nio.file.Path;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.pull.PullEvent;
import org.opendatakit.briefcase.util.TransferFromServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewTransferAction {
  private static final Logger log = LoggerFactory.getLogger(NewTransferAction.class);

  public static void transferServerToBriefcase(ServerConnectionInfo transferSettings, TerminationFuture terminationFuture, TransferForms formsToTransfer, Path briefcaseDir, Boolean pullInParallel, Boolean includeIncomplete) {
    TransferFromServer action = new TransferFromServer(
        transferSettings,
        terminationFuture,
        formsToTransfer,
        briefcaseDir,
        pullInParallel,
        includeIncomplete
    );
    try {
      boolean allSuccessful = action.doAction();

      if (!allSuccessful)
        EventBus.publish(new PullEvent.Failure());

      if (allSuccessful)
        EventBus.publish(new PullEvent.Success(formsToTransfer, transferSettings));
    } catch (Exception e) {
      log.error("transfer action failed", e);
      EventBus.publish(new PullEvent.Failure());
    }
  }

}
