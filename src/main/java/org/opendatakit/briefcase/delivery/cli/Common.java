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
package org.opendatakit.briefcase.delivery.cli;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.net.URL;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.opendatakit.briefcase.operations.transfer.SourceOrTarget;
import org.opendatakit.briefcase.operations.transfer.pull.filesystem.FormInstaller;
import org.opendatakit.briefcase.operations.transfer.pull.filesystem.PathSourceOrTarget;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.cli.Flag;
import org.opendatakit.briefcase.reused.cli.Param;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.http.Request;
import org.opendatakit.briefcase.reused.http.RequestBuilder;
import org.opendatakit.briefcase.reused.http.response.Response;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormMetadataPort;

public class Common {
  // Base CLI flags for operations. Users must choose one of theese
  static final Flag PULL = Flag.of("pll", "pull", "Pull form(s)");
  static final Flag PUSH = Flag.of("psh", "push", "Push form(s)");
  static final Flag EXPORT = Flag.of("exp", "export", "Export form(s)");
  public static final Flag GUI = Flag.of("gui", "gui", "Launch GUI without running any CLI operation");

  // Args for all operations
  static final Param<String> FORM_ID = Param.arg("fid", "form-id", "Form ID");
  static final Param<Integer> PROJECT_ID = Param.integer("pid", "project-id", "Central Project ID number");

  // Common pull&push args
  static final Param<String> CREDENTIALS_USERNAME = Param.arg("u", "username", "Username");
  static final Param<String> CREDENTIALS_EMAIL = Param.arg("e", "email", "Email");
  static final Param<String> CREDENTIALS_PASSWORD = Param.arg("p", "password", "Password");

  private static final String SOURCE_OR_TARGET_TYPES_LIST = Stream.of(SourceOrTarget.Type.values()).map(v -> "\"" + v.getName() + "\"").collect(joining(", "));

  // Pull args
  static final Param<String> PULL_SOURCE = Param.arg("ps", "pull-source", "Pull source of forms (server URL or local path)");
  static final Param<SourceOrTarget.Type> PULL_SOURCE_TYPE = Param.arg("pst", "pull-source-type", "Pull source type. One of: " + SOURCE_OR_TARGET_TYPES_LIST, SourceOrTarget.Type::from);
  static final Flag START_FROM_LAST = Flag.of("sfl", "start_from_last", "Start pull from last submission pulled");
  static final Param<OffsetDateTime> START_FROM_DATE = Param.dateTime("sfd", "start_from_date", "Start pull from date");
  static final Flag INCLUDE_INCOMPLETE = Flag.of("ii", "include_incomplete", "Include incomplete submissions");

  // Push args
  static final Param<String> PUSH_TARGET = Param.arg("pt", "push-target", "Push target for forms (server URL)");
  static final Param<SourceOrTarget.Type> PUSH_TARGET_TYPE = Param.arg("ptt", "push-target-type", "Push target type. One of: " + SOURCE_OR_TARGET_TYPES_LIST, SourceOrTarget.Type::from);
  static final Flag FORCE_SEND_BLANK = Flag.of("fsb", "force_send_blank", "Force sending the blank form (Aggregate only)");

  // Export args
  static final Param<Path> EXPORT_DIR = Param.path("ed", "export-directory", "Export directory");
  static final Param<OffsetDateTime> EXPORT_START_DATE = Param.dateTime("sd", "start-date", "Export start date (inclusive)");
  static final Param<OffsetDateTime> EXPORT_END_DATE = Param.dateTime("ed", "end-date", "Export end date (inclusive)");
  static final Flag EXPORT_ATTACHMENTS = Flag.of("ea", "export-attachments", "Export attachments");
  static final Flag OVERWRITE = Flag.of("ow", "overwrite", "Overwrite exported files");
  static final Param<Path> PEM_FILE = Param.path("pf", "pem-file", "PEM file for form decryption");
  static final Flag SPLIT_SELECT_MULTIPLES = Flag.of("ssm", "split-select-multiples", "Split select multiple fields");
  static final Flag INCLUDE_GEOJSON_EXPORT = Flag.of("ig", "include-geojson", "Include a GeoJSON file with spatial data");
  static final Flag REMOVE_GROUP_NAMES = Flag.of("rgn", "remove-group-names", "Remove group names from column names");
  static final Flag SMART_APPEND = Flag.of("sa", "smart-append", "Include only new submissions since last export");

  // General Briefcase configuration args
  public static final Param<Path> WORKSPACE_LOCATION = Param.path("wl", "workspace-location", "Workspace location");
  public static final Flag DISABLE_ERROR_TRACKING = Flag.of("det", "disable-error-tracking", "Disable error tracking");
  public static final Param<Integer> MAX_HTTP_CONNECTIONS = Param.integer("hmc", "http-max-connections", "Maximum simultaneous HTTP connections (defaults to 8)");
  public static final Param<URL> HTTP_PROXY_HOST = Param.arg("hph", "http-proxy-host", "HTTP proxy host URL", RequestBuilder::url);
  public static final Param<Integer> HTTP_PROXY_PORT_NUMBER = Param.integer("hpp", "http-proxy-port", "HTTP proxy post number");

  static List<FormMetadata> getFormsToPull(Http http, Optional<String> formId, Request<List<FormMetadata>> formListRequest) {
    Response<List<FormMetadata>> formListResponse = http.execute(formListRequest);
    if (!formListResponse.isSuccess())
      throw new BriefcaseException(
          formListResponse.isRedirection()
              ? "Error connecting to Aggregate: Redirection detected"
              : formListResponse.isUnauthorized()
              ? "Error connecting to Aggregate: Wrong credentials"
              : formListResponse.isNotFound()
              ? "Error connecting to Aggregate: Aggregate not found"
              : "Error connecting to Aggregate"
      );

    List<FormMetadata> filteredForms = formListResponse.orElseThrow(BriefcaseException::new)
        .stream()
        .filter(f -> formId.map(id -> f.getKey().getId().equals(id)).orElse(true))
        .collect(toList());

    if (formId.isPresent() && filteredForms.isEmpty())
      throw new BriefcaseException("Form " + formId.get() + " not found");

    return filteredForms;
  }

  static List<FormMetadata> getFormsToPull(Optional<String> formId, Path source) {
    List<FormMetadata> formMetadataList = FormInstaller.scanCollectFormsAt(source)
        .stream()
        .filter(formMetadata -> formId.map(id -> formMetadata.getKey().getId().equals(id)).orElse(true))
        .map(formMetadata -> formMetadata.withPullSource(PathSourceOrTarget.collectFormAt(formMetadata.getFormFile())))
        .collect(toList());

    if (formId.isPresent() && formMetadataList.isEmpty())
      throw new BriefcaseException("Form " + formId.get() + " not found");
    return formMetadataList;
  }

  static List<FormMetadata> getFormsToPush(FormMetadataPort formMetadataPort, Optional<String> formId) {
    List<FormMetadata> formMetadataList = formMetadataPort.fetchAll()
        .filter(formMetadata -> formId.map(id -> formMetadata.getKey().getId().equals(id)).orElse(true))
        .collect(toList());

    if (formId.isPresent() && formMetadataList.isEmpty())
      throw new BriefcaseException("Form " + formId.get() + " not found");

    return formMetadataList;
  }
}
