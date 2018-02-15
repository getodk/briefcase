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

import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransmissionException;
import org.opendatakit.briefcase.operations.ServerConnectionTestException;

public class ServerConnectionTest implements Runnable {
  private final ServerConnectionInfo info;
  private final TerminationFuture terminationFuture;
  private final boolean asTarget;

  private String errorReason = null;
  private boolean isSuccessful = false;

  public ServerConnectionTest(ServerConnectionInfo info, TerminationFuture terminationFuture, boolean asTarget) {
    this.info = info;
    this.terminationFuture = terminationFuture;
    this.asTarget = asTarget;
  }

  public static void testPull(ServerConnectionInfo transferSettings) {
    ServerConnectionTest test = new ServerConnectionTest(transferSettings, new TerminationFuture(), false);
    test.run();
    if (!test.isSuccessful())
      throw new ServerConnectionTestException();
  }

  @Override
  public void run() {
    try {
      if (asTarget) {
        ServerUploader.testServerUploadConnection(info, terminationFuture);
      } else {
        ServerFetcher.testServerDownloadConnection(info, terminationFuture);
      }
      isSuccessful = true;
    } catch (TransmissionException ex) {
      errorReason = ex.getMessage();
      isSuccessful = false;
    }
  }

  public String getErrorReason() {
    return errorReason;
  }

  public boolean isSuccessful() {
    return isSuccessful;
  }
}