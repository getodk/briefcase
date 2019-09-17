/*
 * Copyright (C) 2019 Nafundi
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

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.newInputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.opendatakit.briefcase.operations.transfer.SourceOrTarget;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.api.Json;
import org.opendatakit.briefcase.reused.api.OptionalProduct;
import org.opendatakit.briefcase.reused.http.Credentials;
import org.opendatakit.briefcase.reused.http.Request;
import org.opendatakit.briefcase.reused.http.RequestBuilder;
import org.opendatakit.briefcase.reused.model.form.FormKey;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;

/**
 * This class represents a remote ODK Central server and provides methods to get
 * the different HTTP requests available in Central's REST API.
 */
// TODO v2.0 Test the methods that return Request objects
public class CentralServer implements RemoteServer, SourceOrTarget {
  private final URL baseUrl;
  private final int projectId;
  private final Credentials credentials;

  private CentralServer(URL baseUrl, int projectId, Credentials credentials) {
    this.baseUrl = baseUrl;
    this.projectId = projectId;
    this.credentials = credentials;
  }

  public static CentralServer of(URL baseUrl, int projectId, Credentials credentials) {
    return new CentralServer(baseUrl, projectId, credentials);
  }

  public static CentralServer from(JsonNode root) {
    return OptionalProduct.all(
        Json.get(root, "baseUrl").map(JsonNode::asText).map(RequestBuilder::url),
        Json.get(root, "projectId").map(JsonNode::asInt),
        Json.get(root, "credentials").map(Credentials::from)
    ).map(CentralServer::new).orElseThrow(BriefcaseException::new);
  }

  private static String buildSessionPayload(Credentials credentials) {
    return String.format(
        "{\"email\":\"%s\",\"password\":\"%s\"}",
        credentials.getUsername(),
        credentials.getPassword()
    );
  }

  public static String cleanUrl(String url) {
    int index = url.indexOf("/#/");
    return index == -1 ? url : url.substring(0, index);
  }

  public Request<Boolean> getCredentialsTestRequest() {
    return RequestBuilder.post(baseUrl)
        .asJsonMap()
        .withPath("/v1/sessions")
        .withHeader("Content-Type", "application/json")
        .withBody(buildSessionPayload(credentials))
        .withResponseMapper(json -> !json.path("token").asText().isBlank())
        .build();
  }

  public Request<String> getSessionTokenRequest() {
    return RequestBuilder.post(baseUrl)
        .asJsonMap()
        .withPath("/v1/sessions")
        .withHeader("Content-Type", "application/json")
        .withBody(buildSessionPayload(credentials))
        .withResponseMapper(json -> json.get("token").asText())
        .build();
  }

  public URL getBaseUrl() {
    return baseUrl;
  }

  public int getProjectId() {
    return projectId;
  }

  public Request<Void> getDownloadFormRequest(String formId, Path target, String token) {
    return RequestBuilder.get(baseUrl)
        .withPath("/v1/projects/" + projectId + "/forms/" + formId + ".xml")
        .withHeader("Authorization", "Bearer " + token)
        .downloadTo(target)
        .build();
  }

  public Request<List<CentralAttachment>> getFormAttachmentListRequest(String formId, String token) {
    return RequestBuilder.get(baseUrl)
        .asJsonList()
        .withPath("/v1/projects/" + projectId + "/forms/" + formId + "/attachments")
        .withHeader("Authorization", "Bearer " + token)
        .withResponseMapper(CentralServer::buildCentralAttachmentList)
        .build();
  }

  public Request<Void> getDownloadFormAttachmentRequest(String formId, CentralAttachment attachment, Path target, String token) {
    return RequestBuilder.get(baseUrl)
        .withPath("/v1/projects/" + projectId + "/forms/" + formId + "/attachments/" + attachment.getName())
        .withHeader("Authorization", "Bearer " + token)
        .downloadTo(target)
        .build();
  }

  public Request<List<String>> getInstanceIdListRequest(String formId, String token) {
    return RequestBuilder.get(baseUrl)
        .asJsonList()
        .withPath("/v1/projects/" + projectId + "/forms/" + formId + "/submissions")
        .withHeader("Authorization", "Bearer " + token)
        .withResponseMapper(json -> json.map(o -> o.get("instanceId").asText()).collect(toList()))
        .build();
  }

  public Request<Void> getDownloadSubmissionRequest(String formId, String instanceId, Path target, String token) {
    return RequestBuilder.get(baseUrl)
        .withPath("/v1/projects/" + projectId + "/forms/" + formId + "/submissions/" + instanceId + ".xml")
        .withHeader("Authorization", "Bearer " + token)
        .downloadTo(target)
        .build();
  }

