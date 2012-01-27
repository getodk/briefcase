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

package org.opendatakit.briefcase.model;

import java.util.List;

/**
 * Signals the completion of the retrieval of forms to display as available from
 * a server. The transfer of submissions is signaled by the TransferXXXEvent
 * classes.
 * 
 * @author mitchellsundt@gmail.com
 * 
 */
public class RetrieveAvailableFormsSucceededEvent {
  private FormStatus.TransferType transferType;
  private List<FormStatus> formsToTransfer;

  public RetrieveAvailableFormsSucceededEvent(FormStatus.TransferType transferType, List<FormStatus> formsToTransfer) {
    this.transferType = transferType;
    this.formsToTransfer = formsToTransfer;
  }

  public FormStatus.TransferType getTransferType() {
    return transferType;
  }
  
  public List<FormStatus> getFormsToTransfer() {
    return formsToTransfer;
  }

}
