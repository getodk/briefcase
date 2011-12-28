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
import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.opendatakit.briefcase.model.DocumentDescription;
import org.opendatakit.briefcase.model.FileSystemException;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.LocalFormDefinition;
import org.opendatakit.briefcase.model.ParsingException;
import org.opendatakit.briefcase.model.RemoteFormDefinition;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransmissionException;
import org.opendatakit.briefcase.model.XmlDocumentFetchException;
import org.opendatakit.briefcase.util.JavaRosaWrapper.BadFormDefinition;

public class ServerFetcher {

  private static final Log logger = LogFactory.getLog(ServerFetcher.class);

  private static final String MD5_COLON_PREFIX = "md5:";

  private static final int MAX_ENTRIES = 100;

  ServerConnectionInfo serverInfo;

  private TerminationFuture terminationFuture;

  public static class FormListException extends Exception {

    /**
		 * 
		 */
    private static final long serialVersionUID = -2443850446028219296L;

    FormListException(String message) {
      super(message);
    }
  };

  public static class SubmissionListException extends Exception {

    /**
		 * 
		 */
    private static final long serialVersionUID = 8707375089373674335L;

    SubmissionListException(String message) {
      super(message);
    }
  }

  public static class SubmissionDownloadException extends Exception {

    /**
		 * 
		 */
    private static final long serialVersionUID = 8717375089373674335L;

    SubmissionDownloadException(String message) {
      super(message);
    }
  }

  public static class DownloadException extends Exception {
    /**
		 * 
		 */
    private static final long serialVersionUID = 3142210034175698950L;

    DownloadException(String message) {
      super(message);
    }
  }

  ServerFetcher(ServerConnectionInfo serverInfo, TerminationFuture future) {
    AnnotationProcessor.process(this);// if not using AOP
    this.serverInfo = serverInfo;
    this.terminationFuture = future;
  }
  
  public boolean isCancelled() {
    return terminationFuture.isCancelled();
  }

  public boolean downloadFormAndSubmissionFiles(File briefcaseFormsDir,
      List<FormStatus> formsToTransfer, boolean mustBeWellFormedXmlns) {

    boolean allSuccessful = true;
    
    // boolean error = false;
    int total = formsToTransfer.size();

    for (int i = 0; i < total; i++) {
      FormStatus fs = formsToTransfer.get(i);

      if ( isCancelled() ) {
        fs.setStatusString("aborted. Skipping fetch of form and submissions...", true);
        EventBus.publish(new FormStatusEvent(fs));
        return false;
      }
      
      RemoteFormDefinition fd = (RemoteFormDefinition) fs.getFormDefinition();
      fs.setStatusString("Fetching form definition", true);
      EventBus.publish(new FormStatusEvent(fs));
      try {

        File dl = FileSystemUtils.getFormDefinitionFile(briefcaseFormsDir, fd.getFormName());
        AggregateUtils.commonDownloadFile(serverInfo, dl, fd.getDownloadUrl());

        if (fd.getManifestUrl() != null) {
          File mediaDir = FileSystemUtils.getMediaDirectory(briefcaseFormsDir, fd.getFormName());
          String error = downloadManifestAndMediaFiles(mediaDir, fs);
          if (error != null) {
            allSuccessful = false;
            fs.setStatusString("Error fetching form definition: " + error, false);
            EventBus.publish(new FormStatusEvent(fs));
            continue;
          }
        }
        fs.setStatusString("preparing to retrieve instance data", true);
        EventBus.publish(new FormStatusEvent(fs));

        File formInstancesDir = FileSystemUtils.getFormInstancesDirectory(briefcaseFormsDir,
            fd.getFormName());

        LocalFormDefinition lfd;
        try {
          lfd = new LocalFormDefinition(dl);
        } catch (BadFormDefinition e) {
          e.printStackTrace();
          allSuccessful = false;
          fs.setStatusString("Error parsing form definition: " + e.getMessage(), false);
          EventBus.publish(new FormStatusEvent(fs));
          continue;
        }
        
        // cannot download via the scratch area if the original form definition
        // is not compatible with Aggregate 1.0.  In this case, the user must 
        // first download to their local Briefcase, modify their form definition
        // until it is compatible with Aggregate 1.0, save that modified form 
        // definition as formName.xml.revised, and then upload or do other additional
        // processing.
        if ( mustBeWellFormedXmlns && lfd.isInvalidFormXmlns() ) {
            allSuccessful = false;
            fs.setStatusString("Form definition is not compatible with Aggregate 1.0\nDownload to the Briefcase directory and manually manipulate the form definition.", false);
            EventBus.publish(new FormStatusEvent(fs));
            continue;
        }
        
        // this will publish events 
        boolean successful = downloadAllSubmissionsForForm(formInstancesDir, lfd, fs);
        allSuccessful = allSuccessful && successful; 

        // on success, we haven't actually set a success event (because we don't know we're done)
        if ( successful ) {
          fs.setStatusString("SUCCESS!", true);
          EventBus.publish(new FormStatusEvent(fs));
        } else {
          fs.setStatusString("FAILED.", true);
          EventBus.publish(new FormStatusEvent(fs));
        }
        
      } catch (SocketTimeoutException se) {
        se.printStackTrace();
        allSuccessful = false;
        fs.setStatusString("Communications to the server timed out. Detailed message: "
            + se.getLocalizedMessage() + " while accessing: " + fd.getDownloadUrl()
            + " A network login screen may be interfering with the transmission to the server.", false);
        EventBus.publish(new FormStatusEvent(fs));
        continue;
      } catch (IOException e) {
        e.printStackTrace();
        allSuccessful = false;
        fs.setStatusString("Unexpected error: " + e.getLocalizedMessage() + " while accessing: "
            + fd.getDownloadUrl()
            + " A network login screen may be interfering with the transmission to the server.", false);
        EventBus.publish(new FormStatusEvent(fs));
        continue;
      } catch (FileSystemException e) {
        e.printStackTrace();
        allSuccessful = false;
        fs.setStatusString("Unexpected error: " + e.getLocalizedMessage() + " while accessing: "
            + fd.getDownloadUrl(), false);
        EventBus.publish(new FormStatusEvent(fs));
        continue;
      } catch (URISyntaxException e) {
        e.printStackTrace();
        allSuccessful = false;
        fs.setStatusString("Unexpected error: " + e.getLocalizedMessage() + " while accessing: "
            + fd.getDownloadUrl(), false);
        EventBus.publish(new FormStatusEvent(fs));
        continue;
      } catch (TransmissionException e) {
        e.printStackTrace();
        allSuccessful = false;
        fs.setStatusString("Unexpected error: " + e.getLocalizedMessage() + " while accessing: "
            + fd.getDownloadUrl(), false);
        EventBus.publish(new FormStatusEvent(fs));
        continue;
      }
    }
    return allSuccessful;
  }

