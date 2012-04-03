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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Logger;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.protocol.HttpContext;
import org.bushe.swing.event.EventBus;
import org.kxml2.io.KXmlParser;
import org.kxml2.kdom.Document;
import org.opendatakit.briefcase.model.DocumentDescription;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.MetadataUpdateException;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransmissionException;
import org.opendatakit.briefcase.model.XmlDocumentFetchException;
import org.opendatakit.briefcase.util.ServerUploader.SubmissionResponseAction;
import org.xmlpull.v1.XmlPullParser;

public class AggregateUtils {

  private static final String BRIEFCASE_APP_TOKEN_PARAMETER = "briefcaseAppToken";

  private static final int SERVER_CONNECTION_TIMEOUT = 60000;

  static final Logger log = Logger.getLogger(AggregateUtils.class.getName());

  private static final CharSequence HTTP_CONTENT_TYPE_TEXT_XML = "text/xml";

  private static final CharSequence HTTP_CONTENT_TYPE_APPLICATION_XML = "application/xml";

  private static final String FETCH_FAILED_DETAILED_REASON = "Fetch of %1$s failed. Detailed reason: ";

  public static interface ResponseAction {
    void doAction(DocumentFetchResult result) throws MetadataUpdateException;
  }

  public static class DocumentFetchResult {
    public final Document doc;
    public final boolean isOpenRosaResponse;

    DocumentFetchResult(Document doc, boolean isOpenRosaResponse) {
      this.doc = doc;
      this.isOpenRosaResponse = isOpenRosaResponse;
    }
  }

  /**
   * Common routine to download a document from the downloadUrl and save the
   * contents in the file 'f'. Shared by media file download and form file
   * download.
   * 
   * @param f
   * @param downloadUrl
   * @throws URISyntaxException
   * @throws IOException
   * @throws ClientProtocolException
   * @throws TransmissionException
   */
  public static final void commonDownloadFile(ServerConnectionInfo serverInfo, File f,
      String downloadUrl) throws URISyntaxException, ClientProtocolException, IOException,
      TransmissionException {

    // OK. We need to download it because we either:
    // (1) don't have it
    // (2) don't know if it is changed because the hash is not md5
    // (3) know it is changed
    URI u = null;
    try {
      URL uurl = new URL(downloadUrl);
      u = uurl.toURI();
    } catch (MalformedURLException e) {
      e.printStackTrace();
      throw e;
    } catch (URISyntaxException e) {
      e.printStackTrace();
      throw e;
    }

    // get shared HttpContext so that authentication and cookies are retained.
    HttpContext localContext = serverInfo.getHttpContext();

    HttpClient httpclient = serverInfo.getHttpClient();

    // set up request...
    HttpGet req = WebUtils.createOpenRosaHttpGet(u);

    if (serverInfo.getUsername() != null && serverInfo.getUsername().length() != 0) {
      if (!WebUtils.hasCredentials(localContext, serverInfo.getUsername(), u.getHost())) {
        WebUtils.clearAllCredentials(localContext);
        WebUtils.addCredentials(localContext, serverInfo.getUsername(), serverInfo.getPassword(),
            u.getHost());
      }
    } else {
      WebUtils.clearAllCredentials(localContext);
    }

    if (!serverInfo.isOpenRosaServer()) {
      req.addHeader(BRIEFCASE_APP_TOKEN_PARAMETER, serverInfo.getToken());
    }

    HttpResponse response = null;
    // try
    {
      response = httpclient.execute(req, localContext);
      int statusCode = response.getStatusLine().getStatusCode();

      if (statusCode != 200) {
        String errMsg = String.format(FETCH_FAILED_DETAILED_REASON, f.getAbsolutePath())
            + response.getStatusLine().getReasonPhrase() + " (" + statusCode + ")";
        log.severe(errMsg);
        throw new TransmissionException(errMsg);
      }

      // write connection to file
      InputStream is = null;
      OutputStream os = null;
      try {
        is = response.getEntity().getContent();
        os = new FileOutputStream(f);
        byte buf[] = new byte[1024];
        int len;
        while ((len = is.read(buf)) > 0) {
          os.write(buf, 0, len);
        }
        os.flush();
      } finally {
        if (os != null) {
          try {
            os.close();
          } catch (Exception e) {
          }
        }
        if (is != null) {
          try {
            is.close();
          } catch (Exception e) {
          }
        }
      }

    }
  }

