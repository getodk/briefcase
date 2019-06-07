/*
 * Copyright (C) 2018 Nafundi
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

package org.opendatakit.briefcase.reused.transfer;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.model.BriefcasePreferences.AGGREGATE_1_0_URL;
import static org.opendatakit.briefcase.model.BriefcasePreferences.PASSWORD;
import static org.opendatakit.briefcase.model.BriefcasePreferences.USERNAME;
import static org.opendatakit.briefcase.reused.UncheckedFiles.getFileExtension;
import static org.opendatakit.briefcase.reused.UncheckedFiles.newInputStream;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.get;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.head;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.url;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.opendatakit.briefcase.export.XmlElement;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.RemoteFormDefinition;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.pull.aggregate.Cursor;
import org.opendatakit.briefcase.pull.aggregate.DownloadedSubmission;
import org.opendatakit.briefcase.reused.OptionalProduct;
import org.opendatakit.briefcase.reused.Pair;
import org.opendatakit.briefcase.reused.http.Credentials;
import org.opendatakit.briefcase.reused.http.Request;
import org.opendatakit.briefcase.reused.http.RequestBuilder;

/**
 * This class represents a remote ODK Aggregate server and provides methods to get
 * the different HTTP requests available in Aggregate's web API.
 */
// TODO v2.0 Test the methods that return Request objects
public class AggregateServer implements RemoteServer {
  public static List<String> PREFERENCE_KEYS = Arrays.asList(AGGREGATE_1_0_URL, USERNAME, PASSWORD);
  private final URL baseUrl;
  private final Optional<Credentials> credentials;

  public AggregateServer(URL baseUrl, Optional<Credentials> credentials) {
    this.baseUrl = baseUrl;
    this.credentials = credentials;
  }

  public static AggregateServer authenticated(URL baseUrl, Credentials credentials) {
    return new AggregateServer(baseUrl, Optional.of(credentials));
  }

  public static AggregateServer normal(URL baseUrl) {
    return new AggregateServer(baseUrl, Optional.empty());
  }

  public static AggregateServer from(ServerConnectionInfo sci) {
    return new AggregateServer(
        url(sci.getUrl()),
        OptionalProduct.all(
            Optional.ofNullable(sci.getUsername()),
            Optional.ofNullable(sci.getPassword()).map(String::new)
        ).map(Credentials::new)
    );
  }

  static Optional<AggregateServer> readPreferences(BriefcasePreferences prefs) {
    if (prefs.hasKey(AGGREGATE_1_0_URL)) {
      return prefs.nullSafeGet(AGGREGATE_1_0_URL)
          .map(RequestBuilder::url)
          .map(baseUrl -> new AggregateServer(
              baseUrl,
              OptionalProduct.all(
                  prefs.nullSafeGet(USERNAME),
                  prefs.nullSafeGet(PASSWORD)
              ).map(Credentials::new)
          ));
    }
    return Optional.empty();
  }

  public void storePreferences(BriefcasePreferences prefs, boolean storePasswords) {
    prefs.remove(AGGREGATE_1_0_URL);
    prefs.remove(USERNAME);
    prefs.remove(PASSWORD);

    // We only save the Aggregate URL if no credentials are defined or
    // if they're defined and we have the user's consent to save passwords,
    // to avoid saving a URL that won't work without credentials.
    if (!credentials.isPresent() || storePasswords)
      prefs.put(AGGREGATE_1_0_URL, getBaseUrl().toString());

    // We only save the credentials if we have the user's consent to save
    // passwords
    if (credentials.isPresent() && storePasswords) {
      prefs.put(USERNAME, credentials.get().getUsername());
      prefs.put(PASSWORD, credentials.get().getPassword());
    }
  }

  public URL getBaseUrl() {
    return baseUrl;
  }

  public ServerConnectionInfo asServerConnectionInfo() {
    return new ServerConnectionInfo(
        baseUrl.toString(),
        credentials.map(Credentials::getUsername).orElse(null),
        credentials.map(Credentials::getPassword).orElse("").toCharArray()
    );
  }

  public Request<String> getDownloadFormRequest(String formId) {
    return get(baseUrl)
        .asText()
        .withPath("/formXml")
        .withQuery(Pair.of("formId", formId))
        .build();
  }

