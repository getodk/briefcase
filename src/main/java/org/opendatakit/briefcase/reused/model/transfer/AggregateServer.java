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

package org.opendatakit.briefcase.reused.model.transfer;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.getFileExtension;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.newInputStream;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.get;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.head;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.opendatakit.briefcase.operations.transfer.SourceOrTarget;
import org.opendatakit.briefcase.operations.transfer.pull.aggregate.Cursor;
import org.opendatakit.briefcase.operations.transfer.pull.aggregate.DownloadedSubmission;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.api.Json;
import org.opendatakit.briefcase.reused.api.Pair;
import org.opendatakit.briefcase.reused.http.Credentials;
import org.opendatakit.briefcase.reused.http.Request;
import org.opendatakit.briefcase.reused.http.RequestBuilder;
import org.opendatakit.briefcase.reused.model.XmlElement;
import org.opendatakit.briefcase.reused.model.form.FormKey;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;

/**
 * This class represents a remote ODK Aggregate server and provides methods to get
 * the different HTTP requests available in Aggregate's web API.
 */
// TODO v2.0 Test the methods that return Request objects
public class AggregateServer implements RemoteServer, SourceOrTarget {
  private final URL baseUrl;
  private final Optional<Credentials> credentials;

  public AggregateServer(URL baseUrl, Optional<Credentials> credentials) {
    this.baseUrl = baseUrl;
    this.credentials = credentials;
  }

  public static AggregateServer authenticated(URL baseUrl, Credentials credentials) {
    return new AggregateServer(baseUrl, Optional.of(credentials));
  }

  public static AggregateServer from(JsonNode root) {
    return new AggregateServer(
        Json.get(root, "baseUrl").map(JsonNode::asText).map(RequestBuilder::url).orElseThrow(BriefcaseException::new),
        Json.get(root, "credentials").map(Credentials::from)
    );
  }

  public static AggregateServer normal(URL baseUrl) {
    return new AggregateServer(baseUrl, Optional.empty());
  }

  public static String cleanUrl(String url) {
    return url.contains("/Aggregate.html")
        ? url.substring(0, url.indexOf("/Aggregate.html"))
        : url;
  }

  public URL getBaseUrl() {
    return baseUrl;
  }

  public Request<String> getDownloadFormRequest(String formId) {
    return get(baseUrl)
        .asText()
        .withPath("/formXml")
        .withQuery(Pair.of("formId", formId))
        .withCredentials(credentials)
        .build();
  }

  public Request<String> getDownloadFormRequest(URL downloadUrl) {
    return get(downloadUrl)
        .asText()
        .withCredentials(credentials)
        .build();
  }

  public Request<DownloadedSubmission> getDownloadSubmissionRequest(String submissionKey) {
    return get(baseUrl)
        .asXmlElement()
        .withPath("/view/downloadSubmission")
        .withQuery(Pair.of("formId", submissionKey))
        .withCredentials(credentials)
        .withResponseMapper(DownloadedSubmission::from)
        .build();
  }

  public Request<List<FormMetadata>> getFormListRequest() {
    return get(baseUrl)
        .asXmlElement()
        .withPath("/formList")
        .withCredentials(credentials)
        .withResponseMapper(root -> root.findElements("xform")
            .stream()
            .filter(e -> e.findFirstElement("name").flatMap(XmlElement::maybeValue).isPresent() &&
                e.findFirstElement("formID").flatMap(XmlElement::maybeValue).isPresent())
            .map(e -> FormMetadata.empty(FormKey
                .of(
                    e.findFirstElement("formID").flatMap(XmlElement::maybeValue).get(),
                    e.findFirstElement("version").flatMap(XmlElement::maybeValue)
                ))
                .withFormName(e.findFirstElement("name").flatMap(XmlElement::maybeValue).get())
                .withUrls(
                    e.findFirstElement("manifestUrl").flatMap(XmlElement::maybeValue).map(RequestBuilder::url),
                    e.findFirstElement("downloadUrl").flatMap(XmlElement::maybeValue).map(RequestBuilder::url)
                )).collect(toList())).build();
  }

  public Request<Boolean> getFormExistsRequest(String formId) {
    return getFormListRequest().builder()
        .withResponseMapper(formMeatadataList -> formMeatadataList.stream().anyMatch(formMetadata -> formMetadata.getKey().getId().equals(formId)))
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
            Pair.of("cursor", cursor.getValue()),
            Pair.of("numEntries", String.valueOf(entriesPerBatch)),
            Pair.of("includeIncomplete", includeIncomplete ? "true" : "false")
        )
        .withCredentials(credentials)
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
        .withCredentials(credentials)
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
        .withCredentials(credentials)
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

  @Override
  public ObjectNode asJson(ObjectMapper mapper) {
    ObjectNode root = mapper.createObjectNode();
    root.put("type", getType().getName());
    root.put("baseUrl", baseUrl.toString());
    ObjectNode credentialsNode = root.putObject("credentials");
    credentials.map(c -> c.asJson(mapper)).ifPresent(credentialsNode::setAll);
    return root;
  }

  @Override
  public Type getType() {
    return Type.AGGREGATE;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AggregateServer that = (AggregateServer) o;
    return Objects.equals(baseUrl, that.baseUrl) &&
        Objects.equals(credentials, that.credentials);
  }

  @Override
  public int hashCode() {
    return Objects.hash(baseUrl, credentials);
  }

  @Override
  public String toString() {
    return "AggregateServer{" +
        "baseUrl=" + baseUrl +
        ", credentials=" + credentials +
        '}';
  }
}
