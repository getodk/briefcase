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

package org.opendatakit.briefcase.reused.transfer;

import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.reused.UncheckedFiles.newInputStream;

import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.RemoteFormDefinition;
import org.opendatakit.briefcase.reused.OptionalProduct;
import org.opendatakit.briefcase.reused.http.Credentials;
import org.opendatakit.briefcase.reused.http.Request;
import org.opendatakit.briefcase.reused.http.RequestBuilder;

/**
 * This class represents a remote ODK Central server and provides methods to get
 * the different HTTP requests available in Central's REST API.
 */
// TODO v2.0 Test the methods that return Request objects
public class CentralServer implements RemoteServer {
  private static final String PREFS_KEY_CENTRAL_URL = "central_url";
  private static final String PREFS_KEY_CENTRAL_PROJECT_ID = "central_project_id";
  private static final String PREFS_KEY_CENTRAL_USERNAME = "central_username";
  private static final String PREFS_KEY_CENTRAL_PASSWORD = "central_password";
  public static List<String> PREFERENCE_KEYS = Arrays.asList(
      PREFS_KEY_CENTRAL_URL,
      PREFS_KEY_CENTRAL_PROJECT_ID,
      PREFS_KEY_CENTRAL_USERNAME,
      PREFS_KEY_CENTRAL_PASSWORD
  );
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

  private static CentralServer from(URL url, int projectId, String username, String password) {
    return new CentralServer(url, projectId, Credentials.from(username, password));
  }

  private static String buildSessionPayload(Credentials credentials) {
    return String.format(
        "{\"email\":\"%s\",\"password\":\"%s\"}",
        credentials.getUsername(),
        credentials.getPassword()
    );
  }

  public Request<Boolean> getCredentialsTestRequest() {
    return RequestBuilder.post(baseUrl)
        .asJsonMap()
        .withPath("/v1/sessions")
        .withHeader("Content-Type", "application/json")
        .withBody(buildSessionPayload(credentials))
        .withResponseMapper(json -> !((String) json.get("token")).isEmpty())
        .build();
  }

  public Request<String> getSessionTokenRequest() {
    return RequestBuilder.post(baseUrl)
        .asJsonMap()
        .withPath("/v1/sessions")
        .withHeader("Content-Type", "application/json")
        .withBody(buildSessionPayload(credentials))
        .withResponseMapper(json -> (String) json.get("token"))
        .build();
  }

  public URL getBaseUrl() {
    return baseUrl;
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
        .asJsonList(CentralAttachment.class)
        .withPath("/v1/projects/" + projectId + "/forms/" + formId + "/attachments")
        .withHeader("Authorization", "Bearer " + token)
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
        .withResponseMapper(json -> json.stream().map(o -> (String) o.get("instanceId")).collect(toList()))
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
        .asJsonList(CentralAttachment.class)
        .withPath("/v1/projects/" + projectId + "/forms/" + formId + "/submissions/" + instanceId + "/attachments")
        .withHeader("Authorization", "Bearer " + token)
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
        .withResponseMapper(forms -> forms.stream().anyMatch(form -> form.getFormId().equals(formId)))
        .build();
  }

  public Request<Map<String, Object>> getPushFormRequest(Path formFile, String token) {
    return RequestBuilder.post(baseUrl)
        .asJsonMap()
        .withPath("/v1/projects/" + projectId + "/forms")
        .withHeader("Authorization", "Bearer " + token)
        .withHeader("Content-Type", "application/xml")
        .withBody(newInputStream(formFile))
        .build();
  }

  public Request<Map<String, Object>> getPushFormAttachmentRequest(String formId, Path attachment, String token) {
    return RequestBuilder.post(baseUrl)
        .asJsonMap()
        .withPath("/v1/projects/" + projectId + "/forms/" + formId + "/attachments/" + attachment.getFileName().toString())
        .withHeader("Authorization", "Bearer " + token)
        .withHeader("Content-Type", "*/*")
        .withBody(newInputStream(attachment))
        .build();
  }

