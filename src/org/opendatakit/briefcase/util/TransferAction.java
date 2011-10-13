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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.RetrieveAvailableFormsFailedEvent;
import org.opendatakit.briefcase.model.RetrieveAvailableFormsSucceededEvent;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransferFailedEvent;
import org.opendatakit.briefcase.model.TransferSucceededEvent;
import org.opendatakit.briefcase.ui.TransferInProgressDialog;

public class TransferAction {

  static final String SCRATCH_DIR = "scratch";

  private static ExecutorService backgroundExecutorService = Executors.newCachedThreadPool();

  private static class TransferRunnable implements Runnable {
    ITransferFromSourceAction src;
    ITransferToDestAction dest;
    private List<FormStatus> formsToTransfer;

    TransferRunnable(ITransferFromSourceAction src, ITransferToDestAction dest,
        List<FormStatus> formsToTransfer) {
      this.src = src;
      this.dest = dest;
      this.formsToTransfer = formsToTransfer;
    }

    @Override
    public void run() {
      boolean srcIsDeletable = false;
      if (src != null) {
        srcIsDeletable = src.isSourceDeletable();
      }
      try {
        boolean allSuccessful = true;
        if (src != null) {
          allSuccessful = allSuccessful & // do not short-circuit! 
            src.doAction();
        }
        if (dest != null) {
          allSuccessful = allSuccessful & // do not short-circuit!
            dest.doAction();
        }
        
        if ( allSuccessful ) {
          EventBus.publish(new TransferSucceededEvent(srcIsDeletable, formsToTransfer));
        } else {
          EventBus.publish(new TransferFailedEvent(srcIsDeletable, formsToTransfer));
        }
      } catch (Exception e) {
        e.printStackTrace();
        EventBus.publish(new TransferFailedEvent(srcIsDeletable, formsToTransfer));
      }
    }

  }

  private static void showDialogAndRun(ITransferFromSourceAction src, ITransferToDestAction dest,
      TerminationFuture terminationFuture, List<FormStatus> formsToTransfer) {
    // create the dialog first so that the background task will always have a
    // listener for its completion events...
    final TransferInProgressDialog dlg = new TransferInProgressDialog("Transfer in Progress...", terminationFuture);

    backgroundExecutorService.execute(new TransferRunnable(src, dest, formsToTransfer));

    dlg.setVisible(true);
  }

  private static class RetrieveAvailableFormsRunnable implements Runnable {
    RetrieveAvailableFormsFromServer src;

    RetrieveAvailableFormsRunnable(RetrieveAvailableFormsFromServer src) {
      this.src = src;
    }

    @Override
    public void run() {
      try {
        src.doAction();
        EventBus.publish(new RetrieveAvailableFormsSucceededEvent(src.getAvailableForms()));
      } catch (Exception e) {
        e.printStackTrace();
        EventBus.publish(new RetrieveAvailableFormsFailedEvent(e));
      }
    }

  }

  private static void showDialogAndRun(RetrieveAvailableFormsFromServer src, TerminationFuture terminationFuture) {
    // create the dialog first so that the background task will always have a
    // listener for its completion events...
    final TransferInProgressDialog dlg = new TransferInProgressDialog("Fetching Available Forms...", terminationFuture);

    backgroundExecutorService.execute(new RetrieveAvailableFormsRunnable(src));

    dlg.setVisible(true);
  }

  public static void retrieveAvailableFormsFromServer(ServerConnectionInfo originServerInfo, TerminationFuture terminationFuture) {
    RetrieveAvailableFormsFromServer source = new RetrieveAvailableFormsFromServer(originServerInfo, terminationFuture);
    showDialogAndRun(source, terminationFuture);
  }
  
  public static void uploadForm(ServerConnectionInfo destinationServerInfo, 
      TerminationFuture terminationFuture, File formDefn, FormStatus status) {
    UploadToServer dest = new UploadToServer( destinationServerInfo, terminationFuture, formDefn, status);
    showDialogAndRun( null, dest, terminationFuture, Collections.singletonList(status));
  }

  public static void transferServerViaToServer(ServerConnectionInfo originServerInfo,
      ServerConnectionInfo destinationServerInfo, TerminationFuture terminationFuture, 
      List<FormStatus> formsToTransfer) throws IOException {

    File briefcaseDir = new File(BriefcasePreferences.getBriefcaseDirectoryProperty());
    TransferFromServer source = new TransferFromServer(originServerInfo, terminationFuture, 
        briefcaseDir,
        formsToTransfer, true);
    TransferToServer dest = new TransferToServer(destinationServerInfo, 
        terminationFuture, briefcaseDir,
        formsToTransfer, true);
    showDialogAndRun(source, dest, terminationFuture, formsToTransfer);
  }

  public static void transferServerViaToBriefcase(ServerConnectionInfo originServerInfo,
      TerminationFuture terminationFuture,
      List<FormStatus> formsToTransfer)
      throws IOException {

    File briefcaseDir = new File(BriefcasePreferences.getBriefcaseDirectoryProperty());
    TransferFromServer source = new TransferFromServer(originServerInfo, terminationFuture,
        briefcaseDir,
        formsToTransfer, false);
    showDialogAndRun(source, null, terminationFuture, formsToTransfer);
  }

  public static void transferODKViaToServer(File odkSrcDir,
      ServerConnectionInfo destinationServerInfo,
      TerminationFuture terminationFuture,
      List<FormStatus> formsToTransfer) throws IOException {

    File briefcaseDir = new File(BriefcasePreferences.getBriefcaseDirectoryProperty());
    TransferFromODK source = new TransferFromODK(odkSrcDir, briefcaseDir, formsToTransfer, true);
    TransferToServer dest = new TransferToServer(destinationServerInfo, 
        terminationFuture, briefcaseDir,
        formsToTransfer, true);
    showDialogAndRun(source, dest, terminationFuture, formsToTransfer);
  }

  public static void transferODKViaToBriefcase(TerminationFuture terminationFuture, File odkSrcDir,
      List<FormStatus> formsToTransfer) throws IOException {

    File briefcaseDir = new File(BriefcasePreferences.getBriefcaseDirectoryProperty());
    TransferFromODK source = new TransferFromODK(odkSrcDir, briefcaseDir, formsToTransfer, false);
    showDialogAndRun(source, null, terminationFuture, formsToTransfer);
  }

  public static void transferBriefcaseViaToServer(
      ServerConnectionInfo destinationServerInfo,
      TerminationFuture terminationFuture,
      List<FormStatus> formsToTransfer) throws IOException {

    File briefcaseDir = new File(BriefcasePreferences.getBriefcaseDirectoryProperty());
    TransferToServer dest = new TransferToServer(destinationServerInfo, 
        terminationFuture, briefcaseDir,
        formsToTransfer, false);
    showDialogAndRun(null, dest, terminationFuture, formsToTransfer);
  }
}
