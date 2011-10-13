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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.opendatakit.briefcase.model.DocumentDescription;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.MetadataUpdateException;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransmissionException;
import org.opendatakit.briefcase.util.AggregateUtils.DocumentFetchResult;

public class ServerUploader {

  private final ServerConnectionInfo serverInfo;
  private final TerminationFuture terminationFuture;

  ServerUploader(ServerConnectionInfo serverInfo, TerminationFuture terminationFuture) {
    AnnotationProcessor.process(this);// if not using AOP
    this.serverInfo = serverInfo;
    this.terminationFuture = terminationFuture;
  }

  public boolean isCancelled() { 
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
    
    public void afterUpload(FormStatus formToTransfer) {
      if ( instanceID == null ) return;
      
      File instanceDir = file.getParentFile();
      File instancesParentDir = instanceDir.getParentFile();
      
      File newInstanceDir = FileSystemUtils.getFormSubmissionDirectory(instancesParentDir, instanceID);
      if ( !newInstanceDir.equals(instanceDir) ) {
        try {
          FileUtils.moveDirectory(instanceDir, newInstanceDir);
          String msg = "NOTE: Renaming submission directory: " + instanceDir.getName() + " to: " + newInstanceDir.getName();
          formToTransfer.setStatusString(msg, true);
          EventBus.publish(new FormStatusEvent(formToTransfer));
        } catch (IOException e) {
          e.printStackTrace();
          String msg = "WARNING: Submission directory could not be renamed: " + instanceDir.getName();
          formToTransfer.setStatusString(msg, true);
          EventBus.publish(new FormStatusEvent(formToTransfer));
        }
      }
    }
    
  }
  
  public boolean uploadFormAndSubmissionFiles(File briefcaseFormsDir,
                      List<FormStatus> formsToTransfer) {
    
    boolean allSuccessful = true;
    
    for (FormStatus formToTransfer : formsToTransfer) {

      boolean thisFormSuccessful = true;
      
      if ( isCancelled() ) {
        formToTransfer.setStatusString("Aborted upload.", true);
        EventBus.publish(new FormStatusEvent(formToTransfer));
        return false;
      }

      String formName = formToTransfer.getFormName();
      File briefcaseFormDefFile = FileSystemUtils.getFormDefinitionFileIfExists(
          briefcaseFormsDir, formName);
      if (briefcaseFormDefFile == null) {
        formToTransfer.setStatusString("Form does not exist", true);
        EventBus.publish(new FormStatusEvent(formToTransfer));
        continue;
      }
      File briefcaseFormMediaDir = FileSystemUtils.getMediaDirectoryIfExists(
          briefcaseFormsDir, formName);

      boolean outcome;
      outcome = uploadForm(formToTransfer, briefcaseFormDefFile, briefcaseFormMediaDir);
      thisFormSuccessful = thisFormSuccessful & outcome;
      allSuccessful = allSuccessful & outcome;
          
      List<File> briefcaseInstances = FileSystemUtils.getFormSubmissionDirectories(
          briefcaseFormsDir, formName);

      URI u = getUploadSubmissionUri(formToTransfer);
      if ( u == null ) {
        // error already logged...
        continue;
      }
      
      int i = 1;
      for (File briefcaseInstance : briefcaseInstances) {
        outcome = uploadSubmission(formToTransfer, u, i++, briefcaseInstances.size(), briefcaseInstance);
        thisFormSuccessful = thisFormSuccessful & outcome;
        allSuccessful = allSuccessful & outcome;
        // and stop this loop quickly if we're cancelled...
        if ( isCancelled() ) {
          break;
        }
      }
      
      if ( isCancelled() ) {
        formToTransfer.setStatusString("Aborted upload.", true);
        EventBus.publish(new FormStatusEvent(formToTransfer));
      } else if ( thisFormSuccessful ) {
        formToTransfer.setStatusString("Successful upload!", true);
        EventBus.publish(new FormStatusEvent(formToTransfer));
      } else {
        formToTransfer.setStatusString("Partially successful upload...", true);
        EventBus.publish(new FormStatusEvent(formToTransfer));
      }
    }
    return allSuccessful;
  }
  
  public boolean uploadForm(FormStatus formToTransfer, File briefcaseFormDefFile, File briefcaseFormMediaDir) {
    // very similar to upload submissions...
  
    URI u;
    try {
      u = AggregateUtils.testServerConnectionWithHeadRequest(serverInfo, "formUpload");
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
    if ( briefcaseFormMediaDir != null ) {
      allFiles = briefcaseFormMediaDir.listFiles();
    }
  
    // clean up the list, removing anything that is suspicious
    // or that we won't attempt to upload. For OpenRosa servers,
    // we'll upload just about everything...
    List<File> files = new ArrayList<File>();
    if ( allFiles != null ) {
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
        "Form definition upload failed.", "form definition", terminationFuture);

    return AggregateUtils.uploadFilesToServer(serverInfo, u, "form_def_file", briefcaseFormDefFile, files,
                                              formDefinitionUploadDescription, null, formToTransfer);
  }

  private URI getUploadSubmissionUri(FormStatus formToTransfer) {
    URI u;
    try {
      // Get the actual server URL in u, possibly redirected to https.
      // We know we are talking to the server because the head request
      // succeeded and had a Location header field.
      u = AggregateUtils.testServerConnectionWithHeadRequest(serverInfo, "submission");
    } catch (TransmissionException e) {
      formToTransfer.setStatusString(e.getMessage(), false);
      EventBus.publish(new FormStatusEvent(formToTransfer));
      return null;
    }
    return u;
  }
  
  private final boolean uploadSubmission(FormStatus formToTransfer, URI u, int count, int totalCount, File instanceDirectory) {
  
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
    List<File> files = new ArrayList<File>();
    for (File f : allFiles) {
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
    
    if ( isCancelled() ) {
      formToTransfer.setStatusString("aborting upload of submission...", true);
      EventBus.publish(new FormStatusEvent(formToTransfer));
      return false;
    }

    DocumentDescription submissionUploadDescription = new DocumentDescription("Submission upload failed.  Detailed error: ",
        "Submission upload failed.", "submission (" + count + " of " + totalCount + ")", terminationFuture);
    boolean outcome = AggregateUtils.uploadFilesToServer(serverInfo, u, "xml_submission_file", file, files, submissionUploadDescription, action, formToTransfer);
    
    // and try to rename the instance directory to be its instanceID
    action.afterUpload(formToTransfer);
    return outcome;
  }

  public static final void testServerUploadConnection(ServerConnectionInfo serverInfo, TerminationFuture terminationFuture) throws TransmissionException {
    AggregateUtils.testServerConnectionWithHeadRequest(serverInfo, "submission");
  }

}
