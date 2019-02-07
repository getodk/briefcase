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

import static java.nio.charset.StandardCharsets.UTF_8;

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
import java.util.List;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;

class AggregateUtils {

  private static final Logger log = LoggerFactory.getLogger(AggregateUtils.class);

  private static final CharSequence HTTP_CONTENT_TYPE_TEXT_XML = "text/xml";

  private static final CharSequence HTTP_CONTENT_TYPE_APPLICATION_XML = "application/xml";

  public interface ResponseAction {
    void doAction(DocumentFetchResult result) throws MetadataUpdateException;
  }

  static class DocumentFetchResult {
    final Document doc;
    final boolean isOpenRosaResponse;

    DocumentFetchResult(Document doc, boolean isOpenRosaResponse) {
      this.doc = doc;
      this.isOpenRosaResponse = isOpenRosaResponse;
    }
  }

  /**
   * Common routine to download a document from the downloadUrl and save the
   * contents in the file 'f'. Shared by media file download and form file
   * download.
   */
  static void commonDownloadFile(ServerConnectionInfo serverInfo, File f, String downloadUrl) throws URISyntaxException, IOException, TransmissionException {

    log.info("Downloading URL {} into {}", downloadUrl, f);

    // OK. We need to download it because we either:
    // (1) don't have it
    // (2) don't know if it is changed because the hash is not md5
    // (3) know it is changed
    URI u;
    try {
      log.info("Parsing URL {}", downloadUrl);
      URL uurl = new URL(downloadUrl);
      u = uurl.toURI();
    } catch (MalformedURLException | URISyntaxException e) {
      log.warn("bad download url", e);
      throw e;
    }


    HttpClient httpclient = WebUtils.createHttpClient();

    // get shared HttpContext so that authentication and cookies are retained.
    HttpClientContext localContext = WebUtils.getHttpContext();

    // set up request...
    HttpGet req = WebUtils.createOpenRosaHttpGet(u);

    WebUtils.setCredentials(localContext, serverInfo, u);

    HttpResponse response;
    // try
    {
      response = httpclient.execute(req, localContext);
      int statusCode = response.getStatusLine().getStatusCode();

      if (statusCode == 401) {
        // We reset the Http context to force next request to authenticate itself
        WebUtils.resetHttpContext();
        throw new TransmissionException("Authentication failure");
      } else if (statusCode != 200) {
        String errMsg = "Fetch failed. Detailed reason: " + response.getStatusLine().getReasonPhrase() + " (" + statusCode + ")";
        log.error(errMsg);
        flushEntityBytes(response.getEntity());
        throw new TransmissionException(errMsg);
      }

      // write connection to file

      try (InputStream is = response.getEntity().getContent();
           OutputStream os = new FileOutputStream(f)) {
        byte buf[] = new byte[1024];
        int len;
        while ((len = is.read(buf)) > 0) {
          os.write(buf, 0, len);
        }
        os.flush();
      }
    }
  }

  /**
   * Common method for returning a parsed xml document given a url and the http
   * context and client objects involved in the web connection. The document is
   * the body of the response entity and should be xml.
   */
  static DocumentFetchResult getXmlDocument(String urlString, ServerConnectionInfo serverInfo, boolean alwaysResetCredentials, DocumentDescription description) throws XmlDocumentFetchException {
    log.info("Parsing URL {}", urlString);
    URI u;
    try {
      URL url = new URL(urlString);
      u = url.toURI();
    } catch (MalformedURLException e) {
      String msg = description.getFetchDocFailed() + "Invalid url. Failed with error: " + e.getMessage();
      if (!urlString.toLowerCase().startsWith("http://") && !urlString.toLowerCase().startsWith("https://")) {
        msg += ". Did you forget to prefix the address with 'http://' or 'https://' ?";
      }
      log.warn(msg, e);
      throw new XmlDocumentFetchException(msg);
    } catch (URISyntaxException e) {
      String msg = description.getFetchDocFailed() + "Invalid uri. Failed with error: " + e.getMessage();
      log.warn(msg, e);
      throw new XmlDocumentFetchException(msg);
    }

    // set up request...
    HttpGet req = WebUtils.createOpenRosaHttpGet(u);

    int[] validStatusList = {200};

    return httpRetrieveXmlDocument(req, validStatusList, serverInfo, alwaysResetCredentials, description, null);
  }

