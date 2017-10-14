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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.DocumentDescription;
import org.opendatakit.briefcase.model.FileSystemException;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.ParsingException;
import org.opendatakit.briefcase.model.RemoteFormDefinition;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransmissionException;
import org.opendatakit.briefcase.model.XmlDocumentFetchException;

import static org.opendatakit.briefcase.util.WebUtils.MAX_CONNECTIONS_PER_ROUTE;

public class ServerFetcher {

  private static final Log log = LogFactory.getLog(ServerFetcher.class);

  private static final String MD5_COLON_PREFIX = "md5:";

  private static final int MAX_ENTRIES = 100;

  ServerConnectionInfo serverInfo;

  private TerminationFuture terminationFuture;

  public static String SUCCESS_STATUS = "Success.";
  public static String FAILED_STATUS = "Failed.";

  public static class FormListException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = -2443850446028219296L;

    FormListException(String message) {
      super(message);
    }
  }

  ;

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

  public boolean downloadFormAndSubmissionFiles(List<FormStatus> formsToTransfer) {
    boolean allSuccessful = true;

    // boolean error = false;
    int total = formsToTransfer.size();

    for (int i = 0; i < total; i++) {
      FormStatus fs = formsToTransfer.get(i);

      if (isCancelled()) {
        fs.setStatusString("aborted. Skipping fetch of form and submissions...", true);
        EventBus.publish(new FormStatusEvent(fs));
        return false;
      }

      RemoteFormDefinition fd = (RemoteFormDefinition) fs.getFormDefinition();
      fs.setStatusString("Fetching form definition", true);
      EventBus.publish(new FormStatusEvent(fs));
      try {

        File tmpdl = FileSystemUtils.getTempFormDefinitionFile();
        AggregateUtils.commonDownloadFile(serverInfo, tmpdl, fd.getDownloadUrl());

        fs.setStatusString("resolving against briefcase form definitions", true);
        EventBus.publish(new FormStatusEvent(fs));

        boolean successful = false;
        BriefcaseFormDefinition briefcaseLfd;
        DatabaseUtils formDatabase = null;
        try {
          try {
            briefcaseLfd = BriefcaseFormDefinition.resolveAgainstBriefcaseDefn(tmpdl);
            if (briefcaseLfd.needsMediaUpdate()) {

              if (fd.getManifestUrl() != null) {
                File mediaDir = FileSystemUtils.getMediaDirectory(briefcaseLfd.getFormDirectory());
                String error = downloadManifestAndMediaFiles(mediaDir, fs);
                if (error != null) {
                  allSuccessful = false;
                  fs.setStatusString("Error fetching form definition: " + error, false);
                  EventBus.publish(new FormStatusEvent(fs));
                  continue;
                }
              }

            }
            formDatabase = DatabaseUtils.newInstance(briefcaseLfd.getFormDirectory());

          } catch (BadFormDefinition e) {
            allSuccessful = false;
            String msg = "Error parsing form definition";
            log.error(msg, e);
            fs.setStatusString(msg + ": " + e.getMessage(), false);
            EventBus.publish(new FormStatusEvent(fs));
            continue;
          }

          fs.setStatusString("preparing to retrieve instance data", true);
          EventBus.publish(new FormStatusEvent(fs));

          File formInstancesDir = FileSystemUtils.getFormInstancesDirectory(briefcaseLfd.getFormDirectory());

          // this will publish events
          successful = downloadAllSubmissionsForForm(formInstancesDir, formDatabase, briefcaseLfd, fs);
        } catch (SQLException | FileSystemException e) {
          allSuccessful = false;
          String msg = "unable to open form database";
          log.error(msg, e);
          fs.setStatusString(msg + ": " + e.getMessage(), false);
          EventBus.publish(new FormStatusEvent(fs));
          continue;
        } finally {
          if (formDatabase != null) {
            try {
              formDatabase.close();
            } catch (SQLException e) {
              allSuccessful = false;
              String msg = "unable to close form database";
              log.error(msg, e);
              fs.setStatusString(msg + ": " + e.getMessage(), false);
              EventBus.publish(new FormStatusEvent(fs));
              continue;
            }
          }
        }

        allSuccessful = allSuccessful && successful;

        // on success, we haven't actually set a success event (because we don't know we're done)
        if (successful) {
          fs.setStatusString(SUCCESS_STATUS, true);
          EventBus.publish(new FormStatusEvent(fs));
        } else {
          fs.setStatusString(FAILED_STATUS, true);
          EventBus.publish(new FormStatusEvent(fs));
        }

      } catch (SocketTimeoutException se) {
        allSuccessful = false;
        log.error("error accessing " + fd.getDownloadUrl(), se);
        fs.setStatusString("Communications to the server timed out. Detailed message: " + se.getLocalizedMessage()
            + " while accessing: " + fd.getDownloadUrl()
            + " A network login screen may be interfering with the transmission to the server.", false);
        EventBus.publish(new FormStatusEvent(fs));
      } catch (IOException e) {
        allSuccessful = false;
        log.error("error accessing " + fd.getDownloadUrl(), e);
        fs.setStatusString("Unexpected error: " + e.getLocalizedMessage() + " while accessing: " + fd.getDownloadUrl()
            + " A network login screen may be interfering with the transmission to the server.", false);
        EventBus.publish(new FormStatusEvent(fs));
      } catch (FileSystemException | TransmissionException | URISyntaxException e) {
        allSuccessful = false;
        log.error("error accessing " + fd.getDownloadUrl(), e);
        fs.setStatusString("Unexpected error: " + e.getLocalizedMessage() + " while accessing: " + fd.getDownloadUrl(),
            false);
        EventBus.publish(new FormStatusEvent(fs));
      }
    }
    return allSuccessful;
  }

  public static class SubmissionChunk {
    final String websafeCursorString;
    final List<String> uriList;

    public SubmissionChunk(List<String> uriList, String websafeCursorString) {
      this.uriList = uriList;
      this.websafeCursorString = websafeCursorString;
    }
  }

  ;

  private static class DownloadThreadFactory implements ThreadFactory {
    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

    public DownloadThreadFactory() {
      namePrefix = "briefcase-pull-" + poolNumber.getAndIncrement() + "-thread-";
    }

    public Thread newThread(Runnable r) {
      Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
      t.setPriority(Thread.MIN_PRIORITY);
      t.setDaemon(true);
      return t;
    }
  }

  private ExecutorService getFetchExecutorService() {
    int downloadThreads = BriefcasePreferences.getBriefcaseParallelPullsProperty() ? MAX_CONNECTIONS_PER_ROUTE : 1;
    return Executors.newFixedThreadPool(downloadThreads, new DownloadThreadFactory());
  }

  private boolean downloadAllSubmissionsForForm(File formInstancesDir,
                                                DatabaseUtils formDatabase,
                                                BriefcaseFormDefinition lfd,
                                                FormStatus fs) {
    int submissionCount = 1, chunkCount = 1;
    boolean allSuccessful = true;
    RemoteFormDefinition fd = (RemoteFormDefinition) fs.getFormDefinition();
    ExecutorService execSvc = getFetchExecutorService();
    CompletionService<SubmissionChunk> chunkCompleter = new ExecutorCompletionService<>(execSvc);
    CompletionService<String> submissionCompleter = new ExecutorCompletionService<>(execSvc);

    String oldWebsafeCursorString, websafeCursorString = "";

    chunkCompleter.submit(new SubmissionChunkDownload(fs, fd.getFormId(), websafeCursorString));

    boolean cursorFinished;

    try {
      do {
        if (isCancelled()) {
          fs.setStatusString("aborting fetching submission chunks...", true);
          EventBus.publish(new FormStatusEvent(fs));
          return false;
        }

        fs.setStatusString("processing chunk " + chunkCount + "...", true);
        EventBus.publish(new FormStatusEvent(fs));

        oldWebsafeCursorString = websafeCursorString; // remember what we had...
        SubmissionChunk chunk;
        try {
          chunk = chunkCompleter.take().get();
          chunkCount += 1;
          websafeCursorString = chunk.websafeCursorString;
          cursorFinished = oldWebsafeCursorString.equals(websafeCursorString);
        } catch (InterruptedException | ExecutionException e) {
          return false;
        }

        if (!cursorFinished) {
          // submit another chunk request so it's ready by the time we finish processing this chunk
          chunkCompleter.submit(new SubmissionChunkDownload(fs, fd.getFormId(), websafeCursorString));
        }

        // submit a download job for each uri in the chunk
        for (String uri : chunk.uriList) {
          if (isCancelled()) {
            fs.setStatusString("aborting requesting submissions...", true);
            EventBus.publish(new FormStatusEvent(fs));
            return false;
          }
          submissionCompleter.submit(new SubmissionDownload(formInstancesDir, formDatabase, lfd, fs, uri));
        }

        // call take() and get() exactly once for each download submitted above (we don't need the uri)
        for (int i = 0; i < chunk.uriList.size(); i++) {
          if (isCancelled()) {
            fs.setStatusString("aborting processing submissions...", true);
            EventBus.publish(new FormStatusEvent(fs));
            return false;
          }
          try {
            submissionCompleter.take().get();
            fs.setStatusString(String.format("fetched instance %s...", submissionCount++), true);
            EventBus.publish(new FormStatusEvent(fs));
          } catch (InterruptedException | ExecutionException e) {
            log.error("failure during submission download", e);
            allSuccessful = false;
            fs.setStatusString("Submission not retrieved: " + e.getMessage(), false);
            EventBus.publish(new FormStatusEvent(fs));
            // but try to get the next one...
          }
        }
      } while (!cursorFinished);
    } finally {
      execSvc.shutdown();
      try {
        execSvc.awaitTermination(1, TimeUnit.MINUTES);
      } catch (InterruptedException e) {
        log.warn("interrupted while waiting for pull to complete");
      }
    }
    return allSuccessful;
  }

  private class SubmissionChunkDownload implements Callable<SubmissionChunk> {

    private final FormStatus fs;
    private final String fullUrl;

    SubmissionChunkDownload(FormStatus fs, String formId, String cursor) {
      this.fs = fs;
      this.fullUrl = getChunkUrl(formId, cursor);
    }

    private String getChunkUrl(String formId, String cursor) {
      String baseUrl = serverInfo.getUrl() + "/view/submissionList";
      Map<String, String> params = new HashMap<>();
      params.put("numEntries", Integer.toString(MAX_ENTRIES));
      params.put("formId", formId);
      params.put("cursor", cursor);
      return WebUtils.createLinkWithProperties(baseUrl, params);
    }

    public SubmissionChunk call() throws ParsingException, XmlDocumentFetchException {
      try {
        DocumentDescription submissionChunkDescription = new DocumentDescription(
            "Fetch of submission download chunk failed.  Detailed error: ",
            "Fetch of submission download chunk failed.", "submission download chunk", terminationFuture);
        AggregateUtils.DocumentFetchResult fetchResult = AggregateUtils.getXmlDocument(fullUrl, serverInfo, false,
            submissionChunkDescription, null);
        return XmlManipulationUtils.parseSubmissionDownloadListResponse(fetchResult.doc);
      } catch (XmlDocumentFetchException e) {
        fs.setStatusString("Not all submissions retrieved: Error fetching list of submissions: " + e.getMessage(),
            false);
        EventBus.publish(new FormStatusEvent(fs));
        throw e;
      } catch (ParsingException e) {
        fs.setStatusString("Not all submissions retrieved: Error parsing the list of submissions: " + e.getMessage(),
            false);
        EventBus.publish(new FormStatusEvent(fs));
        throw e;
      }
    }
  }

  private class SubmissionDownload implements Callable<String> {

    private File formInstancesDir;
    private DatabaseUtils formDatabase;
    private BriefcaseFormDefinition lfd;
    private FormStatus fs;
    private String uri;

    SubmissionDownload(File formInstancesDir,
                       DatabaseUtils formDatabase,
                       BriefcaseFormDefinition lfd,
                       FormStatus fs,
                       String uri) {
      this.formInstancesDir = formInstancesDir;
      this.formDatabase = formDatabase;
      this.lfd = lfd;
      this.fs = fs;
      this.uri = uri;
    }

    @Override
    public String call() throws Exception {
      downloadSubmission(formInstancesDir, formDatabase, lfd, fs, uri);
      return uri;
    }
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

  private void downloadSubmission(File formInstancesDir,
                                  DatabaseUtils formDatabase,
                                  BriefcaseFormDefinition lfd,
                                  FormStatus fs,
                                  String uri) throws Exception {

    File instanceFolder = formDatabase.hasRecordedInstance(uri);
    if (instanceFolder != null) {
      //check if the submission file is present in the folder before skipping
      File instance = new File(instanceFolder, "submission.xml");
      File instanceEncrypted = new File(instanceFolder, "submission.xml.enc");
      if (instance.exists() || instanceEncrypted.exists()) {
        log.info("already present - skipping fetch: " + uri);
        return;
      }
    }

    String formId = lfd.getSubmissionKey(uri);

    if (isCancelled()) {
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
      DocumentDescription submissionDescription = new DocumentDescription(
          "Fetch of a submission failed.  Detailed error: ", "Fetch of a submission failed.", "submission",
          terminationFuture);
      result = AggregateUtils.getXmlDocument(fullUrl, serverInfo, false, submissionDescription, null);
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
    log.info(msg);

    if (FileSystemUtils.hasFormSubmissionDirectory(formInstancesDir, submissionManifest.instanceID)) {
      // create instance directory...
      File instanceDir = FileSystemUtils.assertFormSubmissionDirectory(formInstancesDir, submissionManifest.instanceID);

      // fetch attachments
      for (MediaFile m : submissionManifest.attachmentList) {
        downloadMediaFileIfChanged(instanceDir, m, fs);
      }

      // write submission file -- we rely on instanceId being unique...
      File submissionFile = new File(instanceDir, "submission.xml");
      OutputStreamWriter fo = new OutputStreamWriter(new FileOutputStream(submissionFile), "UTF-8");
      fo.write(submissionManifest.submissionXml);
      fo.close();

      // if we get here and it was a legacy server (0.9.x), we don't
      // actually know whether the submission was complete.  Otherwise,
      // if we get here, we know that this is a completed submission
      // (because it was in /view/submissionList) and that we safely
      // copied it into the storage area (because we didn't get any
      // exceptions).
      if (serverInfo.isOpenRosaServer()) {
        formDatabase.assertRecordedInstanceDirectory(uri, instanceDir);
      }
    } else {
      // create instance directory...
      File instanceDir = FileSystemUtils.assertFormSubmissionDirectory(formInstancesDir, submissionManifest.instanceID);

      // fetch attachments
      for (MediaFile m : submissionManifest.attachmentList) {
        downloadMediaFileIfChanged(instanceDir, m, fs);
      }

      // write submission file
      File submissionFile = new File(instanceDir, "submission.xml");
      OutputStreamWriter fo = new OutputStreamWriter(new FileOutputStream(submissionFile), "UTF-8");
      fo.write(submissionManifest.submissionXml);
      fo.close();

      // if we get here and it was a legacy server (0.9.x), we don't
      // actually know whether the submission was complete.  Otherwise,
      // if we get here, we know that this is a completed submission
      // (because it was in /view/submissionList) and that we safely
      // copied it into the storage area (because we didn't get any
      // exceptions).
      if (serverInfo.isOpenRosaServer()) {
        formDatabase.assertRecordedInstanceDirectory(uri, instanceDir);
      }
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
    if (fd.getManifestUrl() == null) {
      return null;
    }
    fs.setStatusString("Fetching form manifest", true);
    EventBus.publish(new FormStatusEvent(fs));

    List<MediaFile> files = new ArrayList<MediaFile>();
    AggregateUtils.DocumentFetchResult result;
    try {
      DocumentDescription formManifestDescription = new DocumentDescription(
          "Fetch of manifest failed. Detailed reason: ", "Fetch of manifest failed ", "form manifest",
          terminationFuture);
      result = AggregateUtils.getXmlDocument(fd.getManifestUrl(), serverInfo, false, formManifestDescription, null);
    } catch (XmlDocumentFetchException e) {
      return e.getMessage();
    }

    try {
      files = XmlManipulationUtils.parseFormManifestResponse(result.isOpenRosaResponse, result.doc);
    } catch (ParsingException e) {
      return e.getMessage();
    }
    // OK we now have the full set of files to download...
    log.info("Downloading " + files.size() + " media files.");
    int mCount = 0;
    if (files.size() > 0) {
      for (MediaFile m : files) {
        ++mCount;
        fs.setStatusString(String.format(" (getting %1$d of %2$d media files)", mCount, files.size()), true);
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
        if (hash.equalsIgnoreCase(hashToMatch)) {
          return;
        }
        mediaFile.delete();
      }
    }

    if (isCancelled()) {
      fs.setStatusString("aborting fetch of media file...", true);
      EventBus.publish(new FormStatusEvent(fs));
      throw new TransmissionException("Transfer cancelled by user.");
    }

    AggregateUtils.commonDownloadFile(serverInfo, mediaFile, m.downloadUrl);
  }

  public static final List<RemoteFormDefinition> retrieveAvailableFormsFromServer(ServerConnectionInfo serverInfo,
                                                                                  TerminationFuture terminationFuture)
      throws XmlDocumentFetchException, ParsingException {
    AggregateUtils.DocumentFetchResult result = fetchFormList(serverInfo, true, terminationFuture);
    List<RemoteFormDefinition> formDefs = XmlManipulationUtils.parseFormListResponse(result.isOpenRosaResponse,
        result.doc);
    return formDefs;
  }

  public static final void testServerDownloadConnection(ServerConnectionInfo serverInfo,
                                                        TerminationFuture terminationFuture)
      throws TransmissionException {
    try {
      fetchFormList(serverInfo, true, terminationFuture);
    } catch (XmlDocumentFetchException e) {
      throw new TransmissionException(e.getMessage());
    }
  }

  public static final AggregateUtils.DocumentFetchResult fetchFormList(ServerConnectionInfo serverInfo,
                                                                       boolean alwaysResetCredentials,
                                                                       TerminationFuture terminationFuture)
      throws XmlDocumentFetchException {

    String urlString = serverInfo.getUrl();
    if (urlString.endsWith("/")) {
      urlString = urlString + "formList";
    } else {
      urlString = urlString + "/formList";
    }

    DocumentDescription formListDescription = new DocumentDescription("Unable to fetch formList: ",
        "Unable to fetch formList.", "form list", terminationFuture);
    AggregateUtils.DocumentFetchResult result = AggregateUtils.getXmlDocument(urlString, serverInfo,
        alwaysResetCredentials, formListDescription, null);
    return result;
  }
}