  public Request<DownloadedSubmission> getDownloadSubmissionRequest(String submissionKey) {
    return get(baseUrl)
        .asXmlElement()
        .withPath("/view/downloadSubmission")
        .withQuery(Pair.of("formId", submissionKey))
        .withResponseMapper(DownloadedSubmission::from)
        .build();
  }

  public Request<List<RemoteFormDefinition>> getFormListRequest() {
    return get(baseUrl)
        .asXmlElement()
        .withPath("/formList")
        .withCredentials(credentials)
        .withResponseMapper(root -> root.findElements("xform")
            .stream()
            .filter(e -> e.findElement("name").flatMap(XmlElement::maybeValue).isPresent() &&
                e.findElement("formID").flatMap(XmlElement::maybeValue).isPresent())
            .map(e -> new RemoteFormDefinition(
                e.findElement("name").flatMap(XmlElement::maybeValue).get(),
                e.findElement("formID").flatMap(XmlElement::maybeValue).get(),
                e.findElement("version").flatMap(XmlElement::maybeValue).orElse(null),
                e.findElement("manifestUrl").flatMap(XmlElement::maybeValue).orElse(null)
            )).collect(toList())).build();
  }

  public Request<Boolean> getFormExistsRequest(String formId) {
    return getFormListRequest().builder()
        .withResponseMapper(formDefs -> formDefs.stream().anyMatch(formDef -> formDef.getFormId().equals(formId)))
        .build();
  }

  public Request<String> getPushFormPreflightRequest() {
    return head(baseUrl)
        .asText()
        .withPath("/upload")
        .withCredentials(credentials)
        .build();
  }

  public Request<XmlElement> getInstanceIdBatchRequest(String formId, int entriesPerBatch, Cursor cursor, boolean includeIncomplete) {
    return get(baseUrl)
        .asXmlElement()
        .withPath("/view/submissionList")
        .withQuery(
            Pair.of("formId", formId),
            Pair.of("cursor", cursor.get()),
            Pair.of("numEntries", String.valueOf(entriesPerBatch)),
            Pair.of("includeIncomplete", includeIncomplete ? "true" : "false")
        )
        .build();
  }

  public Request<InputStream> getPushFormRequest(Path formFile, List<Path> attachments) {
    RequestBuilder<InputStream> builder = RequestBuilder.post(baseUrl)
        .withPath("/formUpload")
        .withMultipartMessage(
            "form_def_file",
            "application/xml",
            formFile.getFileName().toString(),
            newInputStream(formFile)
        );
    for (Path attachment : attachments) {
      builder = builder.withMultipartMessage(
          attachment.getFileName().toString(),
          getContentType(attachment),
          attachment.getFileName().toString(),
          newInputStream(attachment)
      );
    }
    return builder
        .withHeader("X-OpenRosa-Version", "1.0")
        .withHeader("Date", OffsetDateTime.now().format(RFC_1123_DATE_TIME))
        .build();
  }

  public Request<XmlElement> getPushSubmissionRequest(Path submissionFile, List<Path> attachments) {
    RequestBuilder<XmlElement> builder = RequestBuilder.post(baseUrl)
        .asXmlElement()
        .withPath("/submission")
        .withMultipartMessage(
            "xml_submission_file",
            "application/xml",
            submissionFile.getFileName().toString(),
            newInputStream(submissionFile)
        );
    for (Path attachment : attachments) {
      builder = builder.withMultipartMessage(
          attachment.getFileName().toString(),
          getContentType(attachment),
          attachment.getFileName().toString(),
          newInputStream(attachment)
      );
    }
    return builder
        .withHeader("X-OpenRosa-Version", "1.0")
        .withHeader("Date", OffsetDateTime.now().format(RFC_1123_DATE_TIME))
        .build();
  }

  private String getContentType(Path file) {
    return getFileExtension(file.getFileName().toString())
        .map(extension -> {
          switch (extension) {
            case "xml":
              return "text/xml";
            case "jpg":
              return "image/jpeg";
            case "3gpp":
              return "audio/3gpp";
            case "3gp":
              return "video/3gpp";
            case "mp4":
              return "video/mp4";
            case "csv":
              return "text/csv";
            case "xls":
              return "application/vnd.ms-excel";
            default:
              return null;
          }
        })
        .orElse("application/octet-stream");

  }
}