  private static void flushEntityBytes(HttpEntity entity) {
    if (entity != null) {
      // something is amiss -- read and discard any response body.
      try {
        // don't really care about the stream...
        InputStream is = entity.getContent();
        // read to end of stream...
        final long count = 1024L;
        //noinspection StatementWithEmptyBody
        while (is.skip(count) == count) ;
        is.close();
      } catch (Exception e) {
        log.error("failed to flush http content", e);
      }
    }
  }

  /**
   * Common method for returning a parsed xml document given a url and the http
   * context and client objects involved in the web connection. The document is
   * the body of the response entity and should be xml.
   */
  private static DocumentFetchResult httpRetrieveXmlDocument(HttpUriRequest request, int[] validStatusList, ServerConnectionInfo serverInfo, boolean alwaysResetCredentials, DocumentDescription description, ResponseAction action) throws XmlDocumentFetchException {

    HttpClient httpClient = WebUtils.createHttpClient();

    // get shared HttpContext so that authentication and cookies are retained.
    HttpClientContext localContext = WebUtils.getHttpContext();

    URI uri = request.getURI();
    log.info("Attempting URI {}", uri);

    WebUtils.setCredentials(localContext, serverInfo, uri, alwaysResetCredentials);

    if (description.isCancelled()) {
      throw new XmlDocumentFetchException("Transfer of " + description.getDocumentDescriptionType() + " aborted.");
    }

    HttpResponse response;
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

        if (statusCode == 401) {
          // We reset the Http context to force next request to authenticate itself
          WebUtils.resetHttpContext();
          ex = new XmlDocumentFetchException("Authentication failure");
        } else if (statusCode == 400) {
          ex = new XmlDocumentFetchException("" +
              description.getFetchDocFailed() + webError + " while accessing: " + uri.toString() + "\n" +
              "Please verify that the " + description.getDocumentDescriptionType() + " that is being uploaded is well-formed.");
        } else {
          ex = new XmlDocumentFetchException("" +
              description.getFetchDocFailed() + webError + " while accessing: " + uri.toString() + "\n" +
              "Please verify that the URL, your user credentials and your permissions are all correct."
          );
        }
      } else if (entity == null) {
        log.warn("No entity body returned");
        ex = new XmlDocumentFetchException(description.getFetchDocFailed() + " Server unexpectedly returned no content");
      } else if (!(lcContentType.contains(HTTP_CONTENT_TYPE_TEXT_XML) || lcContentType.contains(HTTP_CONTENT_TYPE_APPLICATION_XML))) {
        log.warn("Wrong ContentType: " + entity.getContentType().getValue() + "returned");
        ex = new XmlDocumentFetchException(description.getFetchDocFailed() + "A non-XML document was returned. A network login screen may be interfering with the transmission to the server.");
      }

      if (ex != null) {
        flushEntityBytes(entity);
        // and throw the exception...
        throw ex;
      }