  public static class SubmissionDownloadChunk {
    final String websafeCursorString;
    final List<String> uriList;

    public SubmissionDownloadChunk(List<String> uriList, String websafeCursorString) {
      this.uriList = uriList;
      this.websafeCursorString = websafeCursorString;
    }
  };

  private boolean downloadAllSubmissionsForForm(File formInstancesDir, LocalFormDefinition lfd,
      FormStatus fs) {
    boolean allSuccessful = true;
    
    RemoteFormDefinition fd = (RemoteFormDefinition) fs.getFormDefinition();

    int count = 1;
    String baseUrl = serverInfo.getUrl() + "/view/submissionList";

    String oldWebsafeCursorString = "not-empty";
    String websafeCursorString = "";
    for (; !oldWebsafeCursorString.equals(websafeCursorString);) {
      if ( isCancelled() ) {
        fs.setStatusString("aborting fetching submissions...", true);
        EventBus.publish(new FormStatusEvent(fs));
        return false;
      }

      fs.setStatusString("retrieving next chunk of instances from server...", true);
      EventBus.publish(new FormStatusEvent(fs));

      Map<String, String> params = new HashMap<String, String>();
      params.put("numEntries", Integer.toString(MAX_ENTRIES));
      params.put("formId", fd.getFormId());
      params.put("cursor", websafeCursorString);
      String fullUrl = WebUtils.createLinkWithProperties(baseUrl, params);
      oldWebsafeCursorString = websafeCursorString; // remember what we had...
      AggregateUtils.DocumentFetchResult result;
      try {
        DocumentDescription submissionChunkDescription = new DocumentDescription("Fetch of submission download chunk failed.  Detailed error: ",
            "Fetch of submission download chunk failed.", "submission download chunk",
            terminationFuture);
        result = AggregateUtils.getXmlDocument(fullUrl, serverInfo, false, submissionChunkDescription, null);
      } catch (XmlDocumentFetchException e) {
        fs.setStatusString("NOT ALL SUBMISSIONS RETRIEVED: Error fetching list of submissions: " + e.getMessage(), false);
        EventBus.publish(new FormStatusEvent(fs));
        return false;
      }

      SubmissionDownloadChunk chunk;
      try {
        chunk = XmlManipulationUtils.parseSubmissionDownloadListResponse(result.doc);
      } catch (ParsingException e) {
        fs.setStatusString("NOT ALL SUBMISSIONS RETRIEVED: Error parsing the list of submissions: " + e.getMessage(), false);
        EventBus.publish(new FormStatusEvent(fs));
        return false;
      }
      websafeCursorString = chunk.websafeCursorString;

      for (String uri : chunk.uriList) {
        if ( isCancelled() ) {
          fs.setStatusString("aborting fetching submissions...", true);
          EventBus.publish(new FormStatusEvent(fs));
          return false;
        }
        
        try {
          fs.setStatusString("fetching instance " + count++ + " ...", true);
          EventBus.publish(new FormStatusEvent(fs));
          
          downloadSubmission(formInstancesDir, lfd, fs, uri);
        } catch (Exception e) {
          e.printStackTrace();
          allSuccessful = false;
          fs.setStatusString("SUBMISSION NOT RETRIEVED: Error fetching submission uri: " + uri + " details: " + e.getMessage(), false);
          EventBus.publish(new FormStatusEvent(fs));
          // but try to get the next one...
        }
      }
    }
    return allSuccessful;
  }

