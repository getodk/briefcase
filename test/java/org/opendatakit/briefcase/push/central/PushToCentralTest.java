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

package org.opendatakit.briefcase.push.central;


import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createTempDirectory;
import static org.opendatakit.briefcase.reused.UncheckedFiles.deleteRecursive;
import static org.opendatakit.briefcase.reused.UncheckedFiles.readAllBytes;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.url;
import static org.opendatakit.briefcase.reused.http.RequestSpyMatchers.hasBeenCalled;
import static org.opendatakit.briefcase.reused.http.RequestSpyMatchers.hasBody;
import static org.opendatakit.briefcase.reused.http.RequestSpyMatchers.isMultipart;
import static org.opendatakit.briefcase.reused.http.response.ResponseHelpers.ok;
import static org.opendatakit.briefcase.reused.job.JobsRunner.launchSync;
import static org.opendatakit.briefcase.reused.transfer.TransferTestHelpers.buildFormStatus;
import static org.opendatakit.briefcase.reused.transfer.TransferTestHelpers.getResourcePath;
import static org.opendatakit.briefcase.reused.transfer.TransferTestHelpers.installForm;
import static org.opendatakit.briefcase.reused.transfer.TransferTestHelpers.installFormAttachment;
import static org.opendatakit.briefcase.reused.transfer.TransferTestHelpers.installSubmission;
import static org.opendatakit.briefcase.reused.transfer.TransferTestHelpers.installSubmissionAttachment;
import static org.opendatakit.briefcase.reused.transfer.TransferTestHelpers.listOfFormsResponseFromCentral;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.reused.http.Credentials;
import org.opendatakit.briefcase.reused.http.FakeHttp;
import org.opendatakit.briefcase.reused.http.RequestSpy;
import org.opendatakit.briefcase.reused.job.TestRunnerStatus;
import org.opendatakit.briefcase.reused.transfer.CentralServer;

public class PushToCentralTest {
  private CentralServer server = CentralServer.of(url("http://foo.bar"), 1, Credentials.from("some user", "some password"));
  private FormStatus formStatus = buildFormStatus("push-form-test", server.getBaseUrl().toString());
  private String token = "some token";
  private FakeHttp http;
  private Path briefcaseDir;
  private PushToCentral pushOp;
  private List<String> events;
  private TestRunnerStatus runnerStatus;
  private PushToCentralTracker tracker;
  private Path form;
  private Path formAttachment;
  private String instanceId = "uuid:520e7b86-1572-45b1-a89e-7da26ad1624e";
  private Path submission;
  private Path submissionAttachment;

  @Before
  public void setUp() throws IOException {
    http = new FakeHttp();
    briefcaseDir = createTempDirectory("briefcase-test-");
    pushOp = new PushToCentral(http, server, briefcaseDir, token, this::onEvent);
    events = new ArrayList<>();
    runnerStatus = new TestRunnerStatus(false);
    tracker = new PushToCentralTracker(formStatus, this::onEvent);
    form = installForm(formStatus, getResourcePath("/org/opendatakit/briefcase/push/aggregate/push-form-test.xml"), briefcaseDir);
    formAttachment = installFormAttachment(formStatus, getResourcePath("/org/opendatakit/briefcase/push/aggregate/sparrow.png"), briefcaseDir);
    submission = installSubmission(formStatus, getResourcePath("/org/opendatakit/briefcase/push/aggregate/submission.xml"), briefcaseDir);
    submissionAttachment = installSubmissionAttachment(formStatus, getResourcePath("/org/opendatakit/briefcase/push/aggregate/1556532531101.jpg"), briefcaseDir, instanceId);
  }

  @After
  public void tearDown() {
    deleteRecursive(briefcaseDir);
  }

  private void onEvent(FormStatusEvent e) {
    events.add(e.getStatusString());
  }

  @Test
  public void knows_how_to_check_if_the_form_already_exists_in_Central() {
    // Low-level test that drives an individual step of the push operation

    RequestSpy<?> requestSpy = http.spyOn(
        server.getFormExistsRequest(formStatus.getFormId(), token),
        ok(listOfFormsResponseFromCentral(formStatus))
    );

    boolean exists = pushOp.checkFormExists(formStatus.getFormId(), runnerStatus, tracker);

    assertThat(requestSpy, allOf(hasBeenCalled(), not(isMultipart())));
    assertThat(exists, is(true));
  }

  @Test
  public void knows_how_to_check_if_the_form_does_not_exist_in_Central() {
    // Low-level test that drives an individual step of the push operation
    http.stub(
        server.getFormExistsRequest(formStatus.getFormId(), token),
        ok(listOfFormsResponseFromCentral())
    );

    assertThat(pushOp.checkFormExists(formStatus.getFormId(), runnerStatus, tracker), is(false));
  }