  public Request<List<CentralAttachment>> getSubmissionAttachmentListRequest(String formId, String instanceId, String token) {
    return RequestBuilder.get(baseUrl)
        .asJsonList()
        .withPath("/v1/projects/" + projectId + "/forms/" + formId + "/submissions/" + instanceId + "/attachments")
        .withHeader("Authorization", "Bearer " + token)
        .withResponseMapper(CentralServer::buildCentralAttachmentList)
        .build();
  }

  public Request<Void> getDownloadSubmissionAttachmentRequest(String formId, String instanceId, CentralAttachment attachment, Path target, String token) {
    return RequestBuilder.get(baseUrl)
        .withPath("/v1/projects/" + projectId + "/forms/" + formId + "/submissions/" + instanceId + "/attachments/" + attachment.getName())
        .withHeader("Authorization", "Bearer " + token)
        .downloadTo(target)
        .build();
  }

  public Request<Boolean> getFormExistsRequest(String formId, String token) {
    return getFormsListRequest(token).builder()
        .withResponseMapper(forms -> forms.stream().anyMatch(form -> form.getKey().getId().equals(formId)))
        .build();
  }

  public Request<JsonNode> getPushFormRequest(Path formFile, String token) {
    return RequestBuilder.post(baseUrl)
        .asJsonMap()
        .withPath("/v1/projects/" + projectId + "/forms")
        .withHeader("Authorization", "Bearer " + token)
        .withHeader("Content-Type", "application/xml")
        .withBody(newInputStream(formFile))
        .build();
  }

  public Request<JsonNode> getPushFormAttachmentRequest(String formId, Path attachment, String token) {
    return RequestBuilder.post(baseUrl)
        .asJsonMap()
        .withPath("/v1/projects/" + projectId + "/forms/" + formId + "/attachments/" + attachment.getFileName().toString())
        .withHeader("Authorization", "Bearer " + token)
        .withHeader("Content-Type", "*/*")
        .withBody(newInputStream(attachment))
        .build();
  }

  public Request<JsonNode> getPushSubmissionRequest(String token, String formId, Path submission) {
    return RequestBuilder.post(baseUrl)
        .asJsonMap()
        .withPath("/v1/projects/" + projectId + "/forms/" + formId + "/submissions")
        .withHeader("Authorization", "Bearer " + token)
        .withHeader("Content-Type", "application/xml")
        .withBody(newInputStream(submission))
        .build();
  }

  public Request<JsonNode> getPushSubmissionAttachmentRequest(String token, String formId, String instanceId, Path attachment) {
    return RequestBuilder.post(baseUrl)
        .asJsonMap()
        .withPath("/v1/projects/" + projectId + "/forms/" + formId + "/submissions/" + instanceId + "/attachments/" + attachment.getFileName().toString())
        .withHeader("Authorization", "Bearer " + token)
        .withHeader("Content-Type", "*/*")
        .withBody(newInputStream(attachment))
        .build();
  }

  public Request<List<FormMetadata>> getFormsListRequest(String token) {
    return RequestBuilder.get(baseUrl)
        .asJsonList()
        .withPath("/v1/projects/" + projectId + "/forms")
        .withHeader("Authorization", "Bearer " + token)
        .withResponseMapper(jsons -> jsons
            .map(json -> FormMetadata.empty(FormKey.of(
                json.get("xmlFormId").asText(),
                Optional.ofNullable(json.path("version").asText()).filter(not(String::isBlank))
            )).withFormName(json.get("name").asText()))
            .collect(toList()))
        .build();
  }

  private static List<CentralAttachment> buildCentralAttachmentList(Stream<JsonNode> jsons) {
    return jsons.map(json -> new CentralAttachment(
        json.get("name").asText(),
        json.get("exists").asBoolean()
    )).collect(toList());
  }

  @Override
  public ObjectNode asJson(ObjectMapper mapper) {
    ObjectNode root = mapper.createObjectNode();
    root.put("type", getType().getName());
    root.put("baseUrl", baseUrl.toString());
    root.put("projectId", projectId);
    root.putObject("credentials").setAll(credentials.asJson(mapper));
    return root;
  }

  @Override
  public Type getType() {
    return Type.CENTRAL;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CentralServer that = (CentralServer) o;
    return projectId == that.projectId &&
        Objects.equals(baseUrl, that.baseUrl) &&
        Objects.equals(credentials, that.credentials);
  }

  @Override
  public int hashCode() {
    return Objects.hash(baseUrl, projectId, credentials);
  }

  @Override
  public String toString() {
    return "CentralServer{" +
        "baseUrl=" + baseUrl +
        ", projectId=" + projectId +
        ", credentials=" + credentials +
        '}';
  }
}