  /**
   * Common method for returning a parsed xml document given a url and the http
   * context and client objects involved in the web connection. The document is
   * the body of the response entity and should be xml.
   * 
   * @param urlString
   * @param localContext
   * @param httpclient
   * @return
   */
  public static final DocumentFetchResult getXmlDocument(String urlString,
      ServerConnectionInfo serverInfo, boolean alwaysResetCredentials, 
      DocumentDescription description, ResponseAction action)
      throws XmlDocumentFetchException {

    URI u = null;
    try {
      URL url = new URL(urlString);
      u = url.toURI();
    } catch (MalformedURLException e) {
      e.printStackTrace();
      String msg = description.getFetchDocFailed() + "Invalid url: " + urlString + ".\nFailed with error: "
          + e.getMessage();
      log.severe(msg);
      throw new XmlDocumentFetchException(msg);
    } catch (URISyntaxException e) {
      e.printStackTrace();
      String msg = description.getFetchDocFailed() + "Invalid uri: " + urlString + ".\nFailed with error: "
          + e.getMessage();
      log.severe(msg);
      throw new XmlDocumentFetchException(msg);
    }

    HttpClient httpClient = serverInfo.getHttpClient();
    if (httpClient == null) {
      httpClient = WebUtils.createHttpClient(SERVER_CONNECTION_TIMEOUT);
      serverInfo.setHttpClient(httpClient);
    }

    // get shared HttpContext so that authentication and cookies are retained.
    HttpContext localContext = serverInfo.getHttpContext();
    if (localContext == null) {
      localContext = WebUtils.createHttpContext();
      serverInfo.setHttpContext(localContext);
    }

    // set up request...
    HttpGet req = WebUtils.createOpenRosaHttpGet(u);

    int[] validStatusList = { 200 };

    return httpRetrieveXmlDocument(req, validStatusList, serverInfo, alwaysResetCredentials,
        description, action);
  }

