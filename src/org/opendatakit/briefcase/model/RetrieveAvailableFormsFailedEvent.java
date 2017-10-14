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

import org.opendatakit.briefcase.model.FormStatus.TransferType;

public class RetrieveAvailableFormsFailedEvent {
  private TransferType transferType;
  private Exception e;

  public RetrieveAvailableFormsFailedEvent(TransferType transferType, Exception e) {
    this.transferType = transferType;
    this.e = e;
  }

  public TransferType getTransferType() {
    return transferType;
  }

  public String getReason() {
    if (e != null) {
      return "Exception: " + e.getMessage();
    } else {
      return "unknown";
    }
  }
}
