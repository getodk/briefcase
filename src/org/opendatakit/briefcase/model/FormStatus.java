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


public class FormStatus {
  public enum TransferType { GATHER, UPLOAD };
  private final TransferType transferType;
  private boolean isSelected = false;
  private IFormDefinition form;
  private String statusString = "";
  private final StringBuilder statusHistory = new StringBuilder();
  private boolean isSuccessful = true;
  private int historyMaxSize = 8192;

  public FormStatus(TransferType transferType, IFormDefinition form) {
    this.transferType = transferType;
    this.form = form;
  }

  public TransferType getTransferType() {
    return transferType;
  }
  
  public boolean isSelected() {
    return isSelected;
  }

  public void setSelected(boolean isSelected) {
    this.isSelected = isSelected;
  }

  public String getStatusString() {
    return statusString;
  }

  public void clearStatusHistory() {
    statusHistory.setLength(0);
    isSuccessful = true;
  }

  public void setStatusString(String statusString, boolean isSuccessful) {
    this.statusString = statusString;
    if (statusHistory.length() > historyMaxSize) {
      statusHistory.delete(0, statusString.length() + 1);
      int lineEnd = statusHistory.indexOf("\n");
      if (lineEnd >= 0) {
        statusHistory.delete(0, lineEnd+1);
      }
    }
    statusHistory.append("\n");
    statusHistory.append(statusString);
    // statusHistory.append("</p>");
    this.isSuccessful = this.isSuccessful && isSuccessful;
  }

  public String getStatusHistory() {
    return statusHistory.toString();
  }
  
  public boolean isSuccessful() {
    return isSuccessful;
  }

  public String getFormName() {
    return form.getFormName();
  }

  public IFormDefinition getFormDefinition() {
    return form;
  }
}