  /**
   * Common method for returning a parsed xml document given a url and the http
   * context and client objects involved in the web connection. The document is
   * the body of the response entity and should be xml.
   * 
   * @param urlString
   * @param localContext
   * @param httpclient
   * @return
   */
  private static final DocumentFetchResult httpRetrieveXmlDocument(HttpUriRequest request,
      int[] validStatusList, ServerConnectionInfo serverInfo, boolean alwaysResetCredentials,
      DocumentDescription description, 
      ResponseAction action) throws XmlDocumentFetchException {

    HttpClient httpClient = serverInfo.getHttpClient();

    // get shared HttpContext so that authentication and cookies are retained.
    HttpContext localContext = serverInfo.getHttpContext();

    URI u = request.getURI();

    if (serverInfo.getUsername() != null && serverInfo.getUsername().length() != 0) {
      if (alwaysResetCredentials
          || !WebUtils.hasCredentials(localContext, serverInfo.getUsername(), u.getHost())) {
        WebUtils.clearAllCredentials(localContext);
        WebUtils.addCredentials(localContext, serverInfo.getUsername(), serverInfo.getPassword(),
            u.getHost());
      }
    } else {
      WebUtils.clearAllCredentials(localContext);
    }

    if (!serverInfo.isOpenRosaServer()) {
      request.addHeader(BRIEFCASE_APP_TOKEN_PARAMETER, serverInfo.getToken());
    }

    if ( description.isCancelled() ) {
      throw new XmlDocumentFetchException("Transfer of " + description.getDocumentDescriptionType() + " aborted.");
    }
    
    HttpResponse response = null;
    try {
      response = httpClient.execute(request, localContext);
      int statusCode = response.getStatusLine().getStatusCode();

      HttpEntity entity = response.getEntity();
      String lcContentType = (entity == null) ? null : entity.getContentType().getValue()
          .toLowerCase();

      XmlDocumentFetchException ex = null;
      boolean statusCodeValid = false;
      for (int i : validStatusList) {
        if (i == statusCode) {
          statusCodeValid = true;
          break;
        }
      }
      // if anything is amiss, ex will be non-null after this cascade.

      if (!statusCodeValid) {
        String webError = response.getStatusLine().getReasonPhrase() + " (" + statusCode + ")";

        if (statusCode == 400) {
          ex = new XmlDocumentFetchException(description.getFetchDocFailed() + webError + " while accessing: "
              + u.toString() + "\nPlease verify that the " + description.getDocumentDescriptionType()
              + " that is being uploaded is well-formed.");
        } else {
          ex = new XmlDocumentFetchException(
              description.getFetchDocFailed()
                  + webError
                  + " while accessing: "
                  + u.toString()
                  + "\nPlease verify that the URL, your user credentials and your permissions are all correct.");
        }
      } else if (entity == null) {
        log.severe("No entity body returned from: " + u.toString() + " is not text/xml");
        ex = new XmlDocumentFetchException(description.getFetchDocFailed()
            + " Server unexpectedly returned no content while accessing: " + u.toString());
      } else if (!(lcContentType.contains(HTTP_CONTENT_TYPE_TEXT_XML) || lcContentType
          .contains(HTTP_CONTENT_TYPE_APPLICATION_XML))) {
        log.severe("ContentType: " + entity.getContentType().getValue() + "returned from: "
            + u.toString() + " is not text/xml");
        ex = new XmlDocumentFetchException(description.getFetchDocFailed()
            + "A non-XML document was returned while accessing: " + u.toString()
            + "\nA network login screen may be interfering with the transmission to the server.");
      }

      if (ex != null) {
        if (entity != null) {
          // something is amiss -- read and discard any response body.
          try {
            // don't really care about the stream...
            InputStream is = response.getEntity().getContent();
            // read to end of stream...
            final long count = 1024L;
            while (is.skip(count) == count)
              ;
            is.close();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        // and throw the exception...
        throw ex;
      }

      // parse the xml document...
      Document doc = null;
      try {
        InputStream is = null;
        InputStreamReader isr = null;
        try {
          is = entity.getContent();
          isr = new InputStreamReader(is, "UTF-8");
          doc = new Document();
          KXmlParser parser = new KXmlParser();
          parser.setInput(isr);
          parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
          doc.parse(parser);
          isr.close();
        } finally {
          if (isr != null) {
            try {
              isr.close();
            } catch (Exception e) {
              // no-op
            }
          }
          if (is != null) {
            try {
              is.close();
            } catch (Exception e) {
              // no-op
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        log.severe("Parsing failed with " + e.getMessage());
        throw new XmlDocumentFetchException(description.getFetchDocFailed() + " while accessing: " + u.toString());
      }

      // examine header fields...

      // is it an OpenRosa server?
      boolean isOR = false;
      Header[] fields = response.getHeaders(WebUtils.OPEN_ROSA_VERSION_HEADER);
      if (fields != null && fields.length >= 1) {
        isOR = true;
        boolean versionMatch = false;
        boolean first = true;
        StringBuilder b = new StringBuilder();
        for (Header h : fields) {
          if (WebUtils.OPEN_ROSA_VERSION.equals(h.getValue())) {
            versionMatch = true;
            break;
          }
          if (!first) {
            b.append("; ");
          }
          first = false;
          b.append(h.getValue());
        }
        if (!versionMatch) {
          log.warning(WebUtils.OPEN_ROSA_VERSION_HEADER + " unrecognized version(s): "
              + b.toString());
        }
      }

      // what about location?
      Header[] locations = response.getHeaders("Location");
      if (locations != null && locations.length == 1) {
        try {
          URL url = new URL(locations[0].getValue());
          URI uNew = url.toURI();
          if (u.getHost().equalsIgnoreCase(uNew.getHost())) {
            // trust the server to tell us a new location
            // ... and possibly to use https instead.
            String fullUrl = url.toExternalForm();
            int idx = fullUrl.lastIndexOf("/");
            serverInfo.setUrl(fullUrl.substring(0, idx));
          } else {
            // Don't follow a redirection attempt to a different host.
            // We can't tell if this is a spoof or not.
            String msg = description.getFetchDocFailed() + "Unexpected redirection attempt to a different host: "
                + uNew.toString();
            log.severe(msg);
            throw new XmlDocumentFetchException(msg);
          }
        } catch (MalformedURLException e) {
          e.printStackTrace();
          String msg = description.getFetchDocFailed() + "Unexpected exception: " + e.getMessage();
          log.severe(msg);
          throw new XmlDocumentFetchException(msg);
        } catch (URISyntaxException e) {
          e.printStackTrace();
          String msg = description.getFetchDocFailed() + "Unexpected exception: " + e.getMessage();
          log.severe(msg);
          throw new XmlDocumentFetchException(msg);
        }
      }
      DocumentFetchResult result = new DocumentFetchResult(doc, isOR);
      if (action != null) {
        action.doAction(result);
      }
      return result;
    } catch (ClientProtocolException e) {
      e.printStackTrace();
      String msg = description.getFetchDocFailed() + "Unexpected exception: " + e.getMessage();
      log.severe(msg);
      throw new XmlDocumentFetchException(msg);
    } catch (IOException e) {
      e.printStackTrace();
      String msg;
      if (e instanceof UnknownHostException) {
        msg = description.getFetchDocFailed() + "Unknown host: " + e.getMessage();
      } else {
        msg = description.getFetchDocFailed() + "Unexpected " + e.getClass().getName() + ": " + e.getMessage();
      }
      log.severe(msg);
      throw new XmlDocumentFetchException(msg);
    } catch (MetadataUpdateException e) {
      e.printStackTrace();
      String msg = description.getFetchDocFailed() + "Unexpected exception: " + e.getMessage();
      log.severe(msg);
      throw new XmlDocumentFetchException(msg);
    }
  }

  /**
   * Send a HEAD request to the server to confirm the validity of the URL and
   * credentials.
   * 
   * @param serverInfo
   * @param actionAddr
   * @return the confirmed URI of this action.
   * @throws TransmissionException
   */
  public static final URI testServerConnectionWithHeadRequest(ServerConnectionInfo serverInfo,
      String actionAddr) throws TransmissionException {

    String urlString = serverInfo.getUrl();
    if (urlString.endsWith("/")) {
      urlString = urlString + actionAddr;
    } else {
      urlString = urlString + "/" + actionAddr;
    }

    URI u;
    try {
      URL url = new URL(urlString);
      u = url.toURI();
    } catch (MalformedURLException e) {
      e.printStackTrace();
      String msg = "Invalid url: " + urlString + " for " + actionAddr + ".\nFailed with error: "
          + e.getMessage();
      log.severe(msg);
      throw new TransmissionException(msg);
    } catch (URISyntaxException e) {
      e.printStackTrace();
      String msg = "Invalid uri: " + urlString + " for " + actionAddr + ".\nFailed with error: "
          + e.getMessage();
      log.severe(msg);
      throw new TransmissionException(msg);
    }

    HttpClient httpClient = serverInfo.getHttpClient();
    if (httpClient == null) {
      httpClient = WebUtils.createHttpClient(SERVER_CONNECTION_TIMEOUT);
      serverInfo.setHttpClient(httpClient);
    }

    // get shared HttpContext so that authentication and cookies are retained.
    HttpContext localContext = serverInfo.getHttpContext();
    if (localContext == null) {
      localContext = WebUtils.createHttpContext();
      serverInfo.setHttpContext(localContext);
    }

    if (serverInfo.getUsername() != null && serverInfo.getUsername().length() != 0) {
      if (!WebUtils.hasCredentials(localContext, serverInfo.getUsername(), u.getHost())) {
        WebUtils.clearAllCredentials(localContext);
        WebUtils.addCredentials(localContext, serverInfo.getUsername(), serverInfo.getPassword(),
            u.getHost());
      }
    } else {
      WebUtils.clearAllCredentials(localContext);
    }

    {
      // we need to issue a head request
      HttpHead httpHead = WebUtils.createOpenRosaHttpHead(u);

      if (!serverInfo.isOpenRosaServer()) {
        httpHead.addHeader(BRIEFCASE_APP_TOKEN_PARAMETER, serverInfo.getToken());
      }

      // prepare response
      HttpResponse response = null;
      try {
        response = httpClient.execute(httpHead, localContext);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 204) {
          Header[] openRosaVersions = response.getHeaders(WebUtils.OPEN_ROSA_VERSION_HEADER);
          if (openRosaVersions != null && openRosaVersions.length != 0) {
            if (!serverInfo.isOpenRosaServer()) {
              String msg = "Url: " + u.toString()
                  + " is for an ODK Aggregate 1.0 or higher (OpenRosa compliant) server!";
              log.severe(msg);
              throw new TransmissionException(msg);
            }
          } else if (serverInfo.isOpenRosaServer()) {
            String msg = "Url: " + u.toString()
                + " is for an ODK Aggregate 0.9x or earlier (non-OpenRosa compliant) server!";
            log.severe(msg);
            throw new TransmissionException(msg);
          }
          Header[] locations = response.getHeaders("Location");
          if (locations != null && locations.length == 1) {
            try {
              URL url = new URL(locations[0].getValue());
              URI uNew = url.toURI();
              if (u.getHost().equalsIgnoreCase(uNew.getHost())) {
                // trust the server to tell us a new location
                // ... and possibly to use https instead.
                u = uNew;
                // At this point, we may have updated the uri to use https.
                // This occurs only if the Location header keeps the host name
                // the same. If it specifies a different host name, we error
                // out.
                return u;
              } else {
                // Don't follow a redirection attempt to a different host.
                // We can't tell if this is a spoof or not.
                String msg = "Starting url: " + u.toString()
                    + " unexpected redirection attempt to a different host: " + uNew.toString();
                log.severe(msg);
                throw new TransmissionException(msg);
              }
            } catch (Exception e) {
              e.printStackTrace();
              String msg = "Starting url: " + u.toString() + " unexpected exception: "
                  + e.getLocalizedMessage();
              log.severe(msg);
              throw new TransmissionException(msg);
            }
          } else {
            String msg = "The url: " + u.toString()
                + " is not Aggregate 1.0 - status code on Head request: " + statusCode;
            log.severe(msg);
            throw new TransmissionException(msg);
          }
        } else {
          // may be a server that does not handle HEAD requests
          if (response.getEntity() != null) {
            try {
              // don't really care about the stream...
              InputStream is = response.getEntity().getContent();
              // read to end of stream...
              final long count = 1024L;
              while (is.skip(count) == count)
                ;
              is.close();
            } catch (IOException e) {
              e.printStackTrace();
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
          String msg = "The username or password may be incorrect or the url: " + u.toString()
              + " is not Aggregate 1.0 - status code on Head request: " + statusCode;
          log.severe(msg);
          throw new TransmissionException(msg);
        }
      } catch (ClientProtocolException e) {
        e.printStackTrace();
        String msg = "Starting url: " + u.toString() + " unexpected exception: "
            + e.getLocalizedMessage();
        log.severe(msg);
        throw new TransmissionException(msg);
      } catch (Exception e) {
        e.printStackTrace();
        String msg = "Starting url: " + u.toString() + " unexpected exception: "
            + e.getLocalizedMessage();
        log.severe(msg);
        throw new TransmissionException(msg);
      }
    }
  }

  public static final boolean uploadFilesToServer(ServerConnectionInfo serverInfo, URI u,
      String distinguishedFileTagName, File file, List<File> files, DocumentDescription description,
      SubmissionResponseAction action, TerminationFuture terminationFuture, FormStatus formToTransfer) {

    boolean allSuccessful = true;
    formToTransfer.setStatusString("Preparing for upload of " + description.getDocumentDescriptionType() + " with "
        + files.size() + " media attachments", true);
    EventBus.publish(new FormStatusEvent(formToTransfer));

    boolean first = true; // handles case where there are no media files
    int lastJ = 0;
    int j = 0;
    while (j < files.size() || first) {
      lastJ = j;
      first = false;

      if ( terminationFuture.isCancelled() ) {
        formToTransfer.setStatusString("Aborting upload of " + description.getDocumentDescriptionType() + " with "
            + files.size() + " media attachments", true);
        EventBus.publish(new FormStatusEvent(formToTransfer));
        return false;
      }
      
      HttpPost httppost = WebUtils.createOpenRosaHttpPost(u);

      long byteCount = 0L;

      // mime post
      MultipartEntity entity = new MultipartEntity();

      // add the submission file first...
      FileBody fb = new FileBody(file, "text/xml");
      entity.addPart(distinguishedFileTagName, fb);
      log.info("added " + distinguishedFileTagName + ": " + file.getName());
      byteCount += file.length();

      for (; j < files.size(); j++) {
        File f = files.get(j);
        String fileName = f.getName();
        int idx = fileName.lastIndexOf(".");
        String extension = "";
        if (idx != -1) {
          extension = fileName.substring(idx + 1);
        }

        // we will be processing every one of these, so
        // we only need to deal with the content type determination...
        if (extension.equals("xml")) {
          fb = new FileBody(f, "text/xml");
          entity.addPart(f.getName(), fb);
          byteCount += f.length();
          log.info("added xml file " + f.getName());
        } else if (extension.equals("jpg")) {
          fb = new FileBody(f, "image/jpeg");
          entity.addPart(f.getName(), fb);
          byteCount += f.length();
          log.info("added image file " + f.getName());
        } else if (extension.equals("3gpp")) {
          fb = new FileBody(f, "audio/3gpp");
          entity.addPart(f.getName(), fb);
          byteCount += f.length();
          log.info("added audio file " + f.getName());
        } else if (extension.equals("3gp")) {
          fb = new FileBody(f, "video/3gpp");
          entity.addPart(f.getName(), fb);
          byteCount += f.length();
          log.info("added video file " + f.getName());
        } else if (extension.equals("mp4")) {
          fb = new FileBody(f, "video/mp4");
          entity.addPart(f.getName(), fb);
          byteCount += f.length();
          log.info("added video file " + f.getName());
        } else if (extension.equals("csv")) {
          fb = new FileBody(f, "text/csv");
          entity.addPart(f.getName(), fb);
          byteCount += f.length();
          log.info("added csv file " + f.getName());
        } else if (extension.equals("xls")) {
          fb = new FileBody(f, "application/vnd.ms-excel");
          entity.addPart(f.getName(), fb);
          byteCount += f.length();
          log.info("added xls file " + f.getName());
        } else {
          fb = new FileBody(f, "application/octet-stream");
          entity.addPart(f.getName(), fb);
          byteCount += f.length();
          log.warning("added unrecognized file (application/octet-stream) " + f.getName());
        }

        // we've added at least one attachment to the request...
        if (j + 1 < files.size()) {
          if ((j-lastJ+1) > 100 || byteCount + files.get(j + 1).length() > 10000000L) {
            // more than 100 attachments or the next file would exceed the 10MB threshold...
            log.info("Extremely long post is being split into multiple posts");
            try {
              StringBody sb = new StringBody("yes", Charset.forName("UTF-8"));
              entity.addPart("*isIncomplete*", sb);
            } catch (Exception e) {
              e.printStackTrace();
              throw new IllegalStateException("never happens");
            }
            ++j; // advance over the last attachment added...
            break;
          }
        }
      }

      httppost.setEntity(entity);

      int[] validStatusList = { 201 };

      try {
        if (j != files.size()) {
          formToTransfer.setStatusString("Uploading " + description.getDocumentDescriptionType()
              + " and media files " + (lastJ + 1) + " through " + (j + 1) + " of " + files.size()
              + " media attachments", true);
        } else if (j == 0) {
          formToTransfer.setStatusString("Uploading " + description.getDocumentDescriptionType()
              + " with no media attachments", true);
        } else {
          formToTransfer.setStatusString("Uploading " + description.getDocumentDescriptionType() + " and "
              + (j - lastJ) + ((lastJ != 0) ? " remaining" : "") + " media attachments", true);
        }
        EventBus.publish(new FormStatusEvent(formToTransfer));

        httpRetrieveXmlDocument(httppost, validStatusList, serverInfo, false, description, action);
      } catch (XmlDocumentFetchException e) {
        e.printStackTrace();
        allSuccessful = false;
        formToTransfer.setStatusString("UPLOAD FAILED: " + e.getMessage(), false);
        EventBus.publish(new FormStatusEvent(formToTransfer));
        
        if ( description.isCancelled() ) return false;
      }
    }

    return allSuccessful;
  }
}
