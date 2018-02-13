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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.DocumentDescription;
import org.opendatakit.briefcase.model.FileSystemException;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.MetadataUpdateException;
import org.opendatakit.briefcase.model.ParsingException;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransmissionException;
import org.opendatakit.briefcase.model.XmlDocumentFetchException;
import org.opendatakit.briefcase.util.AggregateUtils.DocumentFetchResult;
import org.opendatakit.briefcase.util.ServerFetcher.SubmissionChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerUploader {

  private static final Logger log = LoggerFactory.getLogger(ServerUploader.class);
  
  private final int MAX_ENTRIES = 100;

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
          String msg = "Submission directory could not be renamed: " + instanceDir.getName();
          log.warn(msg, e);
          formToTransfer.setStatusString("WARNING: " + msg, true);
          EventBus.publish(new FormStatusEvent(formToTransfer));
        }
      }
    }
  }
  
  // remove any instances already completed on server
  private void subtractServerInstances(FormStatus fs, DatabaseUtils formDatabase, Set<File> instancesToUpload) {

    /*
     * The /view/submissionList interface returns the list of COMPLETED submissions
     * on the server. Fetch this list and filter out the locally-held submissions 
     * with the same instanceIds.  We know the server is already content with what 
     * it has, so we don't need to send any of these to the server, as that POST
     * request will be treated as a no-op.
     */
    String baseUrl = serverInfo.getUrl() + "/view/submissionList";

    String oldWebsafeCursorString = "not-empty";
    String websafeCursorString = "";
    for (; !oldWebsafeCursorString.equals(websafeCursorString);) {
      if ( isCancelled() ) {
        fs.setStatusString("aborting retrieval of instanceIds of submissions on server...", true);
        EventBus.publish(new FormStatusEvent(fs));
        return;
      }

      fs.setStatusString("retrieving next chunk of instanceIds from server...", true);
      EventBus.publish(new FormStatusEvent(fs));

      Map<String, String> params = new HashMap<String, String>();
      params.put("numEntries", Integer.toString(MAX_ENTRIES));
      params.put("formId", fs.getFormDefinition().getFormId());
      params.put("cursor", websafeCursorString);
      String fullUrl = WebUtils.createLinkWithProperties(baseUrl, params);
      oldWebsafeCursorString = websafeCursorString; // remember what we had...
      AggregateUtils.DocumentFetchResult result;
      try {
        DocumentDescription submissionChunkDescription = new DocumentDescription(
            "Fetch of instanceIds (submission download chunk) failed.  Detailed error: ",
            "Fetch of instanceIds (submission download chunk) failed.", "submission download chunk",
            terminationFuture);
        result = AggregateUtils.getXmlDocument(fullUrl, serverInfo, false, submissionChunkDescription, null);
      } catch (XmlDocumentFetchException e) {
        fs.setStatusString("Not all submissions retrieved: Error fetching list of instanceIds: " + e.getMessage(), false);
        EventBus.publish(new FormStatusEvent(fs));
        return;
      }

      SubmissionChunk chunk;
      try {
        chunk = XmlManipulationUtils.parseSubmissionDownloadListResponse(result.doc);
      } catch (ParsingException e) {
        fs.setStatusString("Not all instanceIds retrieved: Error parsing the submission download chunk: " + e.getMessage(), false);
        EventBus.publish(new FormStatusEvent(fs));
        return;
      }
      websafeCursorString = chunk.websafeCursorString;

      for (String uri : chunk.uriList) {
        File f = formDatabase.hasRecordedInstance(uri);
        if ( f != null ) {
          instancesToUpload.remove(f);
        }
      }
    }
  }

  public boolean uploadFormAndSubmissionFiles(List<FormStatus> formsToTransfer) {

    boolean allSuccessful = true;

    for (FormStatus formToTransfer : formsToTransfer) {

      BriefcaseFormDefinition briefcaseLfd = (BriefcaseFormDefinition) formToTransfer.getFormDefinition();
      boolean thisFormSuccessful = true;
      
      if ( isCancelled() ) {
        formToTransfer.setStatusString("Aborted upload.", true);
        EventBus.publish(new FormStatusEvent(formToTransfer));
        return false;
      }

      if ( !formToTransfer.isSuccessful() ) {
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
      outcome = uploadForm(formToTransfer, briefcaseFormDefFile, briefcaseFormMediaDir);
      thisFormSuccessful = thisFormSuccessful & outcome;
      allSuccessful = allSuccessful & outcome;

      URI u = getUploadSubmissionUri(formToTransfer);
      if ( u == null ) {
        // error already logged...
        continue;
      }

      Set<File> briefcaseInstances = FileSystemUtils.getFormSubmissionDirectories(briefcaseLfd.getFormDirectory());
      DatabaseUtils formDatabase = null;
      try {
        formDatabase = DatabaseUtils.newInstance(briefcaseLfd.getFormDirectory());
        
        // make sure all the local instances are in the database...
        formDatabase.updateInstanceLists(briefcaseInstances);

        // exclude submissions the server reported as already submitted
        subtractServerInstances(formToTransfer, formDatabase, briefcaseInstances);

        int i = 1;
        for (File briefcaseInstance : briefcaseInstances) {
          outcome = uploadSubmission(formDatabase, formToTransfer, u, i++, briefcaseInstances.size(), briefcaseInstance);
          thisFormSuccessful = thisFormSuccessful & outcome;
          allSuccessful = allSuccessful & outcome;
          // and stop this loop quickly if we're cancelled...
          if ( isCancelled() ) {
            break;
          }
        }
      } catch ( SQLException | FileSystemException e ) {
        thisFormSuccessful = false;
        allSuccessful = false;
        String msg = "unable to open form database";
        log.error(msg, e);
        formToTransfer.setStatusString(msg + ": " + e.getMessage(), false);
        EventBus.publish(new FormStatusEvent(formToTransfer));
      } finally {
        if ( formDatabase != null ) {
          try {
            formDatabase.close();
          } catch ( SQLException e ) {
            thisFormSuccessful = false;
            allSuccessful = false;
            String msg = "unable to close form database";
            log.warn(msg, e);
            formToTransfer.setStatusString(msg + ": " + e.getMessage(), false);
            EventBus.publish(new FormStatusEvent(formToTransfer));
          }
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
                                              formDefinitionUploadDescription, null, terminationFuture, formToTransfer);
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
  
  private final boolean uploadSubmission(DatabaseUtils formDatabase, FormStatus formToTransfer, URI u, int count, int totalCount, File instanceDirectory) {
  
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
    boolean outcome = AggregateUtils.uploadFilesToServer(serverInfo, u, "xml_submission_file", file, files,
        submissionUploadDescription, action, terminationFuture, formToTransfer);

    // and try to rename the instance directory to be its instanceID
    action.afterUpload(formToTransfer);
    return outcome;
  }

  public static final void testServerUploadConnection(ServerConnectionInfo serverInfo, TerminationFuture terminationFuture) throws TransmissionException {
    AggregateUtils.testServerConnectionWithHeadRequest(serverInfo, "upload"); // for form upload...
  }

}