  public static class SubmissionManifest {
    final List<MediaFile> attachmentList;
    final String submissionXml;
    final String instanceID;

    SubmissionManifest(String instanceID, String submissionXml, List<MediaFile> attachmentList) {
      this.instanceID = instanceID;
      this.submissionXml = submissionXml;
      this.attachmentList = attachmentList;
    }
  }

  private void downloadSubmission(File formInstancesDir, LocalFormDefinition lfd, FormStatus fs,
      String uri) throws Exception {
    String formId = lfd.getSubmissionKey(uri);
    
    if ( isCancelled() ) {
      fs.setStatusString("aborting fetch of submission...", true);
      EventBus.publish(new FormStatusEvent(fs));
      throw new SubmissionDownloadException("Transfer cancelled by user.");
    }

    String baseUrl = serverInfo.getUrl() + "/view/downloadSubmission";

    Map<String, String> params = new HashMap<String, String>();
    params.put("formId", formId);
    String fullUrl = WebUtils.createLinkWithProperties(baseUrl, params);
    AggregateUtils.DocumentFetchResult result;
    try {
      DocumentDescription submissionDescription = new DocumentDescription("Fetch of a submission failed.  Detailed error: ", 
          "Fetch of a submission failed.", "submission", terminationFuture);
      result = AggregateUtils.getXmlDocument(fullUrl, serverInfo, false,
          submissionDescription, null);
    } catch (XmlDocumentFetchException e) {
      throw new SubmissionDownloadException(e.getMessage());
    }

    // and parse the document...
    SubmissionManifest submissionManifest;
    try {
      submissionManifest = XmlManipulationUtils.parseDownloadSubmissionResponse(result.doc);
    } catch (ParsingException e) {
      throw new SubmissionDownloadException(e.getMessage());
    }

    String msg = "Fetched instanceID=" + submissionManifest.instanceID;
    logger.info(msg);

    if ( FileSystemUtils.hasFormSubmissionDirectory(formInstancesDir, submissionManifest.instanceID)) {
      // create instance directory...
      File instanceDir = FileSystemUtils.assertFormSubmissionDirectory(formInstancesDir,
          submissionManifest.instanceID);
  
      // fetch attachments
      for (MediaFile m : submissionManifest.attachmentList) {
        downloadMediaFileIfChanged(instanceDir, m, fs);
      }
      
      // write submission file -- we rely on instanceId being unique...
      File submissionFile = new File(instanceDir, "submission.xml");
      FileWriter fo = new FileWriter(submissionFile);
      fo.write(submissionManifest.submissionXml);
      fo.close();
      
    } else {
      // create instance directory...
      File instanceDir = FileSystemUtils.assertFormSubmissionDirectory(formInstancesDir,
          submissionManifest.instanceID);
  
      // fetch attachments
      for (MediaFile m : submissionManifest.attachmentList) {
        downloadMediaFileIfChanged(instanceDir, m, fs);
      }
  
      // write submission file
      File submissionFile = new File(instanceDir, "submission.xml");
      FileWriter fo = new FileWriter(submissionFile);
      fo.write(submissionManifest.submissionXml);
      fo.close();
    }
  }

