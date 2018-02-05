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

import java.util.List;

import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TerminationFuture;

public class TransferToServer implements ITransferToDestAction {
  ServerConnectionInfo destServerInfo;
  TerminationFuture terminationFuture;
  List<FormStatus> formsToTransfer;

  public TransferToServer(ServerConnectionInfo destServerInfo, 
      TerminationFuture terminationFuture, List<FormStatus> formsToTransfer) {
    this.destServerInfo = destServerInfo;
    this.terminationFuture = terminationFuture;
    this.formsToTransfer = formsToTransfer;
  }

  @Override
  public boolean doAction() {
    ServerUploader uploader = new ServerUploader(destServerInfo, terminationFuture);
    
    return uploader.uploadFormAndSubmissionFiles( formsToTransfer);
  }

  @Override
  public ServerConnectionInfo getTransferSettings() {
    return destServerInfo;
  }
}