  public Request<Map<String, Object>> getPushSubmissionRequest(String token, String formId, Path submission) {
    return RequestBuilder.post(baseUrl)
        .asJsonMap()
        .withPath("/v1/projects/" + projectId + "/forms/" + formId + "/submissions")
        .withHeader("Authorization", "Bearer " + token)
        .withHeader("Content-Type", "application/xml")
        .withBody(newInputStream(submission))
        .build();
  }

  public Request<Map<String, Object>> getPushSubmissionAttachmentRequest(String token, String formId, String instanceId, Path attachment) {
    return RequestBuilder.post(baseUrl)
        .asJsonMap()
        .withPath("/v1/projects/" + projectId + "/forms/" + formId + "/submissions/" + instanceId + "/attachments/" + attachment.getFileName().toString())
        .withHeader("Authorization", "Bearer " + token)
        .withHeader("Content-Type", "*/*")
        .withBody(newInputStream(attachment))
        .build();
  }

  public Request<List<RemoteFormDefinition>> getFormsListRequest(String token) {
    return RequestBuilder.get(baseUrl)
        .asJsonList()
        .withPath("/v1/projects/" + projectId + "/forms")
        .withHeader("Authorization", "Bearer " + token)
        .withResponseMapper(list -> list.stream()
            .map(json -> new RemoteFormDefinition(
                (String) json.get("name"),
                (String) json.get("xmlFormId"),
                (String) json.get("version"),
                String.format("%s/v1/projects/%d/forms/%s/manifest", baseUrl.toString(), projectId, (String) json.get("xmlFormId"))
            ))
            .collect(toList()))
        .build();
  }

  //region Saved preferences management - Soon to be replace by a database
  @Override
  public void storePullBeforeExportPrefs(BriefcasePreferences prefs, FormStatus form) {
    // Do nothing for now
  }

  @Override
  public void removePullBeforeExportPrefs(BriefcasePreferences prefs, FormStatus form) {
    // Do nothing for now
  }

  static Optional<RemoteServer> readPullBeforeExportPrefs(BriefcasePreferences prefs, FormStatus form) {
    return Optional.empty();
  }

  static Optional<CentralServer> readSourcePrefs(BriefcasePreferences prefs) {
    return OptionalProduct.all(
        prefs.nullSafeGet(PREFS_KEY_CENTRAL_URL).map(RequestBuilder::url),
        prefs.nullSafeGet(PREFS_KEY_CENTRAL_PROJECT_ID).map(Integer::parseInt),
        prefs.nullSafeGet(PREFS_KEY_CENTRAL_USERNAME),
        prefs.nullSafeGet(PREFS_KEY_CENTRAL_PASSWORD)
    ).map(CentralServer::from);
  }

  public void storeSourcePrefs(BriefcasePreferences prefs) {
    prefs.put(PREFS_KEY_CENTRAL_URL, baseUrl.toString());
    prefs.put(PREFS_KEY_CENTRAL_PROJECT_ID, String.valueOf(projectId));
    prefs.put(PREFS_KEY_CENTRAL_USERNAME, credentials.getUsername());
    prefs.put(PREFS_KEY_CENTRAL_PASSWORD, credentials.getPassword());
  }

  public static void clearSourcePrefs(BriefcasePreferences prefs) {
    prefs.removeAll(
        PREFS_KEY_CENTRAL_URL,
        PREFS_KEY_CENTRAL_PROJECT_ID,
        PREFS_KEY_CENTRAL_USERNAME,
        PREFS_KEY_CENTRAL_PASSWORD
    );
  }

  static boolean isPrefKey(String key) {
    return key.endsWith("_pull_settings_url")
        || key.endsWith("_pull_settings_username")
        || key.endsWith("_pull_settings_password");
  }
  //endregion
}
