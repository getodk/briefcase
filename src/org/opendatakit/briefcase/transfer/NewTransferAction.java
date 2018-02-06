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

package org.opendatakit.briefcase.transfer;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransferFailedEvent;
import org.opendatakit.briefcase.model.TransferSucceededEvent;
import org.opendatakit.briefcase.util.TransferFromServer;

public class NewTransferAction {
  private static final Log log = LogFactory.getLog(NewTransferAction.class);

  public static void transferServerToBriefcase(ServerConnectionInfo transferSettings, TerminationFuture terminationFuture, List<FormStatus> formsToTransfer) {
    TransferFromServer action = new TransferFromServer(
        transferSettings,
        terminationFuture,
        formsToTransfer
    );
    try {
      boolean allSuccessful = action.doAction();

      if (!allSuccessful)
        EventBus.publish(new TransferFailedEvent(false, formsToTransfer));

      if (allSuccessful)
        EventBus.publish(new TransferSucceededEvent(false, formsToTransfer, transferSettings));
    } catch (Exception e) {
      log.error("transfer action failed", e);
      EventBus.publish(new TransferFailedEvent(false, formsToTransfer));
    }
  }

}
