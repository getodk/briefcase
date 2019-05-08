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
import java.net.URI;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.DocumentDescription;
import org.opendatakit.briefcase.model.FileSystemException;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.MetadataUpdateException;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransmissionException;
import org.opendatakit.briefcase.pull.aggregate.InstanceIdBatch;
import org.opendatakit.briefcase.pull.aggregate.InstanceIdBatchGetter;
import org.opendatakit.briefcase.reused.RemoteServer;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.transfer.TransferForms;
import org.opendatakit.briefcase.util.AggregateUtils.DocumentFetchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ServerUploader {

  private static final Logger log = LoggerFactory.getLogger(ServerUploader.class);

  private final ServerConnectionInfo serverInfo;
  private final TerminationFuture terminationFuture;
  private final Http http;
  private final RemoteServer server;
  private final boolean forceSendBlank;

  ServerUploader(ServerConnectionInfo serverInfo, TerminationFuture terminationFuture, Http http, RemoteServer server, boolean forceSendBlank) {
    AnnotationProcessor.process(this);// if not using AOP
    this.serverInfo = serverInfo;
    this.terminationFuture = terminationFuture;
    this.server = server;
    this.http = http;
    this.forceSendBlank = forceSendBlank;
  }

  private boolean isCancelled() {
    return terminationFuture.isCancelled();
  }

  public static class SubmissionResponseAction implements AggregateUtils.ResponseAction {

    private final File file;
    private String instanceID = null;

    SubmissionResponseAction(File file) {
      this.file = file;
    }

    @Override
    public void doAction(DocumentFetchResult result) throws MetadataUpdateException {
      instanceID = XmlManipulationUtils.updateSubmissionMetadata(file, result.doc);
    }

    void afterUpload(FormStatus formToTransfer) {
      if (instanceID == null)
        return;

      File instanceDir = file.getParentFile();
      File instancesParentDir = instanceDir.getParentFile();

      File newInstanceDir = FileSystemUtils.getFormSubmissionDirectory(instancesParentDir, instanceID);
      if (!Objects.equals(newInstanceDir, instanceDir)) {
        try {
          log.info("Moving {} to {}", instanceDir, newInstanceDir);
          FileUtils.moveDirectory(instanceDir, Objects.requireNonNull(newInstanceDir));
          /*
           * We might be tempted to do:
           * formDatabase.putRecordedInstanceDirectory(instanceID, newInstanceDir);
           * However, this would be incorrect.
           * We don't know for certain whether the local
           * copy of the submission is, in fact, fully
           * complete.  The server may have already held a
           * complete copy.  We must download from the
           * server to ensure we have a complete local copy.
           */
          String msg = "NOTE: Renaming submission directory: " + instanceDir.getName() + " to: " + newInstanceDir.getName();
          formToTransfer.setStatusString(msg, true);
          EventBus.publish(new FormStatusEvent(formToTransfer));
        } catch (IOException e) {
          String msg = "Submission directory could not be renamed";
          log.warn(msg, e);
          formToTransfer.setStatusString("WARNING: " + msg, true);
          EventBus.publish(new FormStatusEvent(formToTransfer));
        }
      }
    }
  }

  // remove any instances already completed on server
  private void subtractServerInstances(FormStatus fs, Set<File> instancesToUpload, Path briefcaseDir) {
    RemoteServer server = RemoteServer.from(serverInfo);

    List<InstanceIdBatch> batches = new ArrayList<>();
    InstanceIdBatchGetter batchPager = new InstanceIdBatchGetter(server, http, fs.getFormId(), false);
    while (batchPager.hasNext())
      batches.add(batchPager.next());

    List<File> remoteSubmissions = batches.stream()
        .flatMap(batch -> batch.getInstanceIds().stream())
        .map(instanceId -> fs.getSubmissionDir(briefcaseDir, instanceId).toFile())
        .collect(Collectors.toList());
    instancesToUpload.removeAll(remoteSubmissions);
  }

  boolean uploadFormAndSubmissionFiles(TransferForms formsToTransfer) {

    boolean allSuccessful = true;

    for (FormStatus formToTransfer : formsToTransfer) {

      BriefcaseFormDefinition briefcaseLfd = (BriefcaseFormDefinition) formToTransfer.getFormDefinition();
      boolean thisFormSuccessful = true;

      if (isCancelled()) {
        formToTransfer.setStatusString("Aborted upload.", true);
        EventBus.publish(new FormStatusEvent(formToTransfer));
        return false;
      }

      if (!formToTransfer.isSuccessful()) {
        formToTransfer.setStatusString("Skipping upload -- download failed", false);
        EventBus.publish(new FormStatusEvent(formToTransfer));
        continue;
      }

      File briefcaseFormDefFile = FileSystemUtils.getFormDefinitionFileIfExists(briefcaseLfd.getFormDirectory());
      if (briefcaseFormDefFile == null) {
        formToTransfer.setStatusString("Form does not exist", true);
        EventBus.publish(new FormStatusEvent(formToTransfer));
        continue;
      }
      File briefcaseFormMediaDir = FileSystemUtils.getMediaDirectoryIfExists(briefcaseLfd.getFormDirectory());

      boolean outcome;
      if (forceSendBlank || !checkIfExistsAlready(formToTransfer))
        outcome = uploadForm(formToTransfer, briefcaseFormDefFile, briefcaseFormMediaDir);
      else {
        formToTransfer.setStatusString("Skipping form upload to remote server because it already exists", true);
        EventBus.publish(new FormStatusEvent(formToTransfer));
        outcome = true;
      }
      thisFormSuccessful = thisFormSuccessful & outcome;
      allSuccessful = allSuccessful & outcome;

      URI u = getUploadSubmissionUri(formToTransfer);
      if (u == null) {
        // error already logged...
        continue;
      }

      Set<File> briefcaseInstances = FileSystemUtils.getFormSubmissionDirectories(briefcaseLfd.getFormDirectory());
      DatabaseUtils formDatabase = null;
      try {
        formDatabase = DatabaseUtils.newInstance(briefcaseLfd.getFormDirectory());

        // make sure all the local instances are in the database...
        formDatabase.updateInstanceLists();

        // exclude submissions the server reported as already submitted
        subtractServerInstances(formToTransfer, briefcaseInstances, briefcaseLfd.getFormDirectory().toPath().getParent().getParent());

        int i = 1;
        for (File briefcaseInstance : briefcaseInstances) {
          outcome = uploadSubmission(formToTransfer, u, i++, briefcaseInstances.size(), briefcaseInstance);
          thisFormSuccessful = thisFormSuccessful & outcome;
          allSuccessful = allSuccessful & outcome;
          // and stop this loop quickly if we're cancelled...
          if (isCancelled()) {
            break;
          }
        }
      } catch (SQLException | FileSystemException e) {
        thisFormSuccessful = false;
        allSuccessful = false;
        String msg = "unable to open form database";
        log.error(msg, e);
        formToTransfer.setStatusString(msg + ": " + e.getMessage(), false);
        EventBus.publish(new FormStatusEvent(formToTransfer));
      } finally {
        if (formDatabase != null) {
          try {
            formDatabase.close();
          } catch (SQLException e) {
            thisFormSuccessful = false;
            allSuccessful = false;
            String msg = "unable to close form database";
            log.warn(msg, e);
            formToTransfer.setStatusString(msg + ": " + e.getMessage(), false);
            EventBus.publish(new FormStatusEvent(formToTransfer));
          }
        }
      }

      if (isCancelled()) {
        formToTransfer.setStatusString("Aborted upload.", true);
        EventBus.publish(new FormStatusEvent(formToTransfer));
      } else if (thisFormSuccessful) {
        formToTransfer.setStatusString("Successful upload!", true);
        EventBus.publish(new FormStatusEvent(formToTransfer));
      } else {
        formToTransfer.setStatusString("Partially successful upload...", true);
        EventBus.publish(new FormStatusEvent(formToTransfer));
      }
    }
    return allSuccessful;
  }

  private Boolean checkIfExistsAlready(FormStatus form) {
    return server.containsForm(http, form.getFormDefinition().getFormId());
  }

  private boolean uploadForm(FormStatus formToTransfer, File briefcaseFormDefFile, File briefcaseFormMediaDir) {
    // very similar to upload submissions...

    URI u;
    try {
      u = AggregateUtils.getAggregateActionUri(serverInfo, "formUpload");
    } catch (TransmissionException e) {
      formToTransfer.setStatusString(e.getMessage(), false);
      EventBus.publish(new FormStatusEvent(formToTransfer));
      return false;
    }

    // We have the actual server URL in u, possibly redirected to https.
    // We know we are talking to the server because the head request
    // succeeded and had a Location header field.

    // try to send form...
    if (!briefcaseFormDefFile.exists()) {
      String msg = "Form definition file not found: " + briefcaseFormDefFile.getAbsolutePath();
      formToTransfer.setStatusString(msg, false);
      EventBus.publish(new FormStatusEvent(formToTransfer));
      return false;
    }

    // find all files in parent directory
    File[] allFiles = null;
    if (briefcaseFormMediaDir != null) {
      allFiles = briefcaseFormMediaDir.listFiles();
    }

    // clean up the list, removing anything that is suspicious
    // or that we won't attempt to upload. For OpenRosa servers,
    // we'll upload just about everything...
    List<File> files = new ArrayList<>();
    if (allFiles != null) {
      for (File f : allFiles) {
        String fileName = f.getName();
        if (fileName.startsWith(".")) {
          // potential Apple file attributes file -- ignore it
          continue;
        }
        files.add(f);
      }
    }

    DocumentDescription formDefinitionUploadDescription = new DocumentDescription("Form definition upload failed.  Detailed error: ",
        "form definition", terminationFuture);

    return AggregateUtils.uploadFilesToServer(serverInfo, u, "form_def_file", briefcaseFormDefFile, files,
        formDefinitionUploadDescription, null, terminationFuture, formToTransfer);
  }

  private URI getUploadSubmissionUri(FormStatus formToTransfer) {
    URI u;
    try {
      // Get the actual server URL in u, possibly redirected to https.
      // We know we are talking to the server because the head request
      // succeeded and had a Location header field.
      u = AggregateUtils.getAggregateActionUri(serverInfo, "submission");
    } catch (TransmissionException e) {
      formToTransfer.setStatusString(e.getMessage(), false);
      EventBus.publish(new FormStatusEvent(formToTransfer));
      return null;
    }
    return u;
  }

  private boolean uploadSubmission(FormStatus formToTransfer, URI u, int count, int totalCount, File instanceDirectory) {

    // We have the actual server URL in u, possibly redirected to https.
    // We know we are talking to the server because the head request
    // succeeded and had a Location header field.

    // try to send instance
    // get instance file
    File file = new File(instanceDirectory, "submission.xml");

    String submissionFile = file.getName();

    if (!file.exists()) {
      String msg = "Submission file not found: " + file.getAbsolutePath();
      formToTransfer.setStatusString(msg, false);
      EventBus.publish(new FormStatusEvent(formToTransfer));
      return false;
    }

    // find all files in parent directory
    File[] allFiles = instanceDirectory.listFiles();

    // clean up the list, removing anything that is suspicious
    // or that we won't attempt to upload. For OpenRosa servers,
    // we'll upload just about everything...
    List<File> files = new ArrayList<>();
    for (File f : Objects.requireNonNull(allFiles)) {
      String fileName = f.getName();
      if (fileName.startsWith(".")) {
        // potential Apple file attributes file -- ignore it
        continue;
      }
      if (fileName.equals(submissionFile)) {
        continue; // this is always added
      } else {
        files.add(f);
      }
    }
    SubmissionResponseAction action = new SubmissionResponseAction(file);

    if (isCancelled()) {
      formToTransfer.setStatusString("aborting upload of submission...", true);
      EventBus.publish(new FormStatusEvent(formToTransfer));
      return false;
    }

    DocumentDescription submissionUploadDescription = new DocumentDescription("Submission upload failed.  Detailed error: ",
        "submission (" + count + " of " + totalCount + ")", terminationFuture);
    boolean outcome = AggregateUtils.uploadFilesToServer(serverInfo, u, "xml_submission_file", file, files,
        submissionUploadDescription, action, terminationFuture, formToTransfer);

    // and try to rename the instance directory to be its instanceID
    action.afterUpload(formToTransfer);
    return outcome;
  }

}
