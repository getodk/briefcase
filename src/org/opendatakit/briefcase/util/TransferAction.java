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

import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.RetrieveAvailableFormsFailedEvent;
import org.opendatakit.briefcase.model.RetrieveAvailableFormsSucceededEvent;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransferFailedEvent;
import org.opendatakit.briefcase.model.TransferSucceededEvent;
import org.opendatakit.briefcase.ui.TransferInProgressDialog;

public class TransferAction {

  private static ExecutorService backgroundExecutorService = Executors.newCachedThreadPool();

  private static class UploadTransferRunnable implements Runnable {
    ITransferToDestAction dest;
    private List<FormStatus> formsToTransfer;

    UploadTransferRunnable(ITransferToDestAction dest, List<FormStatus> formsToTransfer) {
      this.dest = dest;
      this.formsToTransfer = formsToTransfer;
    }

    @Override
    public void run() {
      boolean srcIsDeletable = false;
      try {
        boolean allSuccessful = dest.doAction();

        if (allSuccessful) {
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

  private static void backgroundRun(ITransferToDestAction src, List<FormStatus> formsToTransfer) {
    backgroundExecutorService.execute(new UploadTransferRunnable(src, formsToTransfer));
  }

  private static class GatherTransferRunnable implements Runnable {
    ITransferFromSourceAction src;
    private List<FormStatus> formsToTransfer;

    GatherTransferRunnable(ITransferFromSourceAction src, List<FormStatus> formsToTransfer) {
      this.src = src;
      this.formsToTransfer = formsToTransfer;
    }

    @Override
    public void run() {
      boolean srcIsDeletable = src.isSourceDeletable();
      try {
        boolean allSuccessful = src.doAction();

        if (allSuccessful) {
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

  private static void backgroundRun(ITransferFromSourceAction src, List<FormStatus> formsToTransfer) {
    backgroundExecutorService.execute(new GatherTransferRunnable(src, formsToTransfer));
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
        EventBus.publish(new RetrieveAvailableFormsSucceededEvent(FormStatus.TransferType.GATHER, src
            .getAvailableForms()));
      } catch (Exception e) {
        e.printStackTrace();
        EventBus.publish(new RetrieveAvailableFormsFailedEvent(FormStatus.TransferType.GATHER, e));
      }
    }

  }

  private static void showDialogAndRun(Window topLevel, RetrieveAvailableFormsFromServer src,
      TerminationFuture terminationFuture) {
    // create the dialog first so that the background task will always have a
    // listener for its completion events...
    final TransferInProgressDialog dlg = new TransferInProgressDialog(topLevel,
        FormStatus.TransferType.GATHER, terminationFuture);

    backgroundExecutorService.execute(new RetrieveAvailableFormsRunnable(src));

    dlg.setVisible(true);
  }

  public static void retrieveAvailableFormsFromServer(Window topLevel,
      ServerConnectionInfo originServerInfo, TerminationFuture terminationFuture) {
    RetrieveAvailableFormsFromServer source = 
        new RetrieveAvailableFormsFromServer(originServerInfo, terminationFuture);
    showDialogAndRun(topLevel, source, terminationFuture);
  }

  public static void uploadForm(Window topLevel, ServerConnectionInfo destinationServerInfo,
      TerminationFuture terminationFuture, File formDefn, FormStatus status) {
    UploadToServer dest = new UploadToServer(destinationServerInfo, terminationFuture, formDefn,
        status);
    backgroundRun(dest, Collections.singletonList(status));
  }

  public static void transferServerToBriefcase(ServerConnectionInfo originServerInfo,
      TerminationFuture terminationFuture, List<FormStatus> formsToTransfer) throws IOException {

    TransferFromServer source = new TransferFromServer(originServerInfo, terminationFuture,
        formsToTransfer);
    backgroundRun(source, formsToTransfer);
  }

  /**
   * @param odkSrcDir
   *          -- NOTE: this ends with /odk in the typical case.
   * @param terminationFuture
   * @param formsToTransfer
   * @throws IOException
   */
  public static void transferODKToBriefcase(File odkSrcDir, TerminationFuture terminationFuture,
      List<FormStatus> formsToTransfer) throws IOException {

    TransferFromODK source = new TransferFromODK(odkSrcDir, terminationFuture, formsToTransfer);
    backgroundRun(source, formsToTransfer);
  }

  public static void transferBriefcaseToServer(ServerConnectionInfo destinationServerInfo,
      TerminationFuture terminationFuture, List<FormStatus> formsToTransfer) throws IOException {

    TransferToServer dest = new TransferToServer(destinationServerInfo, terminationFuture,
        formsToTransfer);
    backgroundRun(dest, formsToTransfer);
  }
}