  @Test
  public void knows_how_to_push_forms_to_Central() {
    // Low-level test that drives an individual step of the push operation
    RequestSpy<?> requestSpy = http.spyOn(server.getPushFormRequest(form, token), ok("{}"));

    pushOp.pushForm(form, runnerStatus, tracker);

    assertThat(requestSpy, allOf(hasBeenCalled(), not(isMultipart()), hasBody(readAllBytes(form))));
  }

  @Test
  public void knows_how_to_push_form_attachments_to_Central() {
    // Low-level test that drives an individual step of the push operation
    RequestSpy<?> requestSpy = http.spyOn(server.getPushFormAttachmentRequest(formStatus.getFormId(), formAttachment, token), ok("{}"));

    pushOp.pushFormAttachment(formStatus.getFormId(), formAttachment, runnerStatus, tracker);

    assertThat(requestSpy, allOf(hasBeenCalled(), not(isMultipart()), hasBody(readAllBytes(formAttachment))));
  }

  @Test
  public void knows_how_to_push_submissions_to_Central() {
    // Low-level test that drives an individual step of the push operation
    RequestSpy<?> requestSpy = http.spyOn(server.getPushSubmissionRequest(token, formStatus.getFormId(), submission), ok("{}"));

    pushOp.pushSubmission(formStatus.getFormId(), instanceId, submission, runnerStatus, tracker);

    assertThat(requestSpy, allOf(hasBeenCalled(), not(isMultipart()), hasBody(readAllBytes(submission))));
  }

  @Test
  public void knows_how_to_push_submission_attachments_to_Central() {
    // Low-level test that drives an individual step of the push operation
    RequestSpy<?> requestSpy = http.spyOn(
        server.getPushSubmissionAttachmentRequest(token, formStatus.getFormId(), instanceId, submissionAttachment),
        ok("{}")
    );

    pushOp.pushSubmissionAttachment(formStatus.getFormId(), instanceId, submissionAttachment, runnerStatus, tracker);

    assertThat(requestSpy, allOf(hasBeenCalled(), not(isMultipart())));
  }


  @Test
  public void knows_how_to_push_completely_a_form_when_the_form_doesn_exist_in_Central() {
    // High-level test that drives the public push operation
    http.stub(server.getFormExistsRequest(formStatus.getFormId(), token), ok(listOfFormsResponseFromCentral()));
    http.stub(server.getPushFormRequest(form, token), ok("{}"));
    http.stub(server.getPushFormAttachmentRequest(formStatus.getFormId(), formAttachment, token), ok("{}"));
    http.stub(server.getPushSubmissionRequest(token, formStatus.getFormId(), submission), ok("{}"));
    http.stub(server.getPushSubmissionAttachmentRequest(token, formStatus.getFormId(), instanceId, submissionAttachment), ok("{}"));

    launchSync(pushOp.push(formStatus), result -> {}, error -> {});

    assertThat(events, allOf(
        hasItem("Form doesn't exist in Central"),
        hasItem("Pushed form"),
        hasItem("Pushed form attachment sparrow.png"),
        hasItem("Pushed submission uuid:520e7b86-1572-45b1-a89e-7da26ad1624e"),
        hasItem("Pushed attachment 1556532531101.jpg of submission uuid:520e7b86-1572-45b1-a89e-7da26ad1624e")
    ));
  }

  @Test
  public void knows_how_to_push_completely_a_form_when_the_form_exists_in_Central() {
    // High-level test that drives the public push operation
    http.stub(server.getFormExistsRequest(formStatus.getFormId(), token), ok(listOfFormsResponseFromCentral(formStatus)));
    http.stub(server.getPushFormRequest(form, token), ok("{\"a\":1}"));
    http.stub(server.getPushFormAttachmentRequest(formStatus.getFormId(), formAttachment, token), ok("{\"a\":2}"));
    http.stub(server.getPushSubmissionRequest(token, formStatus.getFormId(), submission), ok("{\"a\":3}"));
    http.stub(server.getPushSubmissionAttachmentRequest(token, formStatus.getFormId(), instanceId, submissionAttachment), ok("{\"a\":4}"));

    launchSync(pushOp.push(formStatus), result -> {}, error -> {});

    assertThat(events, allOf(
        hasItem("Form already exists in Central"),
        not(hasItem("Pushed form")),
        not(hasItem("Pushed form attachment sparrow.png")),
        hasItem("Pushed submission uuid:520e7b86-1572-45b1-a89e-7da26ad1624e"),
        hasItem("Pushed attachment 1556532531101.jpg of submission uuid:520e7b86-1572-45b1-a89e-7da26ad1624e")
    ));
  }

}

