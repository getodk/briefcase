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
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.pull.PullEvent;
import org.opendatakit.briefcase.push.PushEvent;
import org.opendatakit.briefcase.reused.RemoteServer;
import org.opendatakit.briefcase.reused.http.Http;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransferAction {

  private static final Logger log = LoggerFactory.getLogger(TransferAction.class);

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
      try {
        boolean allSuccessful = dest.doAction();

        if (allSuccessful)
          EventBus.publish(new PushEvent.Success(formsToTransfer, dest.getTransferSettings()));
        else
          EventBus.publish(new PushEvent.Failure());
      } catch (Exception e) {
        log.error("upload transfer action failed", e);
        EventBus.publish(new PushEvent.Failure());
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
      try {
        if (src.doAction())
          EventBus.publish(src.getTransferSettings()
              .map(ts -> new PullEvent.Success(formsToTransfer, ts))
              .orElse(new PullEvent.Success(formsToTransfer)));
        else
          EventBus.publish(new PullEvent.Failure());
      } catch (Exception e) {
        log.error("gather transfer action failed", e);
        EventBus.publish(new PullEvent.Failure());
      }
    }

  }

  private static void backgroundRun(ITransferFromSourceAction src, List<FormStatus> formsToTransfer) {
    backgroundExecutorService.execute(new GatherTransferRunnable(src, formsToTransfer));
  }

  public static void transferServerToBriefcase(ServerConnectionInfo originServerInfo, TerminationFuture terminationFuture, List<FormStatus> formsToTransfer, Path briefcaseDir) {
    TransferFromServer source = new TransferFromServer(originServerInfo, terminationFuture, formsToTransfer, briefcaseDir);
    backgroundRun(source, formsToTransfer);
  }

  public static void transferODKToBriefcase(Path briefcaseDir, File odkSrcDir, TerminationFuture terminationFuture, List<FormStatus> formsToTransfer) {
    TransferFromODK source = new TransferFromODK(briefcaseDir, odkSrcDir, terminationFuture, formsToTransfer);
    backgroundRun(source, formsToTransfer);
  }

  public static void transferBriefcaseToServer(ServerConnectionInfo destinationServerInfo, TerminationFuture terminationFuture, List<FormStatus> formsToTransfer, Http http, RemoteServer server) {
    TransferToServer dest = new TransferToServer(
        destinationServerInfo,
        terminationFuture,
        formsToTransfer,
        http,
        server,
        // Since this is only used by the GUI, always force sending the blank form
        true
    );
    backgroundRun(dest, formsToTransfer);
  }
}
