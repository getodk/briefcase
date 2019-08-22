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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.pull.PullEvent;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.transfer.TransferForms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransferAction {

  private static final Logger log = LoggerFactory.getLogger(TransferAction.class);

  private static ExecutorService backgroundExecutorService = Executors.newCachedThreadPool();

  private static void backgroundRun(ITransferFromSourceAction src, TransferForms formsToTransfer) {
    backgroundExecutorService.execute(new GatherTransferRunnable(src, formsToTransfer));
  }

  public static void transferODKToBriefcase(Path briefcaseDir, File odkSrcDir, TerminationFuture terminationFuture, TransferForms formsToTransfer) {
    TransferFromODK source = new TransferFromODK(briefcaseDir, odkSrcDir, terminationFuture, formsToTransfer);
    backgroundRun(source, formsToTransfer);
  }

  private static class GatherTransferRunnable implements Runnable {
    ITransferFromSourceAction src;
    private TransferForms formsToTransfer;

    GatherTransferRunnable(ITransferFromSourceAction src, TransferForms formsToTransfer) {
      this.src = src;
      this.formsToTransfer = formsToTransfer;
    }

    @Override
    public void run() {
      try {
        if (src.doAction())
          formsToTransfer.forEach(form -> EventBus.publish(PullEvent.Success.of(form)));
        else
          throw new BriefcaseException("Failed to pull form (legacy)");
      } catch (Exception e) {
        log.error("gather transfer action failed", e);
        throw new BriefcaseException("Failed to pull form (legacy)", e);
      }
    }

  }
}
