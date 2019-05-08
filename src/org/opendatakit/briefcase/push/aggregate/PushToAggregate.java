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

package org.opendatakit.briefcase.push.aggregate;

import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.export.SubmissionParser.getListOfSubmissionFiles;
import static org.opendatakit.briefcase.reused.UncheckedFiles.list;
import static org.opendatakit.briefcase.reused.UncheckedFiles.readAllBytes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.opendatakit.briefcase.export.DateRange;
import org.opendatakit.briefcase.export.FormDefinition;
import org.opendatakit.briefcase.export.SubmissionMetaData;
import org.opendatakit.briefcase.export.XmlElement;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.UncheckedFiles;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.http.response.Response;
import org.opendatakit.briefcase.reused.job.Job;
import org.opendatakit.briefcase.reused.job.RunnerStatus;
import org.opendatakit.briefcase.reused.transfer.AggregateServer;

public class PushToAggregate {
  private final Http http;
  private final AggregateServer server;
  private final Path briefcaseDir;
  private final boolean forceSendForm;
  private final Consumer<FormStatusEvent> onEventCallback;

  public PushToAggregate(Http http, AggregateServer server, Path briefcaseDir, boolean forceSendForm, Consumer<FormStatusEvent> onEventCallback) {
    this.http = http;
    this.server = server;
    this.briefcaseDir = briefcaseDir;
    this.forceSendForm = forceSendForm;
    this.onEventCallback = onEventCallback;
  }

  public Job<Void> push(FormStatus form) {
    PushToAggregateTracker tracker = new PushToAggregateTracker(form, onEventCallback);
    tracker.trackForceSendForm(forceSendForm);
    return Job.supply(runnerStatus -> !forceSendForm && checkFormExists(form.getFormId(), runnerStatus, tracker))
        .thenAccept(((runnerStatus, formExists) -> {
          if (!formExists) {
            List<Path> attachments = getFormAttachments(form);
            pushFormAndAttachments(form, attachments, runnerStatus, tracker);
          }
        }))
        .thenSupply(__ -> getSubmissions(form))
        .thenAccept((runnerStatus, submissions) -> submissions.forEach(submission -> {
          String instanceId = submission.getInstanceId().orElseThrow(BriefcaseException::new);
          List<Path> attachments = UncheckedFiles.list(form.getSubmissionDir(briefcaseDir, instanceId))
              .filter(p -> !p.getFileName().toString().equals("submission.xml"))
              .collect(toList());
          pushSubmissionAndAttachments(form, instanceId, attachments, runnerStatus, tracker);
        }));
  }

  boolean checkFormExists(String formId, RunnerStatus runnerStatus, PushToAggregateTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Check if form exists in Aggregate");
      return false;
    }

    Response<Boolean> response = http.execute(server.getFormExistsRequest(formId));
    if (!response.isSuccess()) {
      tracker.trackError("Error checking if form exists in Aggregate", response);
      return false;
    }

    boolean exists = response.get();
    tracker.trackFormAlreadyExists(exists);
    return exists;
  }

  void pushFormAndAttachments(FormStatus form, List<Path> attachments, RunnerStatus runnerStatus, PushToAggregateTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Push form");
      return;
    }

    tracker.trackStartPushing();

    Response response = http.execute(server.getPushFormRequest(form.getFormFile(briefcaseDir), attachments));
    if (response.isSuccess())
      tracker.trackPushForm(attachments.size());
    else
      tracker.trackError("Failed to push form", response);
  }

  void pushSubmissionAndAttachments(FormStatus form, String instanceId, List<Path> attachments, RunnerStatus runnerStatus, PushToAggregateTracker tracker) {
    if (runnerStatus.isCancelled()) {
      tracker.trackCancellation("Push submission " + instanceId);
      return;
    }

    Response<XmlElement> response = http.execute(server.getPushSubmissionRequest(
        form.getSubmissionFile(briefcaseDir, instanceId),
        attachments
    ));
    if (response.isSuccess())
      tracker.trackPushSubmission(attachments.size(), instanceId);
    else
      tracker.trackError("Failed to push form", response);
  }

  private Stream<SubmissionMetaData> getSubmissions(FormStatus form) {
    return getListOfSubmissionFiles(FormDefinition.from(form.getFormFile(briefcaseDir)), DateRange.empty(), (path, message) -> { })
        .stream()
        .map(file -> {
          String xml = new String(readAllBytes(file));
          XmlElement from = XmlElement.from(xml);
          return new SubmissionMetaData(from);
        });
  }

  private List<Path> getFormAttachments(FormStatus form) {
    Path formMediaDir = form.getFormMediaDir(briefcaseDir);
    return Files.exists(formMediaDir)
        ? list(formMediaDir).filter(Files::isRegularFile).collect(toList())
        : Collections.<Path>emptyList();
  }
}