      // parse the xml document...
      Document doc;
      try (InputStream is = entity.getContent(); InputStreamReader isr = new InputStreamReader(is, UTF_8)) {
        doc = new Document();
        KXmlParser parser = new KXmlParser();
        parser.setInput(isr);
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        doc.parse(parser);
      } catch (Exception e) {
        log.warn("Parsing failed with " + e.getMessage(), e);
        throw new XmlDocumentFetchException(description.getFetchDocFailed());
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
          log.warn(WebUtils.OPEN_ROSA_VERSION_HEADER + " unrecognized version(s): " + b);
        }
      }

      if (statusCode >= 300 && statusCode <= 400) {
        // what about location?
        Header[] locations = response.getHeaders("Location");
        if (locations != null && locations.length == 1) {
          try {
            URL url = new URL(locations[0].getValue());
            URI uNew = url.toURI();
            log.info("Redirection to URI {}", uNew);
            if (uri.getHost().equalsIgnoreCase(uNew.getHost())) {
              // trust the server to tell us a new location
              // ... and possibly to use https instead.
              String fullUrl = url.toExternalForm();
              int idx = fullUrl.lastIndexOf("/");
              serverInfo.setUrl(fullUrl.substring(0, idx));
            } else {
              // Don't follow a redirection attempt to a different host.
              // We can't tell if this is a spoof or not.
              String msg = description.getFetchDocFailed() + "Unexpected redirection attempt";
              log.warn(msg);
              throw new XmlDocumentFetchException(msg);
            }
          } catch (MalformedURLException | URISyntaxException e) {
            String msg = description.getFetchDocFailed() + "Unexpected exception: " + e.getMessage();
            log.warn(msg, e);
            throw new XmlDocumentFetchException(msg);
          }
        }
      }
      DocumentFetchResult result = new DocumentFetchResult(doc, isOR);
      if (action != null) {
        action.doAction(result);
      }
      return result;
    } catch (UnknownHostException e) {
      String msg = description.getFetchDocFailed() + "Unknown host";
      log.warn(msg, e);
      throw new XmlDocumentFetchException(msg);
    } catch (ConnectTimeoutException e) {
      // We need to anonymize the host info that comes on the
      // exception's message before logging the exception
      String msg = description.getFetchDocFailed() + "Connection timeout";
      log.warn(msg, e.getCause());
      throw new XmlDocumentFetchException(msg);
    } catch (IOException | MetadataUpdateException e) {
      String msg = description.getFetchDocFailed() + "Unexpected exception: " + e;
      log.warn(msg, e);
      throw new XmlDocumentFetchException(msg);
    }
  }

  static URI getAggregateActionUri(ServerConnectionInfo serverInfo, String actionAddr) throws TransmissionException {
    String urlString = serverInfo.getUrl();
    if (urlString.endsWith("/")) {
      urlString = urlString + actionAddr;
    } else {
      urlString = urlString + "/" + actionAddr;
    }

    log.info("Parsing URL {}", urlString);
    try {
      URL url = new URL(urlString);
      return url.toURI();
    } catch (MalformedURLException e) {
      String msg = "Invalid url for " + actionAddr + ". Failed with error: " + e.getMessage();
      if (!urlString.toLowerCase().startsWith("http://") && !urlString.toLowerCase().startsWith("https://")) {
        msg += ". Did you forget to prefix the address with 'http://' or 'https://' ?";
      }
      log.warn(msg, e);
      throw new TransmissionException(msg);
    } catch (URISyntaxException e) {
      String msg = "Invalid uri for " + actionAddr + ". Failed with error: " + e.getMessage();
      log.warn(msg, e);
      throw new TransmissionException(msg);
    }
  }

  static boolean uploadFilesToServer(ServerConnectionInfo serverInfo, URI u, String distinguishedFileTagName, File file, List<File> files, DocumentDescription description, SubmissionResponseAction action, TerminationFuture terminationFuture, FormStatus formToTransfer) {

    boolean allSuccessful = true;
    formToTransfer.setStatusString("Preparing for upload of " + description.getDocumentDescriptionType() + " with " + files.size() + " media attachments", true);
    EventBus.publish(new FormStatusEvent(formToTransfer));

    boolean first = true; // handles case where there are no media files
    int lastJ;
    int j = 0;
    while (j < files.size() || first) {
      lastJ = j;
      first = false;

      if (terminationFuture.isCancelled()) {
        formToTransfer.setStatusString("Aborting upload of " + description.getDocumentDescriptionType() + " with " + files.size() + " media attachments", true);
        EventBus.publish(new FormStatusEvent(formToTransfer));
        return false;
      }

      HttpPost httppost = WebUtils.createOpenRosaHttpPost(u);

      long byteCount = 0L;

      // mime post
      MultipartEntityBuilder builder = MultipartEntityBuilder.create();

      // add the submission file first...
      FileBody fb = new FileBody(file, ContentType.TEXT_XML);
      builder.addPart(distinguishedFileTagName, fb);
      log.info("added " + distinguishedFileTagName + ": " + file.getName());
      byteCount += file.length();

      for (; j < files.size(); j++) {
        File f = files.get(j);
        log.info("Trying file {}", f);
        String fileName = f.getName();
        int idx = fileName.lastIndexOf(".");
        String extension = "";
        if (idx != -1) {
          extension = fileName.substring(idx + 1);
        }

        // we will be processing every one of these, so
        // we only need to deal with the content type determination...
        switch (extension) {
          case "xml":
            fb = new FileBody(f, ContentType.TEXT_XML);
            builder.addPart(f.getName(), fb);
            byteCount += f.length();
            log.info("added xml file " + f.getName());
            break;
          case "jpg":
            fb = new FileBody(f, ContentType.create("image/jpeg"));
            builder.addPart(f.getName(), fb);
            byteCount += f.length();
            log.info("added image file " + f.getName());
            break;
          case "3gpp":
            fb = new FileBody(f, ContentType.create("audio/3gpp"));
            builder.addPart(f.getName(), fb);
            byteCount += f.length();
            log.info("added audio file " + f.getName());
            break;
          case "3gp":
            fb = new FileBody(f, ContentType.create("video/3gpp"));
            builder.addPart(f.getName(), fb);
            byteCount += f.length();
            log.info("added video file " + f.getName());
            break;
          case "mp4":
            fb = new FileBody(f, ContentType.create("video/mp4"));
            builder.addPart(f.getName(), fb);
            byteCount += f.length();
            log.info("added video file " + f.getName());
            break;
          case "csv":
            fb = new FileBody(f, ContentType.create("text/csv"));
            builder.addPart(f.getName(), fb);
            byteCount += f.length();
            log.info("added csv file " + f.getName());
            break;
          case "xls":
            fb = new FileBody(f, ContentType.create("application/vnd.ms-excel"));
            builder.addPart(f.getName(), fb);
            byteCount += f.length();
            log.info("added xls file " + f.getName());
            break;
          default:
            fb = new FileBody(f, ContentType.create("application/octet-stream"));
            builder.addPart(f.getName(), fb);
            byteCount += f.length();
            log.warn("added unrecognized file (application/octet-stream)");
            break;
        }

        // we've added at least one attachment to the request...
        if (j + 1 < files.size()) {
          if ((j - lastJ + 1) > 100 || byteCount + files.get(j + 1).length() > 10000000L) {
            // more than 100 attachments or the next file would exceed the 10MB threshold...
            log.info("Extremely long post is being split into multiple posts");
            try {
              StringBody sb = new StringBody("yes", ContentType.DEFAULT_TEXT.withCharset(UTF_8));
              builder.addPart("*isIncomplete*", sb);
            } catch (Exception e) {
              log.error("impossible condition", e);
              throw new IllegalStateException("never happens");
            }
            ++j; // advance over the last attachment added...
            break;
          }
        }
      }

      httppost.setEntity(builder.build());

      int[] validStatusList = {201};

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
        allSuccessful = false;
        log.error("upload failed", e);
        formToTransfer.setStatusString("UPLOAD FAILED: " + e.getMessage(), false);
        EventBus.publish(new FormStatusEvent(formToTransfer));

        if (description.isCancelled())
          return false;
      }
    }

    return allSuccessful;
  }
}