  public static class MediaFile {
    final String filename;
    final String hash;
    final String downloadUrl;

    MediaFile(String filename, String hash, String downloadUrl) {
      this.filename = filename;
      this.hash = hash;
      this.downloadUrl = downloadUrl;
    }
  }

  private String downloadManifestAndMediaFiles(File mediaDir, FormStatus fs) {
    RemoteFormDefinition fd = (RemoteFormDefinition) fs.getFormDefinition();
    if (fd.getManifestUrl() == null)
      return null;
    fs.setStatusString("Fetching form manifest", true);
    EventBus.publish(new FormStatusEvent(fs));

    List<MediaFile> files = new ArrayList<MediaFile>();
    AggregateUtils.DocumentFetchResult result;
    try {
      DocumentDescription formManifestDescription = new DocumentDescription("Fetch of manifest failed. Detailed reason: ", 
          "Fetch of manifest failed ", "form manifest", terminationFuture);
      result = AggregateUtils.getXmlDocument(fd.getManifestUrl(), serverInfo, false,
          formManifestDescription, null);
    } catch (XmlDocumentFetchException e) {
      return e.getMessage();
    }

    try {
      files = XmlManipulationUtils.parseFormManifestResponse(result.isOpenRosaResponse, result.doc);
    } catch (ParsingException e) {
      return e.getMessage();
    }
    // OK we now have the full set of files to download...
    logger.info("Downloading " + files.size() + " media files.");
    int mCount = 0;
    if (files.size() > 0) {
      for (MediaFile m : files) {
        ++mCount;
        fs.setStatusString(String.format(" (getting %1$d of %2$d media files)", mCount,
            files.size()), true);
        EventBus.publish(new FormStatusEvent(fs));
        try {
          downloadMediaFileIfChanged(mediaDir, m, fs);
        } catch (Exception e) {
          return e.getLocalizedMessage();
        }
      }
    }
    return null;
  }

  private void downloadMediaFileIfChanged(File mediaDir, MediaFile m, FormStatus fs) throws Exception {

    File mediaFile = new File(mediaDir, m.filename);

    if (m.hash.startsWith(MD5_COLON_PREFIX)) {
      // see if the file exists and has the same hash
      String hashToMatch = m.hash.substring(MD5_COLON_PREFIX.length());
      if (mediaFile.exists()) {
        String hash = FileSystemUtils.getMd5Hash(mediaFile);
        if (hash.equalsIgnoreCase(hashToMatch))
          return;
        mediaFile.delete();
      }
    }
    
    if ( isCancelled() ) {
      fs.setStatusString("aborting fetch of media file...", true);
      EventBus.publish(new FormStatusEvent(fs));
      throw new TransmissionException("Transfer cancelled by user.");
    }

    AggregateUtils.commonDownloadFile(serverInfo, mediaFile, m.downloadUrl);
  }

  public static final List<RemoteFormDefinition> retrieveAvailableFormsFromServer(ServerConnectionInfo serverInfo, TerminationFuture terminationFuture) throws XmlDocumentFetchException, ParsingException {
    AggregateUtils.DocumentFetchResult result = fetchFormList(serverInfo, true, terminationFuture);
    List<RemoteFormDefinition> formDefs = XmlManipulationUtils.parseFormListResponse(result.isOpenRosaResponse, result.doc);
    return formDefs;
  }

  public static final void testServerDownloadConnection(ServerConnectionInfo serverInfo, TerminationFuture terminationFuture) throws TransmissionException {
    try {
      fetchFormList(serverInfo, true, terminationFuture);
    } catch (XmlDocumentFetchException e) {
      throw new TransmissionException(e.getMessage());
    }
  }

  public static final AggregateUtils.DocumentFetchResult fetchFormList(ServerConnectionInfo serverInfo,
      boolean alwaysResetCredentials, TerminationFuture terminationFuture) throws XmlDocumentFetchException {
  
    String urlString = serverInfo.getUrl();
    if (urlString.endsWith("/")) {
      urlString = urlString + "formList";
    } else {
      urlString = urlString + "/formList";
    }
  
    DocumentDescription formListDescription = 
      new DocumentDescription("Unable to fetch formList: ", 
                             "Unable to fetch formList.", "form list",
                             terminationFuture);
    AggregateUtils.DocumentFetchResult result = AggregateUtils.getXmlDocument(urlString, serverInfo, alwaysResetCredentials,
                                                                                  formListDescription, null);
    return result;
  }
}